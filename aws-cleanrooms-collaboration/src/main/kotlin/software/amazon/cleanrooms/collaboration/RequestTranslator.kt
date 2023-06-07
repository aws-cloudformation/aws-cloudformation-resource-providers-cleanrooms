package software.amazon.cleanrooms.collaboration

import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.awssdk.services.cleanrooms.model.Collaboration
import software.amazon.awssdk.services.cleanrooms.model.CreateCollaborationRequest
import software.amazon.awssdk.services.cleanrooms.model.DataEncryptionMetadata
import software.amazon.awssdk.services.cleanrooms.model.DeleteCollaborationRequest
import software.amazon.awssdk.services.cleanrooms.model.DeleteCollaborationResponse
import software.amazon.awssdk.services.cleanrooms.model.GetCollaborationRequest
import software.amazon.awssdk.services.cleanrooms.model.ListCollaborationsRequest
import software.amazon.awssdk.services.cleanrooms.model.ListCollaborationsResponse
import software.amazon.awssdk.services.cleanrooms.model.ListMembersRequest
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.MemberAbility
import software.amazon.awssdk.services.cleanrooms.model.MemberSummary
import software.amazon.awssdk.services.cleanrooms.model.TagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.UntagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.UpdateCollaborationRequest
import software.amazon.cleanrooms.collaboration.typemapper.toMemberSpecification
import software.amazon.cleanrooms.collaboration.typemapper.toMembers
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy

/**
 * This files is a centralized placeholder for
 * - api request construction
 * - object translation to/from aws sdk
 * - resource model construction for read/list handlers
 * - methods to see if a resource exists, get the resource if it exists and other things, like fetching arn.
 */
fun ResourceModel.fetchIdFromArn(): String {
    // Example regex for collaboration: https://tiny.amazon.com/mm1ki4o3/codeamazpackAWSBblobmainmode
    val arnRegex = "^arn:aws:\\w+:\\w{2}-\\w{4,9}-\\d:\\d{12}:collaboration/([\\w-]+)$".toRegex()

    return arnRegex.find(arn)?.groupValues?.get(1) ?: throw CfnGeneralServiceException(
        "Failed to fetch resource Id from Arn: $arn"
    )
}

/**
 * Method to parse collaborationId from primaryIdentifier if it exists returns null otherwise
 */
fun ResourceModel.getCollaborationIdFromPrimaryIdentifier(): String {
    return primaryIdentifier.getString(ResourceModel.IDENTIFIER_KEY_COLLABORATIONIDENTIFIER)
        ?: error(
            "CollaborationIdentifier key not found. " +
                "Check if the 'collaborationIdentifier' is defined in the resource template before reading it."
        )
}

fun createCollaboration(model: ResourceModel, tagsFromRequest: Map<String, String>, proxy: AmazonWebServicesClientProxy, cleanRoomsClient: CleanRoomsClient): Collaboration = with(model) {
    val createCollaborationRequest = CreateCollaborationRequest.builder()
        .name(name)
        .tags(tagsFromRequest)
        .creatorDisplayName(creatorDisplayName)
        .apply {
            dataEncryptionMetadata?.let {
                dataEncryptionMetadata(
                    with(it) {
                        DataEncryptionMetadata.builder()
                            .allowCleartext(allowCleartext)
                            .allowDuplicates(allowDuplicates)
                            .allowJoinsOnColumnsWithDifferentNames(allowJoinsOnColumnsWithDifferentNames)
                            .preserveNulls(preserveNulls)
                            .build()
                    }
                )
            }
        }
        .creatorMemberAbilities(
            creatorMemberAbilities?.map {
                MemberAbility.fromValue(it)
            } ?: emptyList()
        )
        .description(description)
        .members(toMembers())
        .queryLogStatus(queryLogStatus)
        .build()

    val createCollborationResponse = proxy.injectCredentialsAndInvokeV2(
        createCollaborationRequest,
        cleanRoomsClient::createCollaboration
    )

    createCollborationResponse.collaboration()
}

fun getCollaboration(collaborationId: String, proxy: AmazonWebServicesClientProxy, cleanRoomsClient: CleanRoomsClient): Collaboration {
    val getCollaborationRequest = GetCollaborationRequest.builder()
        .collaborationIdentifier(
            collaborationId
        ).build()

    val getCollaborationResponse = proxy.injectCredentialsAndInvokeV2(
        getCollaborationRequest,
        cleanRoomsClient::getCollaboration
    )

    return getCollaborationResponse.collaboration()
}

fun deleteCollaboration(model: ResourceModel, proxy: AmazonWebServicesClientProxy, cleanRoomsClient: CleanRoomsClient): DeleteCollaborationResponse = with(model) {
    // create the deletion request.
    val deleteCollaborationRequest = DeleteCollaborationRequest.builder().collaborationIdentifier(
        getCollaborationIdFromPrimaryIdentifier()
    ).build()

    // make the call.
    proxy.injectCredentialsAndInvokeV2(
        deleteCollaborationRequest,
        cleanRoomsClient::deleteCollaboration
    )
}

fun updateCollaboration(resourceModel: ResourceModel, proxy: AmazonWebServicesClientProxy, cleanRoomsClient: CleanRoomsClient): Collaboration = with(resourceModel) {
    val updateCollaborationsRequest = UpdateCollaborationRequest.builder()
        .collaborationIdentifier(collaborationIdentifier)
        .apply {
            description?.let { description(it) }
            name?.let { name(it) }
        }
        .build()

    proxy.injectCredentialsAndInvokeV2(
        updateCollaborationsRequest,
        cleanRoomsClient::updateCollaboration
    ).collaboration()
}

/**
 * Calls the listCollaboration API
 */
fun listCollaborations(nextToken: String?, proxy: AmazonWebServicesClientProxy, cleanRoomsClient: CleanRoomsClient): ListCollaborationsResponse {
    val listCollaborationsRequest = ListCollaborationsRequest.builder()
        .apply {
            nextToken?.let { nextToken(it) }
        }.build()
    return proxy.injectCredentialsAndInvokeV2(
        listCollaborationsRequest,
        cleanRoomsClient::listCollaborations
    )
}

/**
 * Gets a list of members from Collaboration object.
 */
fun listMembersForCollaboration(
    collaborationId: String,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
): List<MemberSpecification> {
    val allMembers: List<MemberSummary> = sequence {
        // According to AWS example: https://tiny.amazon.com/1dqswm78u/docsawsamazcodelatecatajava
        // and Documentation for the ListMembers API: https://tiny.amazon.com/96ak3lql/awsDoc
        // there can be more data available to get after a response is received.
        // The code below appropriately calls until we have retrieved all results.
        var token: String? = null
        do {
            val listMembersRequest = ListMembersRequest.builder()
                .collaborationIdentifier(collaborationId)
                .nextToken(token)
                .build()

            // make the call
            proxy.injectCredentialsAndInvokeV2(
                listMembersRequest,
                cleanRoomsClient::listMembers
            ).let {
                token = it.nextToken()
                yieldAll(it.memberSummaries())
            }
        } while (token != null)
    }.toList()

    return allMembers.toMemberSpecification()
}

/**
 * Gets the tags existing on a collaboration resource.
 */
fun listTagsForResource(
    resourceArn: String,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
): Map<String, String> {
    val listTagsRequest = ListTagsForResourceRequest.builder()
        .resourceArn(resourceArn)
        .build()
    return proxy.injectCredentialsAndInvokeV2(listTagsRequest, cleanRoomsClient::listTagsForResource).tags().filter {
        !it.key.startsWith("aws:")
    }
}

/**
 * Untags the resource with the provided keys
 */
fun untagResource(
    resourceArn: String,
    tagKeysToRemove: Set<String>,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
) {
    proxy.injectCredentialsAndInvokeV2(
        UntagResourceRequest.builder().resourceArn(resourceArn).tagKeys(tagKeysToRemove).build(),
        cleanRoomsClient::untagResource
    )
}

/**
 * Tags the resource with the provided tags.
 */
fun tagResource(
    resourceArn: String,
    tagsToAdd: Map<String, String>,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
) {
    proxy.injectCredentialsAndInvokeV2(
        TagResourceRequest.builder().resourceArn(resourceArn).tags(tagsToAdd).build(),
        cleanRoomsClient::tagResource
    )
}
