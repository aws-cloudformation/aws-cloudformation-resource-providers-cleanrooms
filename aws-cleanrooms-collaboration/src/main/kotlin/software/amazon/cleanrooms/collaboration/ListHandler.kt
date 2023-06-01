package software.amazon.cleanrooms.collaboration

import software.amazon.cleanrooms.collaboration.typemapper.toCfnException
import software.amazon.cleanrooms.collaboration.typemapper.toResourceModels
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
        logger.log("Inside LIST handler for Collaboration resource. Request AWS AccountId:$requestAccountId")
        val cleanRoomsClient = ClientBuilder.getCleanRoomsClient()
        return try {
            // working contract : https://tiny.amazon.com/md669qw0/IsenLink
            val listCollaborationsResponse = listCollaborations(request.nextToken, proxy, cleanRoomsClient)
            with(listCollaborationsResponse) {
                logger.log(
                    "ListCollaborations for AWS AccountId: $requestAccountId returned." +
                        "ids: ${collaborationList().map { it.id() }} with nextToken: ${nextToken()}"
                )
                val resourceModels = collaborationList().toResourceModels()

                ProgressEvent.builder<ResourceModel, CallbackContext>()
                    .resourceModels(resourceModels)
                    .nextToken(nextToken())
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
