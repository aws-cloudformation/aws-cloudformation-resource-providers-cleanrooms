package software.amazon.cleanrooms.configuredtable

import software.amazon.cleanrooms.configuredtable.typemapper.toCfnException
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
        logger.log("Inside CREATE handler for ConfiguredTable resource. Request AWS AccountId:$requestAccountId")
        val currentCallbackContext = callbackContext ?: CallbackContext(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES)

        return if (currentCallbackContext.pendingStabilization) {
            waitForCreateStabilization(proxy, request, currentCallbackContext, logger)
        } else {
            processCreateRequest(proxy, request, logger)
        }
    }

    private fun processCreateRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel>,
        logger: Logger
    ): ProgressEvent<ResourceModel, CallbackContext?> {
        val resourceModel = request.desiredResourceState
        val cleanRoomsClient = ClientBuilder.getCleanRoomsClient()
        return try {
            with(resourceModel) {
                val combinedTags = (request.desiredResourceTags ?: emptyMap()) + (request.systemTags ?: emptyMap())
                val configuredTable = createConfiguredTable(this, combinedTags, proxy, cleanRoomsClient)
                logger.log("ConfiguredTable Arn from API: ${configuredTable.arn()}")

                // Current limit is 1 Analysis Rule per Configured Table
                analysisRules?.forEach { analysisRule ->
                    logger.log("Creating Analysis Rule Type: ${analysisRule.type}")
                    createConfiguredTableAnalysisRule(analysisRule, configuredTable.id(), proxy, cleanRoomsClient)
                }
                resourceModel.apply {
                    arn = configuredTable.arn()
                    configuredTableIdentifier = configuredTable.id()
                }
                val callbackContext = CallbackContext(stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
                    pendingStabilization = true)
                ProgressEvent.defaultInProgressHandler(callbackContext, HandlerCommon.CALLBACK_DELAY_IN_SECONDS, resourceModel)
            }
        } catch (e: Exception) {
            val error = e.toCfnException()
            logger.log("[EXCEPTION] CreateHandler failed for account:${request.awsAccountId}. Original error:$e, Mapped error:$error. Full stack trace:${e.stackTraceToString()}")
            // In case failure occurs after resources created, need to return the primaryIdentifier to trigger rollback DeleteHandler
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
        logger: Logger,
    ): ProgressEvent<ResourceModel, CallbackContext?> {
        logger.log("Inside Create Resource Stabilization, Poll Retries Remaining: ${callbackContext.stabilizationRetriesRemaining}")

        val desiredResourceModel = request.desiredResourceState

        val currentCallbackContext = callbackContext.copy(stabilizationRetriesRemaining = callbackContext.stabilizationRetriesRemaining - 1)
        if (currentCallbackContext.stabilizationRetriesRemaining < 0) {
            val notStabilizedException = CfnNotStabilizedException(ResourceModel.TYPE_NAME, desiredResourceModel.configuredTableIdentifier)
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
