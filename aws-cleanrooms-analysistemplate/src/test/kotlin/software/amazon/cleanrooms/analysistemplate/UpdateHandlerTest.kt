package software.amazon.cleanrooms.analysistemplate

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.awssdk.services.cleanrooms.model.AnalysisTemplate
import software.amazon.awssdk.services.cleanrooms.model.GetAnalysisTemplateRequest
import software.amazon.awssdk.services.cleanrooms.model.GetAnalysisTemplateResponse
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.ResourceNotFoundException
import software.amazon.awssdk.services.cleanrooms.model.TagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.TagResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.UntagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.UntagResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.UpdateAnalysisTemplateRequest
import software.amazon.awssdk.services.cleanrooms.model.UpdateAnalysisTemplateResponse
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_ANALYSIS_TEMPLATE_REQUIRED_FIELDS
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_ANALYSIS_TEMPLATE_TAGS
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_ANALYSIS_TEMPLATE_WITH_ALL_FIELDS
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_CREATOR_ACCOUNT_ID
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_AT_ARN
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_AT_ID
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_RESOURCE_MODEL_REQUIRED_FIELDS
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_RESOURCE_MODEL_WITH_ALL_FIELDS
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_SYSTEM_TAGS
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_UPDATED_ANALYSIS_TEMPLATE_TAGS
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_UPDATE_DESCRIPTION_ANALYSIS_TEMPLATE
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_UPDATE_DESCRIPTION_RESOURCE_MODEL
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.HandlerErrorCode
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import java.util.function.Function

class UpdateHandlerTest {
    /**
     * Type to define test arguments for handler tests.
     */
    data class Args(
        val testName: String,
        val requestDesiredResourceModel: ResourceModel,
        val requestPreviousResourceModel: ResourceModel,
        val requestDesiredResourceTags: Map<String, String>,
        val requestPreviousResourceTags: Map<String, String>,
        val sdkResponseList: List<SdkResponse>,
        val expectedResourceModel: ResourceModel
    )
    /**
     * Tests need multiple responses as ReadHandler is called more than once
     */
    data class SdkResponse(
        val analysisTemplateFromApi: AnalysisTemplate,
        val tagsFromApi: Map<String, String>,
    )
    companion object {
        @JvmStatic
        fun updateHandlerSuccessTestData() = listOf(
            Args(
                testName = "AnalysisTemplate Description is updated successfully.",
                requestDesiredResourceModel = TEST_UPDATE_DESCRIPTION_RESOURCE_MODEL,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL_WITH_ALL_FIELDS,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        analysisTemplateFromApi = TEST_ANALYSIS_TEMPLATE_WITH_ALL_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        analysisTemplateFromApi = TEST_UPDATE_DESCRIPTION_ANALYSIS_TEMPLATE,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_UPDATE_DESCRIPTION_RESOURCE_MODEL
            ),
            Args(
                testName = "AnalysisTemplate tags are removed successfully.",
                requestDesiredResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = TEST_ANALYSIS_TEMPLATE_TAGS,
                sdkResponseList = listOf(
                    SdkResponse(
                        analysisTemplateFromApi = TEST_ANALYSIS_TEMPLATE_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        analysisTemplateFromApi = TEST_ANALYSIS_TEMPLATE_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS
            ),
            Args(
                testName = "AnalysisTemplate when new tag keys are added, tags are added successfully.",
                requestDesiredResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS,
                requestDesiredResourceTags = TEST_ANALYSIS_TEMPLATE_TAGS,
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        analysisTemplateFromApi = TEST_ANALYSIS_TEMPLATE_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        analysisTemplateFromApi = TEST_ANALYSIS_TEMPLATE_REQUIRED_FIELDS,
                        tagsFromApi = TEST_ANALYSIS_TEMPLATE_TAGS + TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS
            ),
            Args(
                testName = "AnalysisTemplate when tag values are modified, tags are updated successfully.",
                requestDesiredResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS,
                requestDesiredResourceTags = TEST_ANALYSIS_TEMPLATE_TAGS,
                requestPreviousResourceTags = TEST_UPDATED_ANALYSIS_TEMPLATE_TAGS,
                sdkResponseList = listOf(
                    SdkResponse(
                        analysisTemplateFromApi = TEST_ANALYSIS_TEMPLATE_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        analysisTemplateFromApi = TEST_ANALYSIS_TEMPLATE_REQUIRED_FIELDS,
                        tagsFromApi = TEST_ANALYSIS_TEMPLATE_TAGS + TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS
            ),
        )
    }

    private val mockCleanRoomsClient: CleanRoomsClient = mockk()

    private val mockProxy: AmazonWebServicesClientProxy = mockk()

    private val logger: Logger = LoggerProxy()

    private val handler: UpdateHandler = UpdateHandler()

    @BeforeEach
    fun setup() {
        mockkObject(ClientBuilder)
        every { ClientBuilder.getCleanRoomsClient() } returns mockCleanRoomsClient
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(UpdateAnalysisTemplateRequest::class),
                any<Function<UpdateAnalysisTemplateRequest, UpdateAnalysisTemplateResponse>>()
            )
        } returns UpdateAnalysisTemplateResponse.builder().build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetAnalysisTemplateRequest::class),
                any<Function<GetAnalysisTemplateRequest, GetAnalysisTemplateResponse>>()
            )
        } returns GetAnalysisTemplateResponse.builder()
            .analysisTemplate(TEST_ANALYSIS_TEMPLATE_REQUIRED_FIELDS)
            .build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListTagsForResourceRequest::class),
                any<Function<ListTagsForResourceRequest, ListTagsForResourceResponse>>()
            )
        } returns ListTagsForResourceResponse.builder().tags(TEST_SYSTEM_TAGS).build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(TagResourceRequest::class),
                any<Function<TagResourceRequest, TagResourceResponse>>()
            )
        } returns TagResourceResponse.builder().build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(UntagResourceRequest::class),
                any<Function<UntagResourceRequest, UntagResourceResponse>>()
            )
        } returns UntagResourceResponse.builder().build()
    }

    @ParameterizedTest
    @MethodSource("updateHandlerSuccessTestData")
    fun `UpdateHandler returns SUCCESS with correct resourceModel and primaryIdentifier set`(testArgs: Args) {
        // Setup mock behavior.

        val firstGetAnalysisTemplateResponse = GetAnalysisTemplateResponse.builder()
            .analysisTemplate(testArgs.sdkResponseList[0].analysisTemplateFromApi)
            .build()
        val secondGetAnalysisTemplateResponse = GetAnalysisTemplateResponse.builder()
            .analysisTemplate(testArgs.sdkResponseList[1].analysisTemplateFromApi)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetAnalysisTemplateRequest::class),
                any<Function<GetAnalysisTemplateRequest, GetAnalysisTemplateResponse>>()
            )
        } returns firstGetAnalysisTemplateResponse andThen secondGetAnalysisTemplateResponse

        val firstListTagsForResourceResponse = ListTagsForResourceResponse.builder()
            .tags(testArgs.sdkResponseList[0].tagsFromApi)
            .build()
        val secondListTagsForResourceResponse = ListTagsForResourceResponse.builder()
            .tags(testArgs.sdkResponseList[1].tagsFromApi)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListTagsForResourceRequest::class),
                any<Function<ListTagsForResourceRequest, ListTagsForResourceResponse>>()
            )
        } returns firstListTagsForResourceResponse andThen secondListTagsForResourceResponse

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(testArgs.requestDesiredResourceModel)
            .previousResourceState(testArgs.requestPreviousResourceModel)
            .desiredResourceTags(testArgs.requestDesiredResourceTags)
            .previousResourceTags(testArgs.requestPreviousResourceTags)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, request, null, logger)

        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.SUCCESS)
            with(resourceModel) {
                assertThat(this).isEqualTo(testArgs.expectedResourceModel)
                assertThat(analysisTemplateIdentifier).isEqualTo(TEST_AT_ID)
                assertThat(arn).isEqualTo(TEST_AT_ARN)
            }
        }
    }

    @Test
    fun `UpdateHandler returns FAILED with NotFound if verify resource exists check fails`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetAnalysisTemplateRequest::class),
                any<Function<GetAnalysisTemplateRequest, GetAnalysisTemplateResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_AT_ID)
            .build()

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_UPDATE_DESCRIPTION_RESOURCE_MODEL)
            .previousResourceState(TEST_RESOURCE_MODEL_WITH_ALL_FIELDS)
            .build()

        val response = handler.handleRequest(mockProxy, request, null, logger)
        assertThat(response).isNotNull
        with(response) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
            assertThat(resourceModel).isNull()
        }
    }

    @Test
    fun `UpdateHandler returns FAILED with NotFound if configured table association is not found during update configured table association`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(UpdateAnalysisTemplateRequest::class),
                any<Function<UpdateAnalysisTemplateRequest, UpdateAnalysisTemplateResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_AT_ID)
            .build()

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_UPDATE_DESCRIPTION_RESOURCE_MODEL)
            .previousResourceState(TEST_RESOURCE_MODEL_WITH_ALL_FIELDS)
            .build()

        val response = handler.handleRequest(mockProxy, request, null, logger)
        assertThat(response).isNotNull
        with(response) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `UpdateHandler returns FAILED with NotFound with no model if configured table association is not found and ReadHandler fails`() {

        val firstGetAnalysisTemplateResponse = GetAnalysisTemplateResponse.builder()
            .analysisTemplate(TEST_ANALYSIS_TEMPLATE_REQUIRED_FIELDS)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetAnalysisTemplateRequest::class),
                any<Function<GetAnalysisTemplateRequest, GetAnalysisTemplateResponse>>()
            )
        } returns firstGetAnalysisTemplateResponse andThenThrows ResourceNotFoundException.builder()
            .resourceId(TEST_AT_ID)
            .build()

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_RESOURCE_MODEL_REQUIRED_FIELDS)
            .previousResourceState(TEST_RESOURCE_MODEL_REQUIRED_FIELDS)
            .build()

        val response = handler.handleRequest(mockProxy, request, null, logger)
        assertThat(response).isNotNull
        with(response) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
            assertThat(resourceModel).isNull()
        }
    }
}
