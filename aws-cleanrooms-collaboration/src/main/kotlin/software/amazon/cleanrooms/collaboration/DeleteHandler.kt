package software.amazon.cleanrooms.collaboration

import software.amazon.cleanrooms.collaboration.HandlerCommon.CALLBACK_DELAY_IN_SECONDS
import software.amazon.cleanrooms.collaboration.HandlerCommon.verifyListResourceFound
import software.amazon.cleanrooms.collaboration.typemapper.toCfnException
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.HandlerErrorCode
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ResourceHandlerRequest

class DeleteHandler : BaseHandler<CallbackContext?>() {

    /**
     * Method to handle requests for Delete Handler.
     * Initial DeleteHandler request will delete the resource and return an IN_PROGRESS status.
     * This will trigger Callbacks based on the CALLBACK_DELAY_IN_SECONDS intervals.
     * Each Callback request will be routed to the waitForDeleteStabilization method to verify the resources
     * were in fact deleted.  Only after verification will SUCCESS be returned
     */
    override fun handleRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel>,
        callbackContext: CallbackContext?,
        logger: Logger
    ): ProgressEvent<ResourceModel, CallbackContext?> {
        val requestAccountId = request.awsAccountId
        logger.log("Inside DELETE handler for Collaboration resource. Request AWS AccountId:$requestAccountId")
        val currentCallbackContext = callbackContext ?: CallbackContext(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES)

        return if (currentCallbackContext.pendingStabilization) {
            waitForDeleteStabilization(proxy, request, currentCallbackContext, logger)
        } else {
            processDeleteRequest(proxy, request, currentCallbackContext, logger)
        }
    }

    /**
     * Method to process deleting the resource
     * After delete succeeds, IN_PROGRESS event will be returned with new CallbackContext initialized so that all
     * subsequent Callbacks will only check for stabilization
     */
    private fun processDeleteRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel>,
        callbackContext: CallbackContext,
        logger: Logger
    ): ProgressEvent<ResourceModel, CallbackContext?> {
        val resourceModel = request.desiredResourceState
        val cleanRoomsClient = ClientBuilder.getCleanRoomsClient()

        // Verify Collaboration exists
        val readResult = ReadHandler().handleRequest(proxy, request, callbackContext, logger)
        if (readResult.isFailed) {
            return readResult
        }

        return try {
            with(resourceModel) {
                logger.log("DeleteHandler CollaborationIdentifier from resourceModel: ${getCollaborationIdFromPrimaryIdentifier()}")
                deleteCollaboration(this, proxy, cleanRoomsClient)

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

    /**
     * Method to verify Resource Delete Stabilization
     * Delete Stabilization requires
     *  1. Subsequent ReadHandler requests for the primaryIdentifier to return a NotFound errorCode.
     *  2. Subsequent ListHandler requests DO NOT contain the primaryIdentifier
     * Callbacks will continue until the stabilizationRetriesRemaining < 0 and returns a failure with CfnNotStabilizedException
     * Or Stabilization completes and returns a Success with null resourceModel
     */
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
            val notStabilizedException = CfnNotStabilizedException(ResourceModel.TYPE_NAME, desiredResourceModel.getCollaborationIdFromPrimaryIdentifier())
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
            logger.log("[EXCEPTION] DeleteHandler stabilization failed for account:${request.awsAccountId}. Original error:$e, Mapped error:$error. Full stack trace:${e.stackTraceToString()}")
            return ProgressEvent.defaultInProgressHandler(currentCallbackContext, CALLBACK_DELAY_IN_SECONDS, desiredResourceModel)
        }
    }
}
