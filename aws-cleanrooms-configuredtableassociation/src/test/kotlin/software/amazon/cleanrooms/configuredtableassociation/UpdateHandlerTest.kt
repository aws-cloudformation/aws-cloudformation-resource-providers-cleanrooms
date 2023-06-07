package software.amazon.cleanrooms.configuredtableassociation

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAssociation
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableAssociationRequest
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableAssociationResponse
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.ResourceNotFoundException
import software.amazon.awssdk.services.cleanrooms.model.TagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.TagResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.UntagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.UntagResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.UpdateConfiguredTableAssociationRequest
import software.amazon.awssdk.services.cleanrooms.model.UpdateConfiguredTableAssociationResponse
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_CONFIGURED_TABLE_ASSOCIATION_REQUIRED_FIELDS
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_CONFIGURED_TABLE_ASSOCIATION_TAGS
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_CONFIGURED_TABLE_ASSOCIATION_UPDATED_ROLE_ARN
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_CONFIGURED_TABLE_ASSOCIATION_WITH_DESCRIPTION_FIELD
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_CREATOR_ACCOUNT_ID
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_CTA_ARN
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_CTA_ID
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_RESOURCE_MODEL_REQUIRED_FIELDS
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_RESOURCE_MODEL_WITH_DESCRIPTION
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_SYSTEM_TAGS
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_UPDATED_CONFIGURED_TABLE_ASSOCIATION_TAGS
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_UPDATE_DESCRIPTION_RESOURCE_MODEL
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_UPDATE_ROLE_ARN_RESOURCE_MODEL
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
        val configuredTableAssociationFromApi: ConfiguredTableAssociation,
        val tagsFromApi: Map<String, String>,
    )
    companion object {
        @JvmStatic
        fun updateHandlerSuccessTestData() = listOf(
            Args(
                testName = "ConfiguredTableAssociation RoleArn is updated successfully.",
                requestDesiredResourceModel = TEST_UPDATE_ROLE_ARN_RESOURCE_MODEL,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        configuredTableAssociationFromApi = TEST_CONFIGURED_TABLE_ASSOCIATION_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        configuredTableAssociationFromApi = TEST_CONFIGURED_TABLE_ASSOCIATION_UPDATED_ROLE_ARN,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_UPDATE_ROLE_ARN_RESOURCE_MODEL
            ),
            Args(
                testName = "ConfiguredTableAssociation Description is updated successfully.",
                requestDesiredResourceModel = TEST_UPDATE_DESCRIPTION_RESOURCE_MODEL,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        configuredTableAssociationFromApi = TEST_CONFIGURED_TABLE_ASSOCIATION_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        configuredTableAssociationFromApi = TEST_CONFIGURED_TABLE_ASSOCIATION_WITH_DESCRIPTION_FIELD,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_UPDATE_DESCRIPTION_RESOURCE_MODEL
            ),
            Args(
                testName = "ConfiguredTableAssociation tags are removed successfully.",
                requestDesiredResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = TEST_CONFIGURED_TABLE_ASSOCIATION_TAGS,
                sdkResponseList = listOf(
                    SdkResponse(
                        configuredTableAssociationFromApi = TEST_CONFIGURED_TABLE_ASSOCIATION_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        configuredTableAssociationFromApi = TEST_CONFIGURED_TABLE_ASSOCIATION_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS
            ),
            Args(
                testName = "ConfiguredTableAssociation when new tag keys are added, tags are added successfully.",
                requestDesiredResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS,
                requestDesiredResourceTags = TEST_CONFIGURED_TABLE_ASSOCIATION_TAGS,
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        configuredTableAssociationFromApi = TEST_CONFIGURED_TABLE_ASSOCIATION_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        configuredTableAssociationFromApi = TEST_CONFIGURED_TABLE_ASSOCIATION_REQUIRED_FIELDS,
                        tagsFromApi = TEST_CONFIGURED_TABLE_ASSOCIATION_TAGS + TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS
            ),
            Args(
                testName = "ConfiguredTableAssociation when tag values are modified, tags are updated successfully.",
                requestDesiredResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS,
                requestDesiredResourceTags = TEST_CONFIGURED_TABLE_ASSOCIATION_TAGS,
                requestPreviousResourceTags = TEST_UPDATED_CONFIGURED_TABLE_ASSOCIATION_TAGS,
                sdkResponseList = listOf(
                    SdkResponse(
                        configuredTableAssociationFromApi = TEST_CONFIGURED_TABLE_ASSOCIATION_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        configuredTableAssociationFromApi = TEST_CONFIGURED_TABLE_ASSOCIATION_REQUIRED_FIELDS,
                        tagsFromApi = TEST_CONFIGURED_TABLE_ASSOCIATION_TAGS + TEST_SYSTEM_TAGS,
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
                ofType(UpdateConfiguredTableAssociationRequest::class),
                any<Function<UpdateConfiguredTableAssociationRequest, UpdateConfiguredTableAssociationResponse>>()
            )
        } returns UpdateConfiguredTableAssociationResponse.builder().build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableAssociationRequest::class),
                any<Function<GetConfiguredTableAssociationRequest, GetConfiguredTableAssociationResponse>>()
            )
        } returns GetConfiguredTableAssociationResponse.builder()
            .configuredTableAssociation(TEST_CONFIGURED_TABLE_ASSOCIATION_REQUIRED_FIELDS)
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

        val firstGetConfiguredTableAssociationResponse = GetConfiguredTableAssociationResponse.builder()
            .configuredTableAssociation(testArgs.sdkResponseList[0].configuredTableAssociationFromApi)
            .build()
        val secondGetConfiguredTableAssociationResponse = GetConfiguredTableAssociationResponse.builder()
            .configuredTableAssociation(testArgs.sdkResponseList[1].configuredTableAssociationFromApi)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableAssociationRequest::class),
                any<Function<GetConfiguredTableAssociationRequest, GetConfiguredTableAssociationResponse>>()
            )
        } returns firstGetConfiguredTableAssociationResponse andThen secondGetConfiguredTableAssociationResponse

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
                assertThat(configuredTableAssociationIdentifier).isEqualTo(TEST_CTA_ID)
                assertThat(arn).isEqualTo(TEST_CTA_ARN)
            }
        }
    }

    @Test
    fun `UpdateHandler returns FAILED with NotFound if verify resource exists check fails`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableAssociationRequest::class),
                any<Function<GetConfiguredTableAssociationRequest, GetConfiguredTableAssociationResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_CTA_ID)
            .build()

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_RESOURCE_MODEL_REQUIRED_FIELDS)
            .previousResourceState(TEST_RESOURCE_MODEL_WITH_DESCRIPTION)
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
                ofType(UpdateConfiguredTableAssociationRequest::class),
                any<Function<UpdateConfiguredTableAssociationRequest, UpdateConfiguredTableAssociationResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_CTA_ID)
            .build()

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_RESOURCE_MODEL_REQUIRED_FIELDS)
            .previousResourceState(TEST_RESOURCE_MODEL_WITH_DESCRIPTION)
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

        val firstGetConfiguredTableAssociationResponse = GetConfiguredTableAssociationResponse.builder()
            .configuredTableAssociation(TEST_CONFIGURED_TABLE_ASSOCIATION_REQUIRED_FIELDS)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableAssociationRequest::class),
                any<Function<GetConfiguredTableAssociationRequest, GetConfiguredTableAssociationResponse>>()
            )
        } returns firstGetConfiguredTableAssociationResponse andThenThrows ResourceNotFoundException.builder()
            .resourceId(TEST_CTA_ID)
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
