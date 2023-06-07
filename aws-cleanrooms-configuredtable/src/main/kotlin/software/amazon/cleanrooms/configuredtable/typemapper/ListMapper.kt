package software.amazon.cleanrooms.configuredtable.typemapper

import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableSummary
import software.amazon.cleanrooms.configuredtable.ResourceModel

/**
 * Convert a List<ConfiguredTableSummary> to a List<ResourceModel>
 */
fun List<ConfiguredTableSummary>.toResourceModels(): List<ResourceModel> {
    return this.map { configuredTableSummary ->
        ResourceModel.builder()
            .configuredTableIdentifier(configuredTableSummary.id())
            .build()
    }
}
