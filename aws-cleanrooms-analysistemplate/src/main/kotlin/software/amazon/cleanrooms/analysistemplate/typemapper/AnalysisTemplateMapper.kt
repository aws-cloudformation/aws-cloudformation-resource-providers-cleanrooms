package software.amazon.cleanrooms.analysistemplate.typemapper

import software.amazon.awssdk.services.cleanrooms.model.AnalysisParameter as SdkAnalysisParameter
import software.amazon.awssdk.services.cleanrooms.model.AnalysisSchema as SdkAnalysisSchema
import software.amazon.awssdk.services.cleanrooms.model.AnalysisSource as SdkAnalysisSource
import software.amazon.awssdk.services.cleanrooms.model.AnalysisTemplate
import software.amazon.cleanrooms.analysistemplate.AnalysisParameter
import software.amazon.cleanrooms.analysistemplate.AnalysisSchema
import software.amazon.cleanrooms.analysistemplate.AnalysisSource
import software.amazon.cleanrooms.analysistemplate.ResourceModel
import software.amazon.cleanrooms.analysistemplate.Tag

/**
 * Convert a AnalysisTemplate object to a ResourceModel object
 */
fun AnalysisTemplate.toResourceModel(tags: List<Tag>? = emptyList()): ResourceModel =
    ResourceModel.builder()
        .apply {
            arn(arn())
            name(name())
            analysisTemplateIdentifier(id())
            source(source().toResourceModel())
            format(format().name)
            schema(schema().toResourceModel())
            analysisParameters()?.let {
                analysisParameters(
                    it.map { analysisParameter ->  analysisParameter.toResourceModel() }
                )
            }
            description()?.let { description(it) }
            membershipIdentifier(membershipId())
            membershipArn(membershipArn())
            collaborationIdentifier(collaborationId())
            collaborationArn(collaborationArn())
            tags?.let { tags(it) }
        }
        .build()


/**
 * Convert a AnalysisSource object to a ResourceModel object
 */
fun SdkAnalysisSource.toResourceModel(): AnalysisSource =
    AnalysisSource.builder()
        .apply { text()?.let { text(it) } }
        .build()

/**
 * Convert a AnalysisSource object to a Sdk object
 */
fun AnalysisSource.toSdkModel(): SdkAnalysisSource =
    SdkAnalysisSource.builder()
        .apply { text?.let { text(it) } }
        .build()

/**
 * Convert a AnalysisSource object to a ResourceModel object
 */
fun SdkAnalysisSchema.toResourceModel(): AnalysisSchema =
    AnalysisSchema.builder()
        .apply {
            referencedTables()?.let { referencedTables(it) }
        }
        .build()

/**
 * Convert a AnalysisSource object to a ResourceModel object
 */
fun SdkAnalysisParameter.toResourceModel(): AnalysisParameter =
    AnalysisParameter.builder()
        .apply {
            name(name())
            type(typeAsString())
            defaultValue()?.let { defaultValue(it) }
        }
        .build()

/**
 * Convert a ResourceModel parameter object to a SDK object
 */
fun AnalysisParameter.toSdkModel(): SdkAnalysisParameter =
    SdkAnalysisParameter.builder()
        .apply {
            name(name)
            type(type)
            defaultValue?.let { defaultValue(it) }
        }
        .build()
