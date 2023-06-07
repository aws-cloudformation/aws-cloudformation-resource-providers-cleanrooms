package software.amazon.cleanrooms.membership

import software.amazon.cleanrooms.membership.typemapper.toResourceModel
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
        logger.log("Inside READ handler for Membership resource. Request AWS AccountId:${requestAccountId}")
        val resourceModel = request.desiredResourceState
        val cleanRoomsClient = ClientBuilder.getCleanRoomsClient()

        return try {
            with(resourceModel) {
                val membershipId = getMembershipIdFromPrimaryIdentifier()
                logger.log("MembershipId from resourceModel: $membershipId")
                val membership = getMembership(membershipId, proxy, cleanRoomsClient)
                logger.log("Retrieved membership with arn: ${membership.arn()}")
                val tags = listTagsForResource(membership.arn(), proxy, cleanRoomsClient).map { Tag(it.key, it.value) }

                val stabilizedResourceModel = membership.toResourceModel(tags.toSet())
                logger.log("[SUCCESS] ReadHandler for membership succeeded. Returning resourcemodel with arn: ${stabilizedResourceModel.arn}")
                ProgressEvent.defaultSuccessHandler(stabilizedResourceModel)
            }
        } catch (e: Exception) {
            val error = e.toCfnException()
            logger.log("[EXCEPTION] ReadHandler for membership failed for account:$requestAccountId. Original error:$e, Mapped error:$error. Full stack trace:${e.stackTraceToString()}")
            ProgressEvent.defaultFailureHandler(error, error.errorCode)
        }
    }
}
