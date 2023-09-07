package software.amazon.cleanrooms.analysistemplate

import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.awssdk.services.cleanrooms.model.AnalysisTemplate
import software.amazon.awssdk.services.cleanrooms.model.CreateAnalysisTemplateRequest
import software.amazon.awssdk.services.cleanrooms.model.DeleteAnalysisTemplateRequest
import software.amazon.awssdk.services.cleanrooms.model.GetAnalysisTemplateRequest
import software.amazon.awssdk.services.cleanrooms.model.ListAnalysisTemplatesRequest
import software.amazon.awssdk.services.cleanrooms.model.ListAnalysisTemplatesResponse
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.TagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.UntagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.UpdateAnalysisTemplateRequest
import software.amazon.cleanrooms.analysistemplate.typemapper.toSdkModel
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy

fun ResourceModel.getResourcePrimaryIdentifier(): String {
    return primaryIdentifier.getString(ResourceModel.IDENTIFIER_KEY_ANALYSISTEMPLATEIDENTIFIER)
        ?: error(
            "AnalysisTemplate key not found. " +
                "Check if the 'analysisTemplateIdentifier' is defined in the resource template before reading it."
        )
}
fun getAnalysisTemplate(
    analysisTemplateIdentifier: String,
    membershipIdentifier: String,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
): AnalysisTemplate {
    val getAnalysisTemplateRequest = GetAnalysisTemplateRequest.builder()
        .analysisTemplateIdentifier(analysisTemplateIdentifier)
        .membershipIdentifier(membershipIdentifier)
        .build()

    val getAnalysisTemplateResponse = proxy.injectCredentialsAndInvokeV2(
        getAnalysisTemplateRequest,
        cleanRoomsClient::getAnalysisTemplate
    )

    return getAnalysisTemplateResponse.analysisTemplate()
}

fun listAnalysisTemplates(
    membershipIdentifier: String,
    nextToken: String?,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
): ListAnalysisTemplatesResponse {
    val listAnalysisTemplateRequest = ListAnalysisTemplatesRequest.builder()
        .membershipIdentifier(membershipIdentifier)
        .apply { nextToken?.let { nextToken(it) } }
        .build()

    return proxy.injectCredentialsAndInvokeV2(
        listAnalysisTemplateRequest,
        cleanRoomsClient::listAnalysisTemplates
    )
}

fun createAnalysisTemplate(
    resourceModel: ResourceModel,
    desiredResourceTags: Map<String, String>?,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
): AnalysisTemplate {
    with(resourceModel) {
        val createAnalysisTemplateRequest = CreateAnalysisTemplateRequest.builder()
            .name(name)
            .membershipIdentifier(membershipIdentifier)
            .format(format)
            .source(source.toSdkModel())
            .apply {
                description?.let { description(it) }
                desiredResourceTags?.let { tags(it) }
                analysisParameters?.let {
                    analysisParameters(it.map { parameter -> parameter.toSdkModel() })
                }
            }.build()

        val createAnalysisTemplateResponse = proxy.injectCredentialsAndInvokeV2(
            createAnalysisTemplateRequest,
            cleanRoomsClient::createAnalysisTemplate
        )

        return createAnalysisTemplateResponse.analysisTemplate()
    }
}

fun updateAnalysisTemplate(
    resourceModel: ResourceModel,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
) {
    with(resourceModel) {
        val updateAnalysisTemplateRequest = UpdateAnalysisTemplateRequest.builder()
            .analysisTemplateIdentifier(analysisTemplateIdentifier)
            .membershipIdentifier(membershipIdentifier)
            .apply {
                description(description)
            }
            .build()

        proxy.injectCredentialsAndInvokeV2(
            updateAnalysisTemplateRequest,
            cleanRoomsClient::updateAnalysisTemplate
        )
    }
}

fun deleteAnalysisTemplate(
    analysisTemplateIdentifier: String,
    membershipIdentifier: String,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
) {
    val deleteAnalysisTemplateRequest = DeleteAnalysisTemplateRequest.builder()
        .analysisTemplateIdentifier(analysisTemplateIdentifier)
        .membershipIdentifier(membershipIdentifier)
        .build()

    proxy.injectCredentialsAndInvokeV2(
        deleteAnalysisTemplateRequest,
        cleanRoomsClient::deleteAnalysisTemplate
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
