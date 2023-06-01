package software.amazon.cleanrooms.configuredtable

import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.cleanrooms.configuredtable.typemapper.toCfnException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ResourceHandlerRequest

class UpdateHandler : BaseHandler<CallbackContext?>() {
    override fun handleRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel>,
        callbackContext: CallbackContext?,
        logger: Logger
    ): ProgressEvent<ResourceModel, CallbackContext?> {
        val requestAccountId = request.awsAccountId
        logger.log("Inside UPDATE handler for ConfiguredTable resource. Request AWS AccountId:$requestAccountId")

        // Verify ConfiguredTable exists
        val readResult = ReadHandler().handleRequest(proxy, request, callbackContext, logger)
        if (readResult.isFailed) {
            return readResult
        }

        return try {
            val previousResourceModel = request.previousResourceState
            val desiredResourceModel = request.desiredResourceState
            val cleanRoomsClient = ClientBuilder.getCleanRoomsClient()
            logger.log("UpdateHandler ConfiguredTableIdentifier from resourceModel: ${desiredResourceModel.configuredTableIdentifier}")
            with(desiredResourceModel) {
                if (previousResourceModel.description != description || previousResourceModel.name != name){
                    logger.log("Updating ConfiguredTable")
                    updateConfiguredTable(this, proxy, cleanRoomsClient)
                }
                if (previousResourceModel.analysisRules != desiredResourceModel.analysisRules) {
                    readResult.resourceModel.analysisRules?.forEach { existingAnalysisRule ->
                        logger.log("Deleting Existing AnalysisRule Type: ${existingAnalysisRule.type}")
                        deleteConfiguredTableAnalysisRule(existingAnalysisRule.type, configuredTableIdentifier, proxy, cleanRoomsClient)
                    }
                    analysisRules?.forEach { analysisRule ->
                        logger.log("Creating AnalysisRule Type: ${analysisRule.type}")
                        createConfiguredTableAnalysisRule(analysisRule, configuredTableIdentifier, proxy, cleanRoomsClient)
                    }
                }
            }

            updateTags(
                request.desiredResourceTags?: emptyMap(),
                request.previousResourceTags?: emptyMap(),
                readResult.resourceModel.arn,
                proxy,
                cleanRoomsClient
            )

            ReadHandler().handleRequest(proxy, request, callbackContext, logger)
        } catch (e: Exception) {
            val error = e.toCfnException()
            logger.log("[EXCEPTION] UpdateHandler failed for account:${request.awsAccountId}. Original error:$e, Mapped error:$error. Full stack trace:${e.stackTraceToString()}")
            val currentStateReadResult = ReadHandler().handleRequest(proxy, request, callbackContext, logger)
            ProgressEvent.builder<ResourceModel, CallbackContext>()
                .apply {
                    currentStateReadResult.resourceModel?.let { resourceModel(currentStateReadResult.resourceModel) }
                }
                .status(OperationStatus.FAILED)
                .errorCode(error.errorCode)
                .message(error.message)
                .build()
        }
    }

    /**
     * Takes a diff of tags to add or remove and updates them as necessary.
     */
    private fun updateTags(
        desiredResourceTags: Map<String, String>,
        previousResourceTags: Map<String, String>,
        resourceArn: String,
        proxy: AmazonWebServicesClientProxy,
        cleanRoomsClient: CleanRoomsClient
    ) {
        val tagsToAdd: Map<String, String> = desiredResourceTags.filter { tag ->  !previousResourceTags.containsKey(tag.key) || previousResourceTags[tag.key] != tag.value}
        val tagKeysToRemove: Set<String> = previousResourceTags.keys - desiredResourceTags.keys

        if (tagsToAdd.isNotEmpty()) {
            tagResource(resourceArn, tagsToAdd, proxy, cleanRoomsClient)
        }

        if (tagKeysToRemove.isNotEmpty()) {
            untagResource(resourceArn, tagKeysToRemove, proxy, cleanRoomsClient)
        }
    }
}
