package software.amazon.cleanrooms.membership

import software.amazon.cleanrooms.membership.typemapper.toResourceModels
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
        logger.log("Inside LIST handler for Membership resource. Request AWS AccountId:$requestAccountId")
        val cleanRoomsClient = ClientBuilder.getCleanRoomsClient()
        return try {
            val listMembershipsResponse = listMemberships(request.nextToken, proxy, cleanRoomsClient)
            with(listMembershipsResponse) {
                logger.log(
                    "ListMemberships for AWS AccountId: $requestAccountId returned." +
                        "ids: ${membershipSummaries().map { it.id() }} with nextToken: ${nextToken()}"
                )
                val resourceModels = membershipSummaries().toResourceModels()

                ProgressEvent.builder<ResourceModel, CallbackContext>()
                    .resourceModels(resourceModels)
                    .nextToken(nextToken())
                    .status(OperationStatus.SUCCESS)
                    .build()
            }
        } catch (e: Exception) {
            val error = e.toCfnException()
            logger.log("[EXCEPTION] ListHandler for membership failed for account:$requestAccountId. Original error:$e, Mapped error:$error. Full stack trace:${e.stackTraceToString()}")
            ProgressEvent.defaultFailureHandler(error, error.errorCode)
        }
    }
}
