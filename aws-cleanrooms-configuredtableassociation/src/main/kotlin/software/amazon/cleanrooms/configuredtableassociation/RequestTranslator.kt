package software.amazon.cleanrooms.configuredtableassociation

import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAssociation
import software.amazon.awssdk.services.cleanrooms.model.CreateConfiguredTableAssociationRequest
import software.amazon.awssdk.services.cleanrooms.model.DeleteConfiguredTableAssociationRequest
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableAssociationRequest
import software.amazon.awssdk.services.cleanrooms.model.ListConfiguredTableAssociationsRequest
import software.amazon.awssdk.services.cleanrooms.model.ListConfiguredTableAssociationsResponse
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.TagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.UntagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.UpdateConfiguredTableAssociationRequest
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy

fun ResourceModel.getResourcePrimaryIdentifier(): String {
    return primaryIdentifier.getString(ResourceModel.IDENTIFIER_KEY_CONFIGUREDTABLEASSOCIATIONIDENTIFIER)
        ?: error(
            "ConfiguredTableAssociationIdentifier key not found. " +
                "Check if the 'configuredTableAssociationIdentifier' is defined in the resource template before reading it."
        )
}
fun getConfiguredTableAssociation(
    configuredTableAssociationIdentifier: String,
    membershipIdentifier: String,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
): ConfiguredTableAssociation {
    val getConfiguredTableAssociationRequest = GetConfiguredTableAssociationRequest.builder()
        .configuredTableAssociationIdentifier(configuredTableAssociationIdentifier)
        .membershipIdentifier(membershipIdentifier)
        .build()

    val getConfiguredTableAssociationResponse = proxy.injectCredentialsAndInvokeV2(
        getConfiguredTableAssociationRequest,
        cleanRoomsClient::getConfiguredTableAssociation
    )

    return getConfiguredTableAssociationResponse.configuredTableAssociation()
}

fun listConfiguredTableAssociations(
    membershipIdentifier: String,
    nextToken: String?,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
): ListConfiguredTableAssociationsResponse {
    val listConfiguredTableAssociationRequest = ListConfiguredTableAssociationsRequest.builder()
        .membershipIdentifier(membershipIdentifier)
        .apply { nextToken?.let { nextToken(it) } }
        .build()

    return proxy.injectCredentialsAndInvokeV2(
        listConfiguredTableAssociationRequest,
        cleanRoomsClient::listConfiguredTableAssociations
    )
}

fun createConfiguredTableAssociation(
    resourceModel: ResourceModel,
    desiredResourceTags: Map<String, String>?,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
): ConfiguredTableAssociation {
    with(resourceModel) {
        val createConfiguredTableAssociationRequest = CreateConfiguredTableAssociationRequest.builder()
            .name(name)
            .description(description)
            .membershipIdentifier(membershipIdentifier)
            .configuredTableIdentifier(configuredTableIdentifier)
            .roleArn(roleArn)
            .apply {
                desiredResourceTags?.let { tags(it) }
            }.build()

        val createConfiguredTableAssociationResponse = proxy.injectCredentialsAndInvokeV2(
            createConfiguredTableAssociationRequest,
            cleanRoomsClient::createConfiguredTableAssociation
        )

        return createConfiguredTableAssociationResponse.configuredTableAssociation()
    }
}

fun updateConfiguredTableAssociation(
    resourceModel: ResourceModel,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
) {
    with(resourceModel) {
        val updateConfiguredTableAssociationRequest = UpdateConfiguredTableAssociationRequest.builder()
            .configuredTableAssociationIdentifier(configuredTableAssociationIdentifier)
            .membershipIdentifier(membershipIdentifier)
            .description(description)
            .roleArn(roleArn)
            .build()

        proxy.injectCredentialsAndInvokeV2(
            updateConfiguredTableAssociationRequest,
            cleanRoomsClient::updateConfiguredTableAssociation
        )
    }
}

fun deleteConfiguredTableAssociation(
    configuredTableAssociationIdentifier: String,
    membershipIdentifier: String,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
) {
    val deleteConfiguredTableAssociationRequest = DeleteConfiguredTableAssociationRequest.builder()
        .configuredTableAssociationIdentifier(configuredTableAssociationIdentifier)
        .membershipIdentifier(membershipIdentifier)
        .build()

    proxy.injectCredentialsAndInvokeV2(
        deleteConfiguredTableAssociationRequest,
        cleanRoomsClient::deleteConfiguredTableAssociation
    )
}

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
