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
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_ANALYSIS_TEMPLATE_BASE_WITH_REQUIRED_FIELDS
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_ANALYSIS_TEMPLATE_REQUIRED_FIELDS
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_ANALYSIS_TEMPLATE_TAGS
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_ANALYSIS_TEMPLATE_WITH_ALL_FIELDS
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_CREATOR_ACCOUNT_ID
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_AT_ARN
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_AT_ID
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_GET_AT_RESPONSE
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_MEMBERSHIP_ID
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_RESOURCE_MODEL_REQUIRED_FIELDS
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_RESOURCE_MODEL_WITH_ALL_FIELDS
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_SYSTEM_TAGS
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.HandlerErrorCode
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import java.util.function.Function

private val TEST_READ_REQUEST = ResourceHandlerRequest.builder<ResourceModel>()
    .desiredResourceState(
        ResourceModel.builder()
            .analysisTemplateIdentifier(TEST_AT_ID)
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .build()
    )
    .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
    .build()

class ReadHandlerTest {
    data class Args(
        val testName: String,
        val analysisTemplateFromApi: AnalysisTemplate,
        val tagsFromApi: Map<String, String>,
        val expectedResourceModel: ResourceModel
    )

    companion object {
        @JvmStatic
        fun readHandlerSuccessTestData() = listOf(
            Args(
                testName = "Analysis Template  with required fields reads successfully.",
                analysisTemplateFromApi = TEST_ANALYSIS_TEMPLATE_REQUIRED_FIELDS,
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS
            ),
            Args(
                testName = "Analysis Template with required fields and tags reads successfully.",
                analysisTemplateFromApi = TEST_ANALYSIS_TEMPLATE_REQUIRED_FIELDS,
                tagsFromApi = TEST_ANALYSIS_TEMPLATE_TAGS + TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS
            ),
            Args(
                testName = "Analysis Template with optional fields reads successfully.",
                analysisTemplateFromApi = TEST_ANALYSIS_TEMPLATE_WITH_ALL_FIELDS,
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_RESOURCE_MODEL_WITH_ALL_FIELDS
            )
        )
    }

    private val logger: Logger = LoggerProxy()

    private val mockCleanRoomsClient: CleanRoomsClient = mockk()

    private val mockProxy: AmazonWebServicesClientProxy = mockk()

    private val handler: ReadHandler = ReadHandler()

    @BeforeEach
    fun setup() {
        mockkObject(ClientBuilder)
        every { ClientBuilder.getCleanRoomsClient() } returns mockCleanRoomsClient
    }

    @ParameterizedTest
    @MethodSource("readHandlerSuccessTestData")
    fun `ReadHandler returns SUCCESS with correct resourceModel and primaryIdentifier set`(testArgs: Args) {
        // Setup mock behavior.
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetAnalysisTemplateRequest::class),
                any<Function<GetAnalysisTemplateRequest, GetAnalysisTemplateResponse>>()
            )
        } returns GetAnalysisTemplateResponse.builder()
            .analysisTemplate(testArgs.analysisTemplateFromApi)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListTagsForResourceRequest::class),
                any<Function<ListTagsForResourceRequest, ListTagsForResourceResponse>>()
            )
        } returns ListTagsForResourceResponse.builder().tags(testArgs.tagsFromApi).build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, TEST_READ_REQUEST, null, logger)

        assertThat(result).isNotNull
        with(result) {
            // Checking the status is SUCCESS
            assertThat(status).isEqualTo(OperationStatus.SUCCESS)
            with(resourceModel) {
                // A read handler MUST return a model representation that conforms to the shape of the resource schema.
                assertThat(this).isEqualTo(testArgs.expectedResourceModel)
                // The model MUST contain all properties that have values, including any properties that have default values
                // and any readOnlyProperties as defined in the resource schema.
                assertThat(analysisTemplateIdentifier).isEqualTo(TEST_AT_ID)
                assertThat(arn).isEqualTo(TEST_AT_ARN)
            }
        }
    }

    @Test
    fun `ReadHandler returns FAILED with NOT FOUND if cta is not found during get cta`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetAnalysisTemplateRequest::class),
                any<Function<GetAnalysisTemplateRequest, GetAnalysisTemplateResponse>>()
            )
        } throws ResourceNotFoundException.builder().resourceId(TEST_AT_ID).build()

        val result = ReadHandler().handleRequest(mockProxy, TEST_READ_REQUEST, null, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `ReadHandler returns FAILED with NOT FOUND if cta is not found during list tags for resource`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetAnalysisTemplateRequest::class),
                any<Function<GetAnalysisTemplateRequest, GetAnalysisTemplateResponse>>()
            )
        } returns TEST_GET_AT_RESPONSE
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListTagsForResourceRequest::class),
                any<Function<ListTagsForResourceRequest, ListTagsForResourceResponse>>()
            )
        } throws ResourceNotFoundException.builder().resourceId(TEST_AT_ID).build()

        val result = ReadHandler().handleRequest(mockProxy, TEST_READ_REQUEST, null, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }
}
