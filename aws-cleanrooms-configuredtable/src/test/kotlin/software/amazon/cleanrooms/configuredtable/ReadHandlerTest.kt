package software.amazon.cleanrooms.configuredtable

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTable
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAnalysisRule
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableAnalysisRuleRequest
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableAnalysisRuleResponse
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableRequest
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableResponse
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.ResourceNotFoundException
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
            .configuredTableIdentifier(TEST_CONFIGURED_TABLE_ID)
            .build()
    )
    .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
    .build()

class ReadHandlerTest {

    /**
     * Type to define test arguments for the readHandler tests.
     */
    data class Args(
        val testName: String,
        val configuredTableFromApi: ConfiguredTable,
        val analysisRuleFromApi: ConfiguredTableAnalysisRule?,
        val tagsFromApi: Map<String, String>,
        val expectedResourceModel: ResourceModel
    )

    companion object {
        @JvmStatic
        fun readHandlerSuccessTestData() = listOf(
            Args(
                testName = "Configured Table with required fields reads successfully.",
                configuredTableFromApi = TEST_CONFIGURED_TABLE_REQUIRED_FIELDS,
                analysisRuleFromApi = null,
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS
            ),
            Args(
                testName = "Configured Table with required Agg analysis rule fields reads successfully.",
                configuredTableFromApi = TEST_CONFIGURED_TABLE_WITH_AGG_ANALYSIS_RULE,
                analysisRuleFromApi = TEST_AGG_ANALYSIS_RULE_REQUIRED_FIELDS,
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_RESOURCE_MODEL_WITH_REQUIRED_AGG_ANALYSIS_RULE
            ),
            Args(
                testName = "Configured Table with required fields, List analysis rule reads successfully.",
                configuredTableFromApi = TEST_CONFIGURED_TABLE_WITH_LIST_ANALYSIS_RULE,
                analysisRuleFromApi = TEST_LIST_ANALYSIS_RULE,
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_RESOURCE_MODEL_WITH_REQUIRED_LIST_ANALYSIS_RULE
            ),
            Args(
                testName = "Configured Table with required fields and tags reads successfully.",
                configuredTableFromApi = TEST_CONFIGURED_TABLE_REQUIRED_FIELDS,
                analysisRuleFromApi = null,
                tagsFromApi = TEST_CONFIGURED_TABLE_TAGS + TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS
            ),
            Args(
                testName = "Configured Table all fields reads successfully.",
                configuredTableFromApi = TEST_CONFIGURED_TABLE_WITH_AGG_ANALYSIS_RULE,
                analysisRuleFromApi = TEST_AGG_ANALYSIS_RULE,
                tagsFromApi = TEST_CONFIGURED_TABLE_TAGS + TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_RESOURCE_MODEL_WITH_AGG_ANALYSIS_RULE_AND_TAGS
            ),
        )
    }

    private val mockCleanRoomsClient: CleanRoomsClient = mockk()

    private val mockProxy: AmazonWebServicesClientProxy = mockk()

    private val logger: Logger = LoggerProxy();

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
                ofType(GetConfiguredTableRequest::class),
                any<Function<GetConfiguredTableRequest, GetConfiguredTableResponse>>()
            )
        } returns GetConfiguredTableResponse.builder()
            .configuredTable(testArgs.configuredTableFromApi)
            .build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableAnalysisRuleRequest::class),
                any<Function<GetConfiguredTableAnalysisRuleRequest, GetConfiguredTableAnalysisRuleResponse>>()
            )
        } returns GetConfiguredTableAnalysisRuleResponse.builder()
            .analysisRule(testArgs.analysisRuleFromApi)
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
                assertThat(configuredTableIdentifier).isEqualTo(TEST_CONFIGURED_TABLE_ID)
                assertThat(arn).isEqualTo(TEST_CONFIGURED_TABLE_ARN)
            }
        }
    }

    @Test
    fun `ReadHandler returns FAILED with NOT FOUND if configured table is not found during get configured table`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableRequest::class),
                any<Function<GetConfiguredTableRequest, GetConfiguredTableResponse>>()
            )
        } throws ResourceNotFoundException.builder().resourceId(TEST_CONFIGURED_TABLE_ID).build()

        val result = ReadHandler().handleRequest(mockProxy, TEST_READ_REQUEST, null, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `ReadHandler returns FAILED with NOT FOUND if configured table is not found during get configured table analysis rule`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableRequest::class),
                any<Function<GetConfiguredTableRequest, GetConfiguredTableResponse>>()
            )
        } returns TEST_GET_CONFIGURED_TABLE_WITH_AGG_ANALYSIS_RULE_RESPONSE
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableAnalysisRuleRequest::class),
                any<Function<GetConfiguredTableAnalysisRuleRequest, GetConfiguredTableAnalysisRuleResponse>>()
            )
        } throws ResourceNotFoundException.builder().resourceId(TEST_CONFIGURED_TABLE_ID).build()

        val result = ReadHandler().handleRequest(mockProxy, TEST_READ_REQUEST, null, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `ReadHandler returns FAILED with NOT FOUND if configured table is not found during list tags for resource`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableRequest::class),
                any<Function<GetConfiguredTableRequest, GetConfiguredTableResponse>>()
            )
        } returns TEST_GET_CONFIGURED_TABLE_WITH_AGG_ANALYSIS_RULE_RESPONSE
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableAnalysisRuleRequest::class),
                any<Function<GetConfiguredTableAnalysisRuleRequest, GetConfiguredTableAnalysisRuleResponse>>()
            )
        } returns GetConfiguredTableAnalysisRuleResponse.builder()
            .analysisRule(TEST_AGG_ANALYSIS_RULE)
            .build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListTagsForResourceRequest::class),
                any<Function<ListTagsForResourceRequest, ListTagsForResourceResponse>>()
            )
        } throws ResourceNotFoundException.builder().resourceId(TEST_CONFIGURED_TABLE_ID).build()

        val result = ReadHandler().handleRequest(mockProxy, TEST_READ_REQUEST, null, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }
}
