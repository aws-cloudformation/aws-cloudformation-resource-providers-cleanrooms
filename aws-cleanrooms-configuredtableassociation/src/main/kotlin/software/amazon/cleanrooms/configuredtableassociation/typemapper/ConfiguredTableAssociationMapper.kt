package software.amazon.cleanrooms.configuredtableassociation.typemapper

import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAssociation
import software.amazon.cleanrooms.configuredtableassociation.ResourceModel
import software.amazon.cleanrooms.configuredtableassociation.Tag

/**
 * Convert a ConfiguredTableAssociation object to a ResourceModel object
 */
fun ConfiguredTableAssociation.toResourceModel(tags: List<Tag>? = emptyList()): ResourceModel =
    ResourceModel.builder()
        .arn(arn())
        .name(name())
        .configuredTableAssociationIdentifier(id())
        .configuredTableIdentifier(configuredTableId())
        .description(description())
        .membershipIdentifier(membershipId())
        .roleArn(roleArn())
        .tags(tags)
        .build()
