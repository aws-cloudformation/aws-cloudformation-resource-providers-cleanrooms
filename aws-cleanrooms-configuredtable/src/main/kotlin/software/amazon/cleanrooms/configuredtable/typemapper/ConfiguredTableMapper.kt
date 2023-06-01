package software.amazon.cleanrooms.configuredtable.typemapper

import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTable
import software.amazon.cleanrooms.configuredtable.AnalysisRule
import software.amazon.cleanrooms.configuredtable.GlueTableReference
import software.amazon.cleanrooms.configuredtable.ResourceModel
import software.amazon.cleanrooms.configuredtable.TableReference
import software.amazon.cleanrooms.configuredtable.Tag

/**
 * Convert a ConfiguredTable object to a ResourceModel object
 */
fun ConfiguredTable.toResourceModel(analysisRules: List<AnalysisRule>, tags: Set<Tag>? = emptySet()): ResourceModel =
    ResourceModel.builder()
        .allowedColumns(allowedColumns())
        .configuredTableIdentifier(id())
        .analysisMethod(analysisMethodAsString())
        .arn(arn())
        .description(description())
        .name(name())
        .tableReference(
            TableReference.builder()
                .glue(
                    GlueTableReference.builder()
                        .tableName(tableReference().glue().tableName())
                        .databaseName(tableReference().glue().databaseName())
                        .build()
                )
                .build()
        )
        .apply { if (analysisRules.isNotEmpty()) analysisRules(analysisRules) }
        .tags(tags)
        .build()
