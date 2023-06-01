package software.amazon.cleanrooms.collaboration.typemapper

import software.amazon.awssdk.services.cleanrooms.model.Collaboration
import software.amazon.cleanrooms.collaboration.DataEncryptionMetadata
import software.amazon.cleanrooms.collaboration.MemberSpecification
import software.amazon.cleanrooms.collaboration.ResourceModel
import software.amazon.cleanrooms.collaboration.Tag

/**
 * Convert a Collaboration object to a ResourceModel object
 */
fun Collaboration.toResourceModel(creatorMember: MemberSpecification, nonCreatorMembers: List<MemberSpecification> = emptyList(), tags: Set<Tag>? = emptySet()): ResourceModel =
    ResourceModel.builder()
        .apply {
            arn(arn())
            creatorDisplayName(creatorDisplayName())
            description(description())
            collaborationIdentifier(id())
            members(nonCreatorMembers)
            creatorMemberAbilities(
                creatorMember.memberAbilities
            )
            // This can be confusing to read but it is a kotlin construct which we have to use here.
            // It is disambiguating that first `dataEncryptionMetadata` is coming from the method level ( i.e. collaboration)
            // and that the second one is the property of resource model.
            this@toResourceModel.dataEncryptionMetadata()?.let {
                dataEncryptionMetadata(
                    with(it) {
                        DataEncryptionMetadata.builder()
                            .allowCleartext(allowCleartext())
                            .allowDuplicates(allowDuplicates())
                            .allowJoinsOnColumnsWithDifferentNames(allowJoinsOnColumnsWithDifferentNames())
                            .preserveNulls(preserveNulls())
                            .build()
                    }
                )
            }
            name(name())
            tags(tags)
            queryLogStatus(queryLogStatusAsString())
        }
        .build()
