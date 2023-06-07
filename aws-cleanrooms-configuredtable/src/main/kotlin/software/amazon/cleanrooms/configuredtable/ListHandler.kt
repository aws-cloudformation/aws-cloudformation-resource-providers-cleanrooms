package software.amazon.cleanrooms.configuredtable

import software.amazon.cleanrooms.configuredtable.typemapper.toCfnException
import software.amazon.cleanrooms.configuredtable.typemapper.toResourceModels
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
        logger.log("Inside LIST handler for ConfiguredTable resource. Request AWS AccountId:$requestAccountId")
        val cleanRoomsClient = ClientBuilder.getCleanRoomsClient()

        return try {
            with(request) {
                val listConfiguredTablesResponse = listConfiguredTables(nextToken, proxy, cleanRoomsClient)
                logger.log("ListConfiguredTables for AWS AccountId: $requestAccountId returned " +
                    "ids: ${listConfiguredTablesResponse.configuredTableSummaries().map { it.id() }} " +
                    "with nextToken: ${listConfiguredTablesResponse.nextToken()}")
                val resourceModels: List<ResourceModel> = listConfiguredTablesResponse.configuredTableSummaries().toResourceModels()
                ProgressEvent.builder<ResourceModel, CallbackContext>()
                    .resourceModels(resourceModels)
                    .nextToken(listConfiguredTablesResponse.nextToken())
                    .status(OperationStatus.SUCCESS)
                    .build()
            }

        } catch (e: Exception) {
            val error = e.toCfnException()
            logger.log("[EXCEPTION] ListHandler failed for account:$requestAccountId. Original error:$e, Mapped error:$error. Full stack trace:${e.stackTraceToString()}")
            ProgressEvent.defaultFailureHandler(error, error.errorCode)
        }
    }
}
