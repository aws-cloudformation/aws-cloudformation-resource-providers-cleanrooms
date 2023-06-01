package software.amazon.cleanrooms.collaboration

import software.amazon.cleanrooms.collaboration.typemapper.toCfnException
import software.amazon.cleanrooms.collaboration.typemapper.toResourceModel
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
        logger.log("Inside READ handler for Collaboration resource. Request AWS AccountId:$requestAccountId")
        val resourceModel = request.desiredResourceState
        val cleanRoomsClient = ClientBuilder.getCleanRoomsClient()

        return try {
            with(resourceModel) {
                val collaborationId = getCollaborationIdFromPrimaryIdentifier()
                logger.log("CollaborationId from resourceModel: $collaborationId")
                val collaboration = getCollaboration(collaborationId, proxy, cleanRoomsClient)
                logger.log("Retrieved collaboration with arn: ${collaboration.arn()}")
                val allMembers = listMembersForCollaboration(collaborationId, proxy, cleanRoomsClient)
                logger.log("Member accountIds of this collaboration are: ${allMembers.map { it.accountId }}")
                val creatorMember = allMembers.find { it.accountId == requestAccountId }
                    ?: error("The creator member is not part of listMembers response. This cannot really happen!! for accountId: $requestAccountId")
                val nonCreatorMembers = allMembers.filter { it.accountId != requestAccountId }
                val tags = listTagsForResource(collaboration.arn(), proxy, cleanRoomsClient).map { Tag(it.key, it.value) }

                val stabilizedResourceModel = collaboration.toResourceModel(creatorMember, nonCreatorMembers, tags.toSet())
                logger.log("[SUCCESS] ReadHandler succeeded. Returning resourceModel with arn: ${stabilizedResourceModel.arn}")
                ProgressEvent.defaultSuccessHandler(stabilizedResourceModel)
            }
        } catch (e: Exception) {
            val error = e.toCfnException()
            logger.log("[EXCEPTION] ReadHandler failed for account:$requestAccountId. Original error:$e, Mapped error:$error. Full stack trace:${e.stackTraceToString()}")
            ProgressEvent.defaultFailureHandler(error, error.errorCode)
        }
    }
}
