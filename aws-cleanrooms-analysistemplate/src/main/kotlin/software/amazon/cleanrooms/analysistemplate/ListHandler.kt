package software.amazon.cleanrooms.analysistemplate

import software.amazon.cleanrooms.analysistemplate.typemapper.toCfnException
import software.amazon.cleanrooms.analysistemplate.typemapper.toResourceModels
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ResourceHandlerRequest

class ListHandler : BaseHandler<CallbackContext?>() {
    override fun handleRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel>,
        callbackContext: CallbackContext?,
        logger: Logger
    ): ProgressEvent<ResourceModel, CallbackContext?> {
        val requestAccountId = request.awsAccountId
        val resourceModel = request.desiredResourceState
        logger.log("Inside LIST handler for Analysis Template resource. Request AWS AccountId:$requestAccountId")
        val cleanRoomsClient = ClientBuilder.getCleanRoomsClient()

        return try {
            with(resourceModel) {
                val listAnalysisTemplateResponse = listAnalysisTemplates(
                    membershipIdentifier,
                    request.nextToken,
                    proxy,
                    cleanRoomsClient
                )
                logger.log(
                    "ListAnalysisTemplates for AWS AccountId: $requestAccountId returned " +
                        "ids: ${listAnalysisTemplateResponse.analysisTemplateSummaries().map { it.id() }} " +
                        "with nextToken: ${listAnalysisTemplateResponse.nextToken()}"
                )
                val resourceModels: List<ResourceModel> = listAnalysisTemplateResponse
                    .analysisTemplateSummaries()
                    .toResourceModels(membershipIdentifier)

                ProgressEvent.builder<ResourceModel, CallbackContext>()
                    .resourceModels(resourceModels)
                    .nextToken(listAnalysisTemplateResponse.nextToken())
                    .status(OperationStatus.SUCCESS)
                    .build()
            }
        } catch (e: Exception) {
            val error = e.toCfnException()
            logger.log("[EXCEPTION] ListHandler failed for account:$requestAccountId. Original error:$e, Mapped error:$error. Full stack trace:${e.stackTraceToString()}")
            ProgressEvent.defaultFailureHandler(error, error.errorCode) }
    }
}
