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
import software.amazon.awssdk.services.cleanrooms.model.CreateConfiguredTableAnalysisRuleRequest
import software.amazon.awssdk.services.cleanrooms.model.CreateConfiguredTableAnalysisRuleResponse
import software.amazon.awssdk.services.cleanrooms.model.DeleteConfiguredTableAnalysisRuleRequest
import software.amazon.awssdk.services.cleanrooms.model.DeleteConfiguredTableAnalysisRuleResponse
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableAnalysisRuleRequest
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableAnalysisRuleResponse
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableRequest
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableResponse
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.ResourceNotFoundException
import software.amazon.awssdk.services.cleanrooms.model.TagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.TagResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.UntagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.UntagResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.UpdateConfiguredTableRequest
import software.amazon.awssdk.services.cleanrooms.model.UpdateConfiguredTableResponse
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
        val configuredTableFromApi: ConfiguredTable,
        val analysisRuleFromApi: ConfiguredTableAnalysisRule?,
        val tagsFromApi: Map<String, String>,
    )
    companion object {
        @JvmStatic
        fun updateHandlerSuccessTestData() = listOf(
            Args(
                testName = "ConfiguredTable Name is updated successfully.",
                requestDesiredResourceModel = TEST_RESOURCE_MODEL,
                requestPreviousResourceModel = TEST_PREVIOUS_NAME_RESOURCE_MODEL,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        configuredTableFromApi = TEST_CONFIGURED_TABLE,
                        analysisRuleFromApi = null,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        configuredTableFromApi = TEST_CONFIGURED_TABLE,
                        analysisRuleFromApi = null,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_RESOURCE_MODEL
            ),
            Args(
                testName = "ConfiguredTable Description is updated successfully.",
                requestDesiredResourceModel = TEST_RESOURCE_MODEL,
                requestPreviousResourceModel = TEST_NULL_DESC_RESOURCE_MODEL,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        configuredTableFromApi = TEST_CONFIGURED_TABLE,
                        analysisRuleFromApi = null,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        configuredTableFromApi = TEST_CONFIGURED_TABLE,
                        analysisRuleFromApi = null,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_RESOURCE_MODEL
            ),
            Args(
                testName = "ConfiguredTable updating with new Aggregation AnalysisRule when no existing AnalysisRule is successful.",
                requestDesiredResourceModel = TEST_RESOURCE_MODEL_WITH_REQUIRED_AGG_ANALYSIS_RULE,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        configuredTableFromApi = TEST_CONFIGURED_TABLE,
                        analysisRuleFromApi = TEST_AGG_ANALYSIS_RULE_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        configuredTableFromApi = TEST_CONFIGURED_TABLE_WITH_AGG_ANALYSIS_RULE,
                        analysisRuleFromApi = TEST_AGG_ANALYSIS_RULE_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_RESOURCE_MODEL_WITH_REQUIRED_AGG_ANALYSIS_RULE
            ),
            Args(
                testName = "ConfiguredTable removing existing AnalysisRule is successful.",
                requestDesiredResourceModel = TEST_RESOURCE_MODEL,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL_WITH_REQUIRED_AGG_ANALYSIS_RULE,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        configuredTableFromApi = TEST_CONFIGURED_TABLE_WITH_AGG_ANALYSIS_RULE,
                        analysisRuleFromApi = TEST_AGG_ANALYSIS_RULE_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        configuredTableFromApi = TEST_CONFIGURED_TABLE,
                        analysisRuleFromApi = null,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_RESOURCE_MODEL
            ),
            Args(
                testName = "ConfiguredTable AnalysisRule is updated from Aggregation List successfully.",
                requestDesiredResourceModel = TEST_RESOURCE_MODEL_WITH_REQUIRED_LIST_ANALYSIS_RULE,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL_WITH_REQUIRED_AGG_ANALYSIS_RULE,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        configuredTableFromApi = TEST_CONFIGURED_TABLE_WITH_AGG_ANALYSIS_RULE,
                        analysisRuleFromApi = TEST_AGG_ANALYSIS_RULE,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        configuredTableFromApi = TEST_CONFIGURED_TABLE_WITH_LIST_ANALYSIS_RULE,
                        analysisRuleFromApi = TEST_LIST_ANALYSIS_RULE,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_RESOURCE_MODEL_WITH_REQUIRED_LIST_ANALYSIS_RULE
            ),
            Args(
                testName = "ConfiguredTable tags are removed successfully.",
                requestDesiredResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = TEST_CONFIGURED_TABLE_TAGS,
                sdkResponseList = listOf(
                    SdkResponse(
                        configuredTableFromApi = TEST_CONFIGURED_TABLE,
                        analysisRuleFromApi = null,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        configuredTableFromApi = TEST_CONFIGURED_TABLE,
                        analysisRuleFromApi = null,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_RESOURCE_MODEL
            ),
            Args(
                testName = "ConfiguredTable when new tag keys are added, tags are added successfully.",
                requestDesiredResourceModel = TEST_NULL_DESC_RESOURCE_MODEL,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS,
                requestDesiredResourceTags = TEST_CONFIGURED_TABLE_TAGS,
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        configuredTableFromApi = TEST_CONFIGURED_TABLE,
                        analysisRuleFromApi = null,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        configuredTableFromApi = TEST_CONFIGURED_TABLE_REQUIRED_FIELDS,
                        analysisRuleFromApi = null,
                        tagsFromApi = TEST_CONFIGURED_TABLE_TAGS + TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS
            ),
            Args(
                testName = "ConfiguredTable when tag values are modified, tags are updated successfully.",
                requestDesiredResourceModel = TEST_NULL_DESC_RESOURCE_MODEL,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS,
                requestDesiredResourceTags = TEST_CONFIGURED_TABLE_TAGS,
                requestPreviousResourceTags = TEST_UPDATED_CONFIGURED_TABLE_TAGS,
                sdkResponseList = listOf(
                    SdkResponse(
                        configuredTableFromApi = TEST_CONFIGURED_TABLE,
                        analysisRuleFromApi = null,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        configuredTableFromApi = TEST_CONFIGURED_TABLE_REQUIRED_FIELDS,
                        analysisRuleFromApi = null,
                        tagsFromApi = TEST_CONFIGURED_TABLE_TAGS + TEST_SYSTEM_TAGS,
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
                ofType(UpdateConfiguredTableRequest::class),
                any<Function<UpdateConfiguredTableRequest, UpdateConfiguredTableResponse>>()
            )
        } returns UpdateConfiguredTableResponse.builder().build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(CreateConfiguredTableAnalysisRuleRequest::class),
                any<Function<CreateConfiguredTableAnalysisRuleRequest, CreateConfiguredTableAnalysisRuleResponse>>()
            )
        } returns CreateConfiguredTableAnalysisRuleResponse.builder().build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableRequest::class),
                any<Function<GetConfiguredTableRequest, GetConfiguredTableResponse>>()
            )
        } returns GetConfiguredTableResponse.builder()
            .configuredTable(TEST_CONFIGURED_TABLE_WITH_AGG_ANALYSIS_RULE)
            .build()
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
                ofType(DeleteConfiguredTableAnalysisRuleRequest::class),
                any<Function<DeleteConfiguredTableAnalysisRuleRequest, DeleteConfiguredTableAnalysisRuleResponse>>()
            )
        } returns DeleteConfiguredTableAnalysisRuleResponse.builder().build()
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

        val firstGetConfiguredTableResponse = GetConfiguredTableResponse.builder()
            .configuredTable(testArgs.sdkResponseList[0].configuredTableFromApi)
            .build()
        val secondGetConfiguredTableResponse = GetConfiguredTableResponse.builder()
            .configuredTable(testArgs.sdkResponseList[1].configuredTableFromApi)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableRequest::class),
                any<Function<GetConfiguredTableRequest, GetConfiguredTableResponse>>()
            )
        } returns firstGetConfiguredTableResponse andThen secondGetConfiguredTableResponse

        val firstGetConfiguredTableAnalysisRuleResponse = GetConfiguredTableAnalysisRuleResponse.builder()
            .analysisRule(testArgs.sdkResponseList[0].analysisRuleFromApi)
            .build()
        val secondGetConfiguredTableAnalysisRuleResponse = GetConfiguredTableAnalysisRuleResponse.builder()
            .analysisRule(testArgs.sdkResponseList[1].analysisRuleFromApi)
            .build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableAnalysisRuleRequest::class),
                any<Function<GetConfiguredTableAnalysisRuleRequest, GetConfiguredTableAnalysisRuleResponse>>()
            )
        } returns firstGetConfiguredTableAnalysisRuleResponse andThen secondGetConfiguredTableAnalysisRuleResponse

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
                assertThat(configuredTableIdentifier).isEqualTo(TEST_CONFIGURED_TABLE_ID)
                assertThat(arn).isEqualTo(TEST_CONFIGURED_TABLE_ARN)
            }
        }
    }

    @Test
    fun `UpdateHandler returns FAILED with NotFound if verify resource exists check fails`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableRequest::class),
                any<Function<GetConfiguredTableRequest, GetConfiguredTableResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_CONFIGURED_TABLE_ID)
            .build()

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_RESOURCE_MODEL)
            .previousResourceState(TEST_PREVIOUS_NAME_RESOURCE_MODEL)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
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
    fun `UpdateHandler returns FAILED with NotFound if configured table is not found during update configured table`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(UpdateConfiguredTableRequest::class),
                any<Function<UpdateConfiguredTableRequest, UpdateConfiguredTableResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_CONFIGURED_TABLE_ID)
            .build()

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_NULL_DESC_RESOURCE_MODEL)
            .previousResourceState(TEST_RESOURCE_MODEL)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .build()

        val response = handler.handleRequest(mockProxy, request, null, logger)
        assertThat(response).isNotNull
        with(response) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `UpdateHandler returns FAILED with NotFound if configured table is not found during delete analysis rule`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(DeleteConfiguredTableAnalysisRuleRequest::class),
                any<Function<DeleteConfiguredTableAnalysisRuleRequest, DeleteConfiguredTableAnalysisRuleResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_CONFIGURED_TABLE_ID)
            .build()

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_RESOURCE_MODEL_WITH_REQUIRED_AGG_ANALYSIS_RULE)
            .previousResourceState(TEST_RESOURCE_MODEL)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .build()

        val response = handler.handleRequest(mockProxy, request, null, logger)
        assertThat(response).isNotNull
        with(response) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `UpdateHandler returns FAILED with NotFound if configured table is not found during create analysis rule`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(CreateConfiguredTableAnalysisRuleRequest::class),
                any<Function<CreateConfiguredTableAnalysisRuleRequest, CreateConfiguredTableAnalysisRuleResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_CONFIGURED_TABLE_ID)
            .build()

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_RESOURCE_MODEL_WITH_REQUIRED_AGG_ANALYSIS_RULE)
            .previousResourceState(TEST_RESOURCE_MODEL)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .build()

        val response = handler.handleRequest(mockProxy, request, null, logger)
        assertThat(response).isNotNull
        with(response) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `UpdateHandler returns FAILED with NotFound with no model if configured table is not found and ReadHandler fails`() {

        val firstGetConfiguredTableResponse = GetConfiguredTableResponse.builder()
            .configuredTable(TEST_CONFIGURED_TABLE)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableRequest::class),
                any<Function<GetConfiguredTableRequest, GetConfiguredTableResponse>>()
            )
        } returns firstGetConfiguredTableResponse andThenThrows ResourceNotFoundException.builder()
            .resourceId(TEST_CONFIGURED_TABLE_ID)
            .build()

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_RESOURCE_MODEL)
            .previousResourceState(TEST_PREVIOUS_NAME_RESOURCE_MODEL)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
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
