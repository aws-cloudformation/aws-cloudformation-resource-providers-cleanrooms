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
import software.amazon.awssdk.services.cleanrooms.model.AnalysisTemplateSummary
import software.amazon.awssdk.services.cleanrooms.model.ListAnalysisTemplatesRequest
import software.amazon.awssdk.services.cleanrooms.model.ListAnalysisTemplatesResponse
import software.amazon.awssdk.services.cleanrooms.model.ValidationException
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_ANALYSIS_TEMPLATE_SUMMARY_1
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_ANALYSIS_TEMPLATE_SUMMARY_2
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_CREATOR_ACCOUNT_ID
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_EXPECTED_NEXT_TOKEN
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_MEMBERSHIP_ID
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_NEXT_TOKEN
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_RESOURCE_MODEL_SUMMARY_1
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_RESOURCE_MODEL_SUMMARY_2
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.HandlerErrorCode
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import java.util.function.Function

private val TEST_LIST_REQUEST = ResourceHandlerRequest.builder<ResourceModel>()
    .nextToken(TEST_NEXT_TOKEN)
    .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
    .desiredResourceState(
        ResourceModel.builder()
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .build()
    )
    .build()

class ListHandlerTest {

    /**
     * Type to define test arguments for the listHandler tests.
     */
    data class Args(
        val testName: String,
        val analysisTemplateSummariesFromApi: List<AnalysisTemplateSummary>,
        val nextToken: String?,
        val nextTokenFromApi: String?,
        val expectedResourceModels: List<ResourceModel>
    )

    companion object {
        @JvmStatic
        fun listHandlerSuccessTestData() = listOf(
            Args(
                testName = "No nextToken input returns empty list when no analysis templates found successfully.",
                analysisTemplateSummariesFromApi = emptyList(),
                nextToken = null,
                nextTokenFromApi = null,
                expectedResourceModels = emptyList()
            ),
            Args(
                testName = "No nextToken input returns analysis template  id list and no next token successfully.",
                analysisTemplateSummariesFromApi = listOf(TEST_ANALYSIS_TEMPLATE_SUMMARY_1),
                nextToken = null,
                nextTokenFromApi = null,
                expectedResourceModels = listOf(TEST_RESOURCE_MODEL_SUMMARY_1)
            ),
            Args(
                testName = "nextToken input returns configured association table id list and no next token successfully.",
                analysisTemplateSummariesFromApi = listOf(TEST_ANALYSIS_TEMPLATE_SUMMARY_1),
                nextToken = TEST_NEXT_TOKEN,
                nextTokenFromApi = null,
                expectedResourceModels = listOf(TEST_RESOURCE_MODEL_SUMMARY_1)
            ),
            Args(
                testName = "No nextToken input returns analysis template id list and next token successfully.",
                analysisTemplateSummariesFromApi = listOf(TEST_ANALYSIS_TEMPLATE_SUMMARY_1, TEST_ANALYSIS_TEMPLATE_SUMMARY_2),
                nextToken = null,
                nextTokenFromApi = TEST_EXPECTED_NEXT_TOKEN,
                expectedResourceModels = listOf(TEST_RESOURCE_MODEL_SUMMARY_1, TEST_RESOURCE_MODEL_SUMMARY_2)
            )
        )
    }

    private val mockCleanRoomsClient: CleanRoomsClient = mockk()

    private val mockProxy: AmazonWebServicesClientProxy = mockk()

    private val logger: Logger = LoggerProxy()

    private val handler: ListHandler = ListHandler()

    @BeforeEach
    fun setup() {
        mockkObject(ClientBuilder)
        every { ClientBuilder.getCleanRoomsClient() } returns mockCleanRoomsClient
    }

    @ParameterizedTest
    @MethodSource("listHandlerSuccessTestData")
    fun `ListHandler returns SUCCESS with correct resourceModels and nextToken set`(testArgs: Args) {
        // Setup mock behavior.
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListAnalysisTemplatesRequest::class),
                any<Function<ListAnalysisTemplatesRequest, ListAnalysisTemplatesResponse>>()
            )
        } returns ListAnalysisTemplatesResponse.builder()
            .analysisTemplateSummaries(testArgs.analysisTemplateSummariesFromApi)
            .nextToken(testArgs.nextTokenFromApi)
            .build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, TEST_LIST_REQUEST, null, logger)

        assertThat(result).isNotNull
        with(result) {
            // Checking the status is SUCCESS
            assertThat(status).isEqualTo(OperationStatus.SUCCESS)
            assertThat(nextToken).isEqualTo(testArgs.nextTokenFromApi)
            with(resourceModels) {
                assertThat(this).isEqualTo(testArgs.expectedResourceModels)
            }
        }
    }

    @Test
    fun `ListHandler returns FAILED with INVALID REQUEST if list analysis templates returns validation exception `() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListAnalysisTemplatesRequest::class),
                any<Function<ListAnalysisTemplatesRequest, ListAnalysisTemplatesResponse>>()
            )
        } throws ValidationException.builder()
            .build()

        val result = handler.handleRequest(mockProxy, TEST_LIST_REQUEST, null, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.InvalidRequest)
        }
    }
}
