package software.amazon.cleanrooms.membership

import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.awssdk.services.cleanrooms.model.Collaboration
import software.amazon.awssdk.services.cleanrooms.model.CreateCollaborationRequest
import software.amazon.awssdk.services.cleanrooms.model.CreateMembershipRequest
import software.amazon.awssdk.services.cleanrooms.model.DataEncryptionMetadata
import software.amazon.awssdk.services.cleanrooms.model.DeleteMembershipRequest
import software.amazon.awssdk.services.cleanrooms.model.DeleteMembershipResponse
import software.amazon.awssdk.services.cleanrooms.model.GetMembershipRequest
import software.amazon.awssdk.services.cleanrooms.model.ListMembershipsRequest
import software.amazon.awssdk.services.cleanrooms.model.ListMembershipsResponse
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.MemberAbility
import software.amazon.awssdk.services.cleanrooms.model.Membership
import software.amazon.awssdk.services.cleanrooms.model.TagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.UntagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.UpdateMembershipRequest
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy


/**
 * This files is a centralized placeholder for
 * - api request construction
 * - object translation to/from aws sdk
 * - resource model construction for read/list handlers
 * - methods to see if a resource exists, get the resource if it exists and other things, like fetching arn.
 */


/**
 * Method to parse membershipId from primaryIdentifier if it exists returns null otherwise
 */
fun ResourceModel.getMembershipIdFromPrimaryIdentifier(): String {
    return primaryIdentifier.getString(ResourceModel.IDENTIFIER_KEY_MEMBERSHIPIDENTIFIER)
        ?: error(
            "MembershipIdentifier key not found.  " +
                "Check if the 'membershipIdentifier' is defined in the resource template before reading it."
        )
}

fun createMembership(model: ResourceModel, tagsFromRequest: Map<String, String>, proxy: AmazonWebServicesClientProxy, cleanRoomsClient: CleanRoomsClient): Membership = with(model) {
    val createMembershipRequest = CreateMembershipRequest.builder()
        .collaborationIdentifier(collaborationIdentifier)
        .queryLogStatus(queryLogStatus)
        .tags(tagsFromRequest)
        .build();

    val createMembershipResponse = proxy.injectCredentialsAndInvokeV2(
        createMembershipRequest,
        cleanRoomsClient::createMembership
    )

    createMembershipResponse.membership()
}

fun updateMembership(model: ResourceModel, proxy: AmazonWebServicesClientProxy, cleanRoomsClient: CleanRoomsClient): Membership = with(model) {
    val updateMembershipRequest = UpdateMembershipRequest.builder()
        .membershipIdentifier(membershipIdentifier)
        .queryLogStatus(queryLogStatus)
        .build();

    val updateMembershipResponse = proxy.injectCredentialsAndInvokeV2(
        updateMembershipRequest,
        cleanRoomsClient::updateMembership
    )

    updateMembershipResponse.membership()
}

fun getMembership(membershipId: String, proxy: AmazonWebServicesClientProxy, cleanRoomsClient: CleanRoomsClient): Membership {
    val getMembershipRequest = GetMembershipRequest.builder()
        .membershipIdentifier(membershipId)
        .build()

    val getMembershipResponse = proxy.injectCredentialsAndInvokeV2(
        getMembershipRequest,
        cleanRoomsClient::getMembership
    )

    return getMembershipResponse.membership()
}

fun listMemberships(nextToken: String?, proxy: AmazonWebServicesClientProxy, cleanRoomsClient: CleanRoomsClient): ListMembershipsResponse {
    val listMembershipsRequest = ListMembershipsRequest.builder()
        .apply {
            nextToken?.let { nextToken(it) }
        }.build()
    return proxy.injectCredentialsAndInvokeV2(
        listMembershipsRequest,
        cleanRoomsClient::listMemberships
    )
}

fun deleteMembership(model: ResourceModel, proxy: AmazonWebServicesClientProxy, cleanRoomsClient: CleanRoomsClient): DeleteMembershipResponse = with(model) {
    // create the deletion request.
    val deleteCollaborationRequest = DeleteMembershipRequest.builder().membershipIdentifier(
        getMembershipIdFromPrimaryIdentifier()
    ).build()

    // make the call.
    proxy.injectCredentialsAndInvokeV2(
        deleteCollaborationRequest,
        cleanRoomsClient::deleteMembership
    )
}


/**
 * Gets the tags existing on a membership resource.
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
 * Build Request and Makes service call for Add Tags
 */
fun tagResource(
    resourceArn: String,
    tagsToAdd: Map<String, String>,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
) {
    val tagResourceRequest: TagResourceRequest = TagResourceRequest.builder()
        .resourceArn(resourceArn)
        .tags(tagsToAdd)
        .build()

    proxy.injectCredentialsAndInvokeV2(tagResourceRequest, cleanRoomsClient::tagResource)
}

/**
 * Build Request and Makes service call for Remove Tags
 */
fun untagResource(
    resourceArn: String,
    tagKeysToRemove: Set<String>,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
) {
    val untagResourceRequest: UntagResourceRequest = UntagResourceRequest.builder()
        .resourceArn(resourceArn)
        .tagKeys(tagKeysToRemove)
        .build()

    proxy.injectCredentialsAndInvokeV2(untagResourceRequest, cleanRoomsClient::untagResource)
}
