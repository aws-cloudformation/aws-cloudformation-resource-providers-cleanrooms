package software.amazon.cleanrooms.analysistemplate.typemapper

import software.amazon.awssdk.services.cleanrooms.model.AnalysisTemplateSummary
import software.amazon.cleanrooms.analysistemplate.ResourceModel

/**
 * Convert a List<AnalysisTemplateSummary> to a List<ResourceModel>
 */
fun Collection<AnalysisTemplateSummary>.toResourceModels(membershipId: String): List<ResourceModel> =
    map { analysisTemplateSummary ->
        ResourceModel.builder()
            .analysisTemplateIdentifier(analysisTemplateSummary.id())
            .membershipIdentifier(membershipId)
            .build()
    }
