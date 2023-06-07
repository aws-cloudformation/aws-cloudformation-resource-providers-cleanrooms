package software.amazon.cleanrooms.membership.typemapper

import software.amazon.awssdk.services.cleanrooms.model.Membership
import software.amazon.cleanrooms.membership.ResourceModel
import software.amazon.cleanrooms.membership.Tag

/**
 * Convert a Membership object to a ResourceModel object
 */
fun Membership.toResourceModel(tags: Set<Tag>? = emptySet()): ResourceModel = ResourceModel.builder()
    .arn(arn())
    .tags(tags)
    .collaborationArn(collaborationArn())
    .collaborationCreatorAccountId(collaborationCreatorAccountId())
    .collaborationIdentifier(collaborationId())
    .membershipIdentifier(id())
    .queryLogStatus(queryLogStatusAsString())
    .build()
