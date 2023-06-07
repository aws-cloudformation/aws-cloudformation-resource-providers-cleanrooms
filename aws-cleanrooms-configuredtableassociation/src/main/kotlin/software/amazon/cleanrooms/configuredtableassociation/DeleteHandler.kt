package software.amazon.cleanrooms.configuredtableassociation

import software.amazon.cleanrooms.configuredtableassociation.HandlerCommon.CALLBACK_DELAY_IN_SECONDS
import software.amazon.cleanrooms.configuredtableassociation.HandlerCommon.verifyListResourceFound
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.HandlerErrorCode
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ResourceHandlerRequest

class DeleteHandler : BaseHandler<CallbackContext?>() {
    override fun handleRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel>,
        callbackContext: CallbackContext?,
        logger: Logger
    ): ProgressEvent<ResourceModel, CallbackContext?> {
        val requestAccountId = request.awsAccountId
        logger.log("Inside DELETE handler for ConfiguredTableAssociation resource. Request AWS AccountId:$requestAccountId")
        val currentCallbackContext = callbackContext ?: CallbackContext(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES)

        return if (currentCallbackContext.pendingStabilization) {
            waitForDeleteStabilization(proxy, request, currentCallbackContext, logger)
        } else {
            processDeleteRequest(proxy, request, currentCallbackContext, logger)
        }
    }

    private fun processDeleteRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel>,
        callbackContext: CallbackContext,
        logger: Logger
    ): ProgressEvent<ResourceModel, CallbackContext?> {
        val resourceModel = request.desiredResourceState
        val cleanRoomsClient = ClientBuilder.getCleanRoomsClient()

        val readResult = ReadHandler().handleRequest(proxy, request, callbackContext, logger)
        if (readResult.isFailed) {
            logger.log("[WARNING] Read operation in DeleteHandler failed for account:${request.awsAccountId}.")
            return readResult
        }

        return try {
            with(resourceModel) {
                logger.log("DeleteHandler from resourceModel: $configuredTableAssociationIdentifier")
                deleteConfiguredTableAssociation(
                    configuredTableAssociationIdentifier,
                    membershipIdentifier,
                    proxy,
                    cleanRoomsClient
                )

                val currentCallbackContext = CallbackContext(
                    stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
                    pendingStabilization = true
                )
                ProgressEvent.defaultInProgressHandler(currentCallbackContext, CALLBACK_DELAY_IN_SECONDS, resourceModel)
            }
        } catch (e: Exception) {
            val error = e.toCfnException()
            logger.log("[EXCEPTION] DeleteHandler failed for account:${request.awsAccountId}. Original error:$e, Mapped error:$error. Full stack trace:${e.stackTraceToString()}")
            ProgressEvent.defaultFailureHandler(error, error.errorCode)
        }
    }

    private fun waitForDeleteStabilization(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel>,
        callbackContext: CallbackContext,
        logger: Logger
    ): ProgressEvent<ResourceModel, CallbackContext?> {
        logger.log("Inside Delete Resource Stabilization, Poll Retries Remaining: ${callbackContext.stabilizationRetriesRemaining}")

        val desiredResourceModel = request.desiredResourceState

        val currentCallbackContext = callbackContext.copy(stabilizationRetriesRemaining = callbackContext.stabilizationRetriesRemaining - 1)
        if (currentCallbackContext.stabilizationRetriesRemaining < 0) {
            val notStabilizedException = CfnNotStabilizedException(ResourceModel.TYPE_NAME, desiredResourceModel.configuredTableAssociationIdentifier)
            return ProgressEvent.defaultFailureHandler(notStabilizedException, notStabilizedException.errorCode)
        }
        try {
            val readResult = ReadHandler().handleRequest(proxy, request, callbackContext, logger)

            with(readResult) {
                val isReadFailedWithNotFound = (isFailed && errorCode == HandlerErrorCode.NotFound)

                val isResourceFoundInListResponse = verifyListResourceFound(proxy, request, callbackContext, logger)

                return if (isReadFailedWithNotFound && !isResourceFoundInListResponse) {
                    ProgressEvent.defaultSuccessHandler(null)
                } else {
                    ProgressEvent.defaultInProgressHandler(currentCallbackContext, CALLBACK_DELAY_IN_SECONDS, desiredResourceModel)
                }
            }
        } catch (e: Exception) {
            val error = e.toCfnException()
            logger.log("[EXCEPTION] DeleteHandler failed for account:${request.awsAccountId}. Original error:$e, Mapped error:$error. Full stack trace:${e.stackTraceToString()}")
            return ProgressEvent.defaultInProgressHandler(currentCallbackContext, CALLBACK_DELAY_IN_SECONDS, desiredResourceModel)
        }
    }
}
