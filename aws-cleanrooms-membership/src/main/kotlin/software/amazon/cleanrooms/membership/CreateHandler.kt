package software.amazon.cleanrooms.membership

import software.amazon.cloudformation.exceptions.CfnNotStabilizedException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ResourceHandlerRequest

class CreateHandler : BaseHandler<CallbackContext?>() {

    override fun handleRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel>,
        callbackContext: CallbackContext?,
        logger: Logger
    ): ProgressEvent<ResourceModel, CallbackContext?> {
        val requestAccountId = request.awsAccountId

        logger.log("Inside CREATE handler for Membership resource. Request AWS AccountId:$requestAccountId")
        val currentCallbackContext = callbackContext ?: CallbackContext(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES)
        return if (currentCallbackContext.pendingStabilization) {
            waitForCreateStabilization(proxy, request, currentCallbackContext, logger)
        } else {
            val response = processCreateRequest(proxy, request, logger)
            return response
        }
    }

    private fun processCreateRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel>,
        logger: Logger
    ): ProgressEvent<ResourceModel, CallbackContext?> {
        val resourceModel = request.desiredResourceState
        val cleanRoomsClient = ClientBuilder.getCleanRoomsClient()
        logger.log("Creating Membership for account: ${request.awsAccountId}")

        return try {
            with(resourceModel) {
                val combinedTags = (request.desiredResourceTags ?: emptyMap()) + (request.systemTags ?: emptyMap())
                val membership = createMembership(this, combinedTags, proxy, cleanRoomsClient)
                logger.log("Membership Arn from API: ${membership.arn()}")

                // Updating the readOnly properties for the resourceModel.
                resourceModel.apply {
                    membershipIdentifier = membership.id()
                    arn = membership.arn()
                }

                val callbackContext = CallbackContext(
                    stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
                    pendingStabilization = true
                )
                ProgressEvent.defaultInProgressHandler(callbackContext, HandlerCommon.CALLBACK_DELAY_IN_SECONDS, resourceModel)
            }
        } catch (e: Exception) {
            val error = e.toCfnException()
            logger.log("[EXCEPTION] CreateHandler failed for account:${request.awsAccountId}. Original error:$e, Mapped error:$error. Full stack trace:${e.stackTraceToString()}")
            // In case failure occurs after resources created, need to return the primaryIdentifier to trigger rollback DeleteHandler.
            // we had previously updated the resourceModel after creating, so in case any failure occurs, after membership is created, it will have the primaryIdentifier.
            ProgressEvent.builder<ResourceModel, CallbackContext>()
                .resourceModel(resourceModel)
                .status(OperationStatus.FAILED)
                .errorCode(error.errorCode)
                .message(error.message)
                .build()
        }
    }

    /**
     * Method to verify Resource Create Stabilization.
     * Create Stabilization requires
     *  1. Subsequent ReadHandler requests for the primaryIdentifier returns a matching model.
     *  2. Subsequent ListHandler requests DO contain the primaryIdentifier
     * Callbacks will continue until the stabilizationRetriesRemaining < 0 and returns a failure with CfnNotStabilizedException
     * Or Stabilization completes and returns a Success with the desired resourceModel
     */
    private fun waitForCreateStabilization(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel>,
        callbackContext: CallbackContext,
        logger: Logger
    ): ProgressEvent<ResourceModel, CallbackContext?> {
        logger.log("Inside Create Resource Stabilization for Membership resource, Poll Retries Remaining: ${callbackContext.stabilizationRetriesRemaining}")

        val desiredResourceModel = request.desiredResourceState

        val currentCallbackContext = callbackContext.copy(stabilizationRetriesRemaining = callbackContext.stabilizationRetriesRemaining - 1)
        if (currentCallbackContext.stabilizationRetriesRemaining < 0) {
            val notStabilizedException = CfnNotStabilizedException(ResourceModel.TYPE_NAME, desiredResourceModel.getMembershipIdFromPrimaryIdentifier())
            return ProgressEvent.defaultFailureHandler(notStabilizedException, notStabilizedException.errorCode)
        }
        try {
            val readResult = ReadHandler().handleRequest(proxy, request, callbackContext, logger)

            val isResourceFoundInListResponse = HandlerCommon.verifyListResourceFound(proxy, request, callbackContext, logger)

            with(readResult) {
                return if (isSuccess && isResourceFoundInListResponse) {
                    ProgressEvent.defaultSuccessHandler(resourceModel)
                } else {
                    ProgressEvent.defaultInProgressHandler(currentCallbackContext, HandlerCommon.CALLBACK_DELAY_IN_SECONDS, desiredResourceModel)
                }
            }
        } catch (e: Exception) {
            val error = e.toCfnException()
            logger.log("[EXCEPTION] CreateHandler Stabilization failed for account:${request.awsAccountId}. Original error:$e, Mapped error:$error. Full stack trace:${e.stackTraceToString()}")
            return ProgressEvent.defaultInProgressHandler(currentCallbackContext, HandlerCommon.CALLBACK_DELAY_IN_SECONDS, desiredResourceModel)
        }
    }
}
