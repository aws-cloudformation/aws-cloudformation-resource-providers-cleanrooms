package software.amazon.cleanrooms.configuredtableassociation.typemapper

import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAssociationSummary
import software.amazon.cleanrooms.configuredtableassociation.ResourceModel

/**
 * Convert a List<ConfiguredTableAssociationSummary> to a List<ResourceModel>
 */
fun Collection<ConfiguredTableAssociationSummary>.toResourceModels(membershipId: String): List<ResourceModel> =
    map { configuredTableAssociationSummary ->
        ResourceModel.builder()
            .configuredTableAssociationIdentifier(configuredTableAssociationSummary.id())
            .membershipIdentifier(membershipId)
            .build()
    }
