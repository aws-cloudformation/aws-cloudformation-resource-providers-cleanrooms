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
import software.amazon.awssdk.services.cleanrooms.model.CreateConfiguredTableRequest
import software.amazon.awssdk.services.cleanrooms.model.CreateConfiguredTableResponse
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableAnalysisRuleRequest
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableAnalysisRuleResponse
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableRequest
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableResponse
import software.amazon.awssdk.services.cleanrooms.model.ListConfiguredTablesRequest
import software.amazon.awssdk.services.cleanrooms.model.ListConfiguredTablesResponse
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.ResourceNotFoundException
import software.amazon.awssdk.services.cleanrooms.model.ValidationException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.HandlerErrorCode
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import java.util.function.Function

class CreateHandlerTest {

    /**
     * Type to define test arguments for handler tests.
     */
    data class Args(
        val testName: String,
        val requestInputModel: ResourceModel,
        val configuredTableFromApi: ConfiguredTable,
        val analysisRuleFromApi: ConfiguredTableAnalysisRule?,
        val tagsFromApi: Map<String, String>,
        val expectedResourceModel: ResourceModel
    )

    companion object {
        @JvmStatic
        fun createHandlerSuccessTestData() = listOf(
            Args(
                testName = "Configured Table with required fields creates successfully.",
                requestInputModel = TEST_CREATE_RESOURCE_MODEL_REQUIRED_FIELDS,
                configuredTableFromApi = TEST_CONFIGURED_TABLE_REQUIRED_FIELDS,
                analysisRuleFromApi = null,
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS
            ),
            Args(
                testName = "Configured Table with required Agg analysis rule fields creates successfully.",
                requestInputModel = TEST_CREATE_RESOURCE_MODEL_WITH_REQUIRED_AGG_ANALYSIS_RULE,
                configuredTableFromApi = TEST_CONFIGURED_TABLE_WITH_AGG_ANALYSIS_RULE,
                analysisRuleFromApi = TEST_AGG_ANALYSIS_RULE_REQUIRED_FIELDS,
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_RESOURCE_MODEL_WITH_REQUIRED_AGG_ANALYSIS_RULE
            ),
            Args(
                testName = "Configured Table with required fields and List analysis rule creates successfully.",
                requestInputModel = TEST_CREATE_RESOURCE_MODEL_WITH_REQUIRED_LIST_ANALYSIS_RULE,
                configuredTableFromApi = TEST_CONFIGURED_TABLE_WITH_LIST_ANALYSIS_RULE,
                analysisRuleFromApi = TEST_LIST_ANALYSIS_RULE,
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_RESOURCE_MODEL_WITH_REQUIRED_LIST_ANALYSIS_RULE
            ),
            Args(
                testName = "Configured Table with required fields and Custom analysis rule with allowed analyses creates successfully.",
                requestInputModel = TEST_CREATE_RESOURCE_MODEL_WITH_REQUIRED_CUSTOM_ANALYSIS_RULE_ANALYSES,
                configuredTableFromApi = TEST_CONFIGURED_TABLE_WITH_CUSTOM_ANALYSIS_RULE,
                analysisRuleFromApi = TEST_CUSTOM_ANALYSIS_RULE_ANALYSES,
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_RESOURCE_MODEL_WITH_REQUIRED_CUSTOM_ANALYSIS_RULE_ANALYSES
            ),
            Args(
                testName = "Configured Table with required fields and Custom analysis rule with allowed providers creates successfully.",
                requestInputModel = TEST_CREATE_RESOURCE_MODEL_WITH_REQUIRED_CUSTOM_ANALYSIS_RULE_PROVIDERS,
                configuredTableFromApi = TEST_CONFIGURED_TABLE_WITH_CUSTOM_ANALYSIS_RULE,
                analysisRuleFromApi = TEST_CUSTOM_ANALYSIS_RULE_PROVIDERS,
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_RESOURCE_MODEL_WITH_REQUIRED_CUSTOM_ANALYSIS_RULE_PROVIDERS
            ),
            Args(
                testName = "Configured Table with only OR field set in List analysis rule creates successfully.",
                requestInputModel = TEST_CREATE_RESOURCE_MODEL_WITH_OR_JOIN_OPERATOR_LIST_ANALYSIS_RULE,
                configuredTableFromApi = TEST_CONFIGURED_TABLE_WITH_LIST_ANALYSIS_RULE,
                analysisRuleFromApi = TEST_ONLY_OR_LIST_ANALYSIS_RULE,
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_RESOURCE_MODEL_OR_JOIN_OPERATOR_REQUIRED_LIST_ANALYSIS_RULE
            ),
            Args(
                testName = "Configured Table with required fields and tags reads successfully.",
                requestInputModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS,
                configuredTableFromApi = TEST_CONFIGURED_TABLE_REQUIRED_FIELDS,
                analysisRuleFromApi = null,
                tagsFromApi = TEST_CONFIGURED_TABLE_TAGS + TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS
            ),
            Args(
                testName = "Configured Table all fields create successfully.",
                requestInputModel = TEST_RESOURCE_MODEL_WITH_AGG_ANALYSIS_RULE_AND_TAGS,
                configuredTableFromApi = TEST_CONFIGURED_TABLE_WITH_AGG_ANALYSIS_RULE,
                analysisRuleFromApi = TEST_AGG_ANALYSIS_RULE,
                tagsFromApi = TEST_CONFIGURED_TABLE_TAGS + TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_RESOURCE_MODEL_WITH_AGG_ANALYSIS_RULE_AND_TAGS
            ),
        )
    }

    private val mockCleanRoomsClient: CleanRoomsClient = mockk()

    private val mockProxy: AmazonWebServicesClientProxy = mockk()

    private val logger: Logger = LoggerProxy()

    private val handler: CreateHandler = CreateHandler()

    @BeforeEach
    fun setup() {
        mockkObject(ClientBuilder)
        every { ClientBuilder.getCleanRoomsClient() } returns mockCleanRoomsClient
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListTagsForResourceRequest::class),
                any<Function<ListTagsForResourceRequest, ListTagsForResourceResponse>>()
            )
        } returns ListTagsForResourceResponse.builder().tags(TEST_SYSTEM_TAGS).build()
    }

    @ParameterizedTest
    @MethodSource("createHandlerSuccessTestData")
    fun `Test Complete Resource Creation and stabilization returns successful`(testArgs: Args) {

        with(testArgs) {
            // Setup mock behavior.
            every {
                mockProxy.injectCredentialsAndInvokeV2(
                    ofType(CreateConfiguredTableRequest::class),
                    any<Function<CreateConfiguredTableRequest, CreateConfiguredTableResponse>>()
                )
            } returns CreateConfiguredTableResponse.builder()
                .configuredTable(configuredTableFromApi)
                .build()
            every {
                mockProxy.injectCredentialsAndInvokeV2(
                    ofType(CreateConfiguredTableAnalysisRuleRequest::class),
                    any<Function<CreateConfiguredTableAnalysisRuleRequest, CreateConfiguredTableAnalysisRuleResponse>>()
                )
            } returns CreateConfiguredTableAnalysisRuleResponse.builder()
                .analysisRule(analysisRuleFromApi)
                .build()
            every {
                mockProxy.injectCredentialsAndInvokeV2(
                    ofType(GetConfiguredTableRequest::class),
                    any<Function<GetConfiguredTableRequest, GetConfiguredTableResponse>>()
                )
            } returns GetConfiguredTableResponse.builder()
                .configuredTable(configuredTableFromApi)
                .build()
            every {
                mockProxy.injectCredentialsAndInvokeV2(
                    ofType(GetConfiguredTableAnalysisRuleRequest::class),
                    any<Function<GetConfiguredTableAnalysisRuleRequest, GetConfiguredTableAnalysisRuleResponse>>()
                )
            } returns GetConfiguredTableAnalysisRuleResponse.builder()
                .analysisRule(analysisRuleFromApi)
                .build()
            every {
                mockProxy.injectCredentialsAndInvokeV2(
                    ofType(ListConfiguredTablesRequest::class),
                    any<Function<ListConfiguredTablesRequest, ListConfiguredTablesResponse>>()
                )
            } returns ListConfiguredTablesResponse.builder()
                .configuredTableSummaries(listOf(TEST_CONFIGURED_TABLE_SUMMARY_1))
                .build()
            every {
                mockProxy.injectCredentialsAndInvokeV2(
                    ofType(ListTagsForResourceRequest::class),
                    any<Function<ListTagsForResourceRequest, ListTagsForResourceResponse>>()
                )
            } returns ListTagsForResourceResponse.builder().tags(tagsFromApi).build()
            val request = ResourceHandlerRequest.builder<ResourceModel>()
                .desiredResourceState(requestInputModel)
                .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
                .systemTags(TEST_SYSTEM_TAGS)
                .build()

            val resultAfterCreation = handler.handleRequest(mockProxy, request, null, logger)

            assertThat(resultAfterCreation).isNotNull
            with(resultAfterCreation) {
                assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
                with(resourceModel) {
                    assertThat(this).isEqualTo(expectedResourceModel)
                    assertThat(this.getResourcePrimaryIdentifier()).isEqualTo(TEST_CONFIGURED_TABLE_ID)
                    assertThat(arn).isEqualTo(TEST_CONFIGURED_TABLE_ARN)
                }
                assertThat(callbackContext).isNotNull
                with(callbackContext) {
                    assertThat(this?.stabilizationRetriesRemaining).isEqualTo(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES)
                    assertThat(this?.pendingStabilization)
                }
            }

            // Now we will make another request to pass into the stabilizers.
            val requestForStabilization = ResourceHandlerRequest.builder<ResourceModel>()
                .desiredResourceState(resultAfterCreation.resourceModel)
                .desiredResourceTags(request.desiredResourceTags)
                .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
                .build()

            val result = handler.handleRequest(mockProxy, requestForStabilization, resultAfterCreation.callbackContext, logger)

            assertThat(result).isNotNull
            with(result) {
                assertThat(status).isEqualTo(OperationStatus.SUCCESS)
                with(resourceModel) {
                    assertThat(this).isEqualTo(expectedResourceModel)
                    assertThat(this.getResourcePrimaryIdentifier()).isEqualTo(TEST_CONFIGURED_TABLE_ID)
                    assertThat(arn).isEqualTo(TEST_CONFIGURED_TABLE_ARN)
                }
                assertThat(callbackContext).isNull()
            }
        }
    }

    @Test
    fun `CreateHandler returns IN_PROGRESS if failed returned from ReadHandler during stabilization`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableRequest::class),
                any<Function<GetConfiguredTableRequest, GetConfiguredTableResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_CONFIGURED_TABLE_ID)
            .build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListConfiguredTablesRequest::class),
                any<Function<ListConfiguredTablesRequest, ListConfiguredTablesResponse>>()
            )
        } returns ListConfiguredTablesResponse.builder()
            .configuredTableSummaries(listOf(TEST_CONFIGURED_TABLE_SUMMARY_1))
            .build()

        val inputCallbackContext = CallbackContext(stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
            pendingStabilization = true)

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_RESOURCE_MODEL_REQUIRED_FIELDS)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .systemTags(TEST_SYSTEM_TAGS)
            .build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, request, inputCallbackContext, logger)

        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            with(resourceModel) {
                assertThat(this).isEqualTo(TEST_RESOURCE_MODEL_REQUIRED_FIELDS)
                assertThat(this.getResourcePrimaryIdentifier()).isEqualTo(TEST_CONFIGURED_TABLE_ID)
                assertThat(arn).isEqualTo(TEST_CONFIGURED_TABLE_ARN)
            }
            assertThat(callbackContext).isNotNull
            with(callbackContext) {
                assertThat(this?.stabilizationRetriesRemaining).isEqualTo(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES - 1)
                assertThat(this?.pendingStabilization)
            }
        }
    }

    @Test
    fun `CreateHandler returns IN_PROGRESS if resource identifier is not returned in ListHandler during stabilization`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableRequest::class),
                any<Function<GetConfiguredTableRequest, GetConfiguredTableResponse>>()
            )
        } returns GetConfiguredTableResponse.builder()
            .configuredTable(TEST_CONFIGURED_TABLE_REQUIRED_FIELDS)
            .build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListConfiguredTablesRequest::class),
                any<Function<ListConfiguredTablesRequest, ListConfiguredTablesResponse>>()
            )
        } returns ListConfiguredTablesResponse.builder()
            .configuredTableSummaries(listOf(TEST_CONFIGURED_TABLE_SUMMARY_2))
            .build()

        val inputCallbackContext = CallbackContext(stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
            pendingStabilization = true)

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_RESOURCE_MODEL_REQUIRED_FIELDS)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .systemTags(TEST_SYSTEM_TAGS)
            .build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, request, inputCallbackContext, logger)

        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            with(resourceModel) {
                assertThat(this).isEqualTo(TEST_RESOURCE_MODEL_REQUIRED_FIELDS)
                assertThat(this.getResourcePrimaryIdentifier()).isEqualTo(TEST_CONFIGURED_TABLE_ID)
                assertThat(arn).isEqualTo(TEST_CONFIGURED_TABLE_ARN)
            }
            assertThat(callbackContext).isNotNull
            with(callbackContext) {
                assertThat(this?.stabilizationRetriesRemaining).isEqualTo(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES - 1)
                assertThat(this?.pendingStabilization)
            }
        }
    }

    @Test
    fun `CreateHandler returns IN_PROGRESS if exception thrown from ListHandler during stabilization`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableRequest::class),
                any<Function<GetConfiguredTableRequest, GetConfiguredTableResponse>>()
            )
        } returns GetConfiguredTableResponse.builder()
            .configuredTable(TEST_CONFIGURED_TABLE_REQUIRED_FIELDS)
            .build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListConfiguredTablesRequest::class),
                any<Function<ListConfiguredTablesRequest, ListConfiguredTablesResponse>>()
            )
        } throws ValidationException.builder().build()

        val inputCallbackContext = CallbackContext(stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
            pendingStabilization = true)

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_RESOURCE_MODEL_REQUIRED_FIELDS)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .systemTags(TEST_SYSTEM_TAGS)
            .build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, request, inputCallbackContext, logger)

        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            with(resourceModel) {
                assertThat(this).isEqualTo(TEST_RESOURCE_MODEL_REQUIRED_FIELDS)
                assertThat(this.getResourcePrimaryIdentifier()).isEqualTo(TEST_CONFIGURED_TABLE_ID)
                assertThat(arn).isEqualTo(TEST_CONFIGURED_TABLE_ARN)
            }
            assertThat(callbackContext).isNotNull
            with(callbackContext) {
                assertThat(this?.stabilizationRetriesRemaining).isEqualTo(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES - 1)
                assertThat(this?.pendingStabilization)
            }
        }
    }

    @Test
    fun `CreateHandler returns FAILED with NotFound if configured table is not found during create configured table analysis rule`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(CreateConfiguredTableRequest::class),
                any<Function<CreateConfiguredTableRequest, CreateConfiguredTableResponse>>()
            )
        } returns CreateConfiguredTableResponse.builder()
            .configuredTable(TEST_CONFIGURED_TABLE_WITH_AGG_ANALYSIS_RULE)
            .build()

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
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .systemTags(TEST_SYSTEM_TAGS)
            .build()

        val result = handler.handleRequest(mockProxy, request, null, logger)

        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
            with(resourceModel) {
                assertThat(this.getResourcePrimaryIdentifier()).isEqualTo(TEST_CONFIGURED_TABLE_ID)
                assertThat(arn).isEqualTo(TEST_CONFIGURED_TABLE_ARN)
            }
        }
    }

    @Test
    fun `CreateHandler returns FAILED with NOT STABILIZED if resource is not stabilized within retry poll count`() {
        val callbackContext = CallbackContext(stabilizationRetriesRemaining = 0, pendingStabilization = true)

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_RESOURCE_MODEL_WITH_REQUIRED_AGG_ANALYSIS_RULE)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .build()

        val result = handler.handleRequest(mockProxy, request, callbackContext, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotStabilized)
        }
    }
}
