package software.amazon.cleanrooms.membership

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.awssdk.services.cleanrooms.model.GetMembershipRequest
import software.amazon.awssdk.services.cleanrooms.model.GetMembershipResponse
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.Membership
import software.amazon.awssdk.services.cleanrooms.model.ResourceNotFoundException
import software.amazon.awssdk.services.cleanrooms.model.TagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.TagResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.UntagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.UntagResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.UpdateMembershipRequest
import software.amazon.awssdk.services.cleanrooms.model.UpdateMembershipResponse
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
        val membershipFromApi: Membership,
        val tagsFromApi: Map<String, String>,
    )
    companion object {
        @JvmStatic
        fun updateHandlerSuccessTestData() = listOf(
            Args(
                testName = "Membership QueryLogStatus is updated successfully.",
                requestDesiredResourceModel = TEST_QUERYLOG_DISABLED_RESOURCE_MODEL,
                requestPreviousResourceModel = TEST_QUERYLOG_ENABLED_RESOURCE_MODEL,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        membershipFromApi = TEST_MEMBERSHIP_RESPONSE_WITH_QUERYLOG_ENABLED,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        membershipFromApi = TEST_MEMBERSHIP_RESPONSE_WITH_QUERYLOG_DISABLED,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_QUERYLOG_DISABLED_RESOURCE_MODEL
            ),
            Args(
                testName = "Membership tags are removed successfully.",
                requestDesiredResourceModel = TEST_RESOURCE_MODEL_WITHOUT_TAGS,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL_WITH_TAGS,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = TEST_MEMBERSHIP_TAGS,
                sdkResponseList = listOf(
                    SdkResponse(
                        membershipFromApi = TEST_MEMBERSHIP_RESPONSE_WITH_QUERYLOG_ENABLED,
                        tagsFromApi = TEST_MEMBERSHIP_TAGS + TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        membershipFromApi = TEST_MEMBERSHIP_RESPONSE_WITH_QUERYLOG_ENABLED,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_RESOURCE_MODEL_WITHOUT_TAGS
            ),
            Args(
                testName = "Membership when new tag keys are added, tags are added successfully.",
                requestDesiredResourceModel = TEST_RESOURCE_MODEL_WITH_TAGS,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL_WITHOUT_TAGS,
                requestDesiredResourceTags = TEST_MEMBERSHIP_TAGS,
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        membershipFromApi = TEST_MEMBERSHIP_RESPONSE_WITH_QUERYLOG_ENABLED,
                        tagsFromApi = TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        membershipFromApi = TEST_MEMBERSHIP_RESPONSE_WITH_QUERYLOG_ENABLED,
                        tagsFromApi = TEST_MEMBERSHIP_TAGS + TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_RESOURCE_MODEL_WITH_TAGS
            ),
            Args(
                testName = "Membership when tag values are modified, tags are updated successfully.",
                requestDesiredResourceModel = TEST_RESOURCE_MODEL_WITH_TAGS_AFTER_UPDATE,
                requestPreviousResourceModel = TEST_RESOURCE_MODEL_WITH_TAGS_BEFORE_UPDATE,
                requestDesiredResourceTags = TEST_MEMBERSHIP_TAGS_AFTER_UPDATE,
                requestPreviousResourceTags = TEST_MEMBERSHIP_TAGS_BEFORE_UPDATE,
                sdkResponseList = listOf(
                    SdkResponse(
                        membershipFromApi = TEST_MEMBERSHIP_RESPONSE_WITH_QUERYLOG_ENABLED,
                        tagsFromApi = TEST_MEMBERSHIP_TAGS_BEFORE_UPDATE + TEST_SYSTEM_TAGS,
                    ),
                    SdkResponse(
                        membershipFromApi = TEST_MEMBERSHIP_RESPONSE_WITH_QUERYLOG_ENABLED,
                        tagsFromApi = TEST_MEMBERSHIP_TAGS_AFTER_UPDATE + TEST_SYSTEM_TAGS,
                    ),
                ),
                expectedResourceModel = TEST_RESOURCE_MODEL_WITH_TAGS_AFTER_UPDATE
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
                ofType(UpdateMembershipRequest::class),
                any<Function<UpdateMembershipRequest, UpdateMembershipResponse>>()
            )
        } returns UpdateMembershipResponse.builder().membership(TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS).build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetMembershipRequest::class),
                any<Function<GetMembershipRequest, GetMembershipResponse>>()
            )
        } returns GetMembershipResponse.builder()
            .membership(TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS)
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

        val firstGetMembershipResponse = GetMembershipResponse.builder()
            .membership(testArgs.sdkResponseList[0].membershipFromApi)
            .build()
        val secondGetMembershipResponse = GetMembershipResponse.builder()
            .membership(testArgs.sdkResponseList[1].membershipFromApi)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetMembershipRequest::class),
                any<Function<GetMembershipRequest, GetMembershipResponse>>()
            )
        } returns firstGetMembershipResponse andThen secondGetMembershipResponse

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
                assertThat(membershipIdentifier).isEqualTo(TEST_MEMBERSHIP_ID)
                assertThat(arn).isEqualTo(TEST_MEMBERSHIP_ARN)
            }
        }
    }

    @Test
    fun `UpdateHandler returns SUCCESS with NO UpdateMembership called when only stack level tags changed`() {
        verify(exactly = 0) {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(UpdateMembershipRequest::class),
                any<Function<UpdateMembershipRequest, UpdateMembershipResponse>>()
            )
        }

        val firstListTagsForResourceResponse = ListTagsForResourceResponse.builder()
            .tags(TEST_SYSTEM_TAGS)
            .build()
        val secondListTagsForResourceResponse = ListTagsForResourceResponse.builder()
            .tags(TEST_MEMBERSHIP_TAGS + TEST_SYSTEM_TAGS)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListTagsForResourceRequest::class),
                any<Function<ListTagsForResourceRequest, ListTagsForResourceResponse>>()
            )
        } returns firstListTagsForResourceResponse andThen secondListTagsForResourceResponse

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_QUERYLOG_ENABLED_RESOURCE_MODEL)
            .previousResourceState(TEST_QUERYLOG_ENABLED_RESOURCE_MODEL)
            .previousResourceTags(emptyMap())
            .desiredResourceTags(TEST_MEMBERSHIP_TAGS)
            .build()

        val response = handler.handleRequest(mockProxy, request, null, logger)
        assertThat(response).isNotNull
        with(response) {
            assertThat(status).isEqualTo(OperationStatus.SUCCESS)
            with(resourceModel) {
                assertThat(this).isEqualTo(TEST_RESOURCE_MODEL_WITH_TAGS)
                assertThat(membershipIdentifier).isEqualTo(TEST_MEMBERSHIP_ID)
                assertThat(arn).isEqualTo(TEST_MEMBERSHIP_ARN)
            }
        }
    }

    @Test
    fun `UpdateHandler returns FAILED with NotFound if membership is not found during update membership`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(UpdateMembershipRequest::class),
                any<Function<UpdateMembershipRequest, UpdateMembershipResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_MEMBERSHIP_ID)
            .build()

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_QUERYLOG_DISABLED_RESOURCE_MODEL)
            .previousResourceState(TEST_QUERYLOG_ENABLED_RESOURCE_MODEL)
            .build()

        val response = handler.handleRequest(mockProxy, request, null, logger)
        assertThat(response).isNotNull
        with(response) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `UpdateHandler returns FAILED with NotFound if Read after update fails`() {

        val firstGetMembershipResponse = GetMembershipResponse.builder()
            .membership(TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetMembershipRequest::class),
                any<Function<GetMembershipRequest, GetMembershipResponse>>()
            )
        } returns firstGetMembershipResponse andThenThrows ResourceNotFoundException.builder()
            .resourceId(TEST_MEMBERSHIP_ID)
            .build()

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_QUERYLOG_DISABLED_RESOURCE_MODEL)
            .previousResourceState(TEST_QUERYLOG_ENABLED_RESOURCE_MODEL)
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
    fun `UpdateHandler returns FAILED with NotFound if Read before update fails`() {

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetMembershipRequest::class),
                any<Function<GetMembershipRequest, GetMembershipResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_MEMBERSHIP_ID)
            .build()

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_QUERYLOG_DISABLED_RESOURCE_MODEL)
            .previousResourceState(TEST_QUERYLOG_ENABLED_RESOURCE_MODEL)
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
    fun `UpdateHandler returns FAILED without ResourceModel when update fails and subsequent Read fails`() {

        val firstGetMembershipResponse = GetMembershipResponse.builder()
            .membership(TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetMembershipRequest::class),
                any<Function<GetMembershipRequest, GetMembershipResponse>>()
            )
        } returns firstGetMembershipResponse andThenThrows ResourceNotFoundException.builder()
            .resourceId(TEST_MEMBERSHIP_ID)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(UpdateMembershipRequest::class),
                any<Function<UpdateMembershipRequest, UpdateMembershipResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_MEMBERSHIP_ID)
            .build()

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_QUERYLOG_DISABLED_RESOURCE_MODEL)
            .previousResourceState(TEST_QUERYLOG_ENABLED_RESOURCE_MODEL)
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
    fun `UpdateHandler returns FAILED with ResourceModel when update fails and subsequent read succceds`() {

        val getMembershipResponse = GetMembershipResponse.builder()
            .membership(TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetMembershipRequest::class),
                any<Function<GetMembershipRequest, GetMembershipResponse>>()
            )
        } returns getMembershipResponse

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(UpdateMembershipRequest::class),
                any<Function<UpdateMembershipRequest, UpdateMembershipResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_MEMBERSHIP_ID)
            .build()

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_QUERYLOG_DISABLED_RESOURCE_MODEL)
            .previousResourceState(TEST_QUERYLOG_ENABLED_RESOURCE_MODEL)
            .build()

        val response = handler.handleRequest(mockProxy, request, null, logger)
        assertThat(response).isNotNull
        with(response) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
            assertThat(resourceModel).isNotNull
            assertThat(resourceModel.membershipIdentifier).isEqualTo(TEST_MEMBERSHIP_ID)
        }
    }
}
