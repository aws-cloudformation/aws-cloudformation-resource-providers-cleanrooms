package software.amazon.cleanrooms.configuredtable

import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableAnalysisRuleResponse
import software.amazon.cleanrooms.configuredtable.typemapper.toAnalysisRule
import software.amazon.cleanrooms.configuredtable.typemapper.toCfnException
import software.amazon.cleanrooms.configuredtable.typemapper.toResourceModel
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ResourceHandlerRequest

class ReadHandler : BaseHandler<CallbackContext?>() {
    override fun handleRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel>,
        callbackContext: CallbackContext?,
        logger: Logger
    ): ProgressEvent<ResourceModel, CallbackContext?> {
        val requestAccountId = request.awsAccountId
        logger.log("Inside READ handler for ConfiguredTable resource. Request AWS AccountId:$requestAccountId")
        val resourceModel = request.desiredResourceState
        val cleanRoomsClient = ClientBuilder.getCleanRoomsClient()
        return try {
            with(resourceModel) {
                logger.log("ReadHandler ConfiguredTableIdentifier from resourceModel: $configuredTableIdentifier")
                val configuredTable = getConfiguredTable(configuredTableIdentifier, proxy, cleanRoomsClient)
                val analysisRules = configuredTable.analysisRuleTypesAsStrings().map { analysisRuleType ->
                    logger.log("Associated Analysis Rule type: $analysisRuleType")
                    val getAnalysisRuleResponse: GetConfiguredTableAnalysisRuleResponse =
                        getConfiguredTableAnalysisRule(analysisRuleType, resourceModel.configuredTableIdentifier, proxy, cleanRoomsClient)
                    getAnalysisRuleResponse.analysisRule().toAnalysisRule()
                }
                val tags = listTagsForResource(configuredTable.arn(), proxy, cleanRoomsClient).map { Tag(it.key, it.value) }.toSet()
                ProgressEvent.defaultSuccessHandler(configuredTable.toResourceModel(analysisRules, tags))
            }
        } catch (e: Exception) {
            val error = e.toCfnException()
            logger.log("[EXCEPTION] ReadHandler failed for account:$requestAccountId. Original error:$e, Mapped error:$error. Full stack trace:${e.stackTraceToString()}")
            ProgressEvent.defaultFailureHandler(error, error.errorCode)
        }
    }
}
