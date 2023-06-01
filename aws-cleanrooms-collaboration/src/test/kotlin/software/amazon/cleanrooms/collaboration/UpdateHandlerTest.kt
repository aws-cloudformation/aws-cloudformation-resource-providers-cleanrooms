package software.amazon.cleanrooms.collaboration

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
import software.amazon.awssdk.services.cleanrooms.model.Collaboration
import software.amazon.awssdk.services.cleanrooms.model.GetCollaborationRequest
import software.amazon.awssdk.services.cleanrooms.model.GetCollaborationResponse
import software.amazon.awssdk.services.cleanrooms.model.ListMembersRequest
import software.amazon.awssdk.services.cleanrooms.model.ListMembersResponse
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.ResourceNotFoundException
import software.amazon.awssdk.services.cleanrooms.model.TagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.TagResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.UntagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.UntagResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.UpdateCollaborationRequest
import software.amazon.awssdk.services.cleanrooms.model.UpdateCollaborationResponse
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
        val collaborationFromApi: Collaboration,
        val tagsFromApi: Map<String, String>
    )

    companion object {
        @JvmStatic
        fun updateHandlerSuccessTestData() = listOf(
            Args(
                testName = "Test UpdateHandler for collaboration with only name changed.",
                requestDesiredResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_WITH_NEW_NAME,
                requestPreviousResourceModel = TEST_BASE_COLLABORATION_RESOURCE_MODEL,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS
                    ),
                    SdkResponse(
                        collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS_AND_NEW_NAME,
                        tagsFromApi = TEST_SYSTEM_TAGS
                    )
                ),
                expectedResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_WITH_NEW_NAME
            ),
            Args(
                testName = "Test UpdateHandler for collaboration with only description changed.",
                requestDesiredResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_WITH_NEW_DESCRIPTION,
                requestPreviousResourceModel = TEST_BASE_COLLABORATION_RESOURCE_MODEL,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS
                    ),
                    SdkResponse(
                        collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS_AND_NEW_DESCRIPTION,
                        tagsFromApi = TEST_SYSTEM_TAGS
                    )
                ),
                expectedResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_WITH_NEW_DESCRIPTION
            ),
            Args(
                testName = "Test UpdateHandler for collaboration with both name and description changed.",
                requestDesiredResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_WITH_NEW_NAME_AND_DESCRIPTION,
                requestPreviousResourceModel = TEST_BASE_COLLABORATION_RESOURCE_MODEL,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS
                    ),
                    SdkResponse(
                        collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS_WITH_NEW_NAME_AND_DESCRIPTION,
                        tagsFromApi = TEST_SYSTEM_TAGS
                    )
                ),
                expectedResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_WITH_NEW_NAME_AND_DESCRIPTION
            ),
            Args(
                testName = "Test UpdateHandler for collaboration with name null does not change anything in previous model.",
                requestDesiredResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_WITH_NULL_NAME,
                requestPreviousResourceModel = TEST_BASE_COLLABORATION_RESOURCE_MODEL,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS
                    ),
                    SdkResponse(
                        collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS
                    )
                ),
                expectedResourceModel = TEST_BASE_COLLABORATION_RESOURCE_MODEL
            ),
            Args(
                testName = "Test UpdateHandler for collaboration with description null does not change anything in previous model.",
                requestDesiredResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_WITH_NULL_DESCRIPTION,
                requestPreviousResourceModel = TEST_BASE_COLLABORATION_RESOURCE_MODEL,
                requestDesiredResourceTags = emptyMap(),
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS
                    ),
                    SdkResponse(
                        collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS
                    )
                ),
                expectedResourceModel = TEST_BASE_COLLABORATION_RESOURCE_MODEL
            ),
            Args(
                testName = "Test UpdateHandler for collaboration with new tags updates tags correctly.",
                requestDesiredResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_WITH_NEW_TAGS,
                requestPreviousResourceModel = TEST_BASE_COLLABORATION_RESOURCE_MODEL,
                requestDesiredResourceTags = TEST_COLLABORATION_NEW_TAGS,
                requestPreviousResourceTags = emptyMap(),
                sdkResponseList = listOf(
                    SdkResponse(
                        collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                        tagsFromApi = TEST_SYSTEM_TAGS
                    ),
                    SdkResponse(
                        collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                        tagsFromApi = TEST_COLLABORATION_NEW_TAGS + TEST_SYSTEM_TAGS
                    )
                ),
                expectedResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_WITH_NEW_TAGS
            ),
            Args(
                testName = "Test UpdateHandler for collaboration with new tags adds new tags to already existing tags.",
                requestDesiredResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_WITH_OLD_AND_NEW_TAGS,
                requestPreviousResourceModel = TEST_BASE_COLLABORATION_RESOURCE_MODEL,
                requestDesiredResourceTags = TEST_COLLABORATION_NEW_TAGS,
                requestPreviousResourceTags = TEST_COLLABORATION_TAGS,
                sdkResponseList = listOf(
                    SdkResponse(
                        collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                        tagsFromApi = TEST_COLLABORATION_TAGS + TEST_SYSTEM_TAGS
                    ),
                    SdkResponse(
                        collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                        tagsFromApi = TEST_COLLABORATION_TAGS + TEST_COLLABORATION_NEW_TAGS + TEST_SYSTEM_TAGS
                    )
                ),
                expectedResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_WITH_OLD_AND_NEW_TAGS
            ),
            Args(
                testName = "Test UpdateHandler for collaboration with tags removes the tags from resource correctly.",
                requestDesiredResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_WITH_PREVIOUS_TAGS,
                requestPreviousResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_WITH_OLD_AND_NEW_TAGS,
                requestDesiredResourceTags = TEST_COLLABORATION_TAGS,
                requestPreviousResourceTags = TEST_COLLABORATION_TAGS + TEST_COLLABORATION_NEW_TAGS,
                sdkResponseList = listOf(
                    SdkResponse(
                        collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                        tagsFromApi = TEST_COLLABORATION_TAGS + TEST_COLLABORATION_NEW_TAGS + TEST_SYSTEM_TAGS
                    ),
                    SdkResponse(
                        collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                        tagsFromApi = TEST_COLLABORATION_TAGS + TEST_SYSTEM_TAGS
                    )
                ),
                expectedResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_WITH_PREVIOUS_TAGS
            )
        )
    }

    private val mockCleanRoomsClient: CleanRoomsClient = mockk()
    private val mockProxy: AmazonWebServicesClientProxy = mockk()
    private val logger: Logger = LoggerProxy()
    private val handler: UpdateHandler = UpdateHandler()
    private val listMembersResponse = ListMembersResponse.builder()
        .memberSummaries(listOf(TEST_CREATOR_MEMBER_SUMMARY))
        .build()

    private val getCollaborationResponse = GetCollaborationResponse.builder()
        .collaboration(TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS)
        .build()

    private val defaultUpdateRequest = ResourceHandlerRequest.builder<ResourceModel>()
        .previousResourceState(TEST_BASE_COLLABORATION_RESOURCE_MODEL)
        .desiredResourceState(TEST_COLLABORATION_RESOURCE_MODEL_WITH_NEW_NAME)
        .desiredResourceTags(emptyMap())
        .previousResourceTags(emptyMap())
        .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
        .build()

    @BeforeEach
    fun setup() {
        // this sets up the default behavior of mocks so that we don't have to repeat some mocking functionality.
        mockkObject(ClientBuilder)
        every { ClientBuilder.getCleanRoomsClient() } returns mockCleanRoomsClient

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(UpdateCollaborationRequest::class),
                any<Function<UpdateCollaborationRequest, UpdateCollaborationResponse>>()
            )
        } returns UpdateCollaborationResponse.builder().collaboration(TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS).build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } returns getCollaborationResponse

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListMembersRequest::class),
                any<Function<ListMembersRequest, ListMembersResponse>>()
            )
        } returns listMembersResponse

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
    fun `UpdateHandler returns SUCCESS with correct attributes and tags updated`(testArgs: Args) {
        with(testArgs) {
            // mocking getCollaborationResponse so that the name and description can be verified for update..
            val firstGetCollaborationResponse = GetCollaborationResponse.builder()
                .collaboration(sdkResponseList[0].collaborationFromApi)
                .build()

            val secondGetCollaborationResponse = GetCollaborationResponse.builder()
                .collaboration(sdkResponseList[1].collaborationFromApi)
                .build()

            every {
                mockProxy.injectCredentialsAndInvokeV2(
                    ofType(GetCollaborationRequest::class),
                    any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
                )
            } returns firstGetCollaborationResponse andThen secondGetCollaborationResponse

            // mocking listTagsForResponse so that the tags can be verified for update.
            // Note: we will use the default listMembers call mock since we are not updating members in UpdateHandler.
            val firstListTagsForResourceResponse = ListTagsForResourceResponse.builder()
                .tags(sdkResponseList[0].tagsFromApi)
                .build()
            val secondListTagsForResourceResponse = ListTagsForResourceResponse.builder()
                .tags(sdkResponseList[1].tagsFromApi)
                .build()

            every {
                mockProxy.injectCredentialsAndInvokeV2(
                    ofType(ListTagsForResourceRequest::class),
                    any<Function<ListTagsForResourceRequest, ListTagsForResourceResponse>>()
                )
            } returns firstListTagsForResourceResponse andThen secondListTagsForResourceResponse

            val request = ResourceHandlerRequest.builder<ResourceModel>()
                .desiredResourceState(requestDesiredResourceModel)
                .previousResourceState(requestPreviousResourceModel)
                .desiredResourceTags(requestDesiredResourceTags)
                .previousResourceTags(requestPreviousResourceTags)
                .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
                .build()

            val result = handler.handleRequest(mockProxy, request, null, logger)

            assertThat(result).isNotNull
            with(result) {
                assertThat(status).isEqualTo(OperationStatus.SUCCESS)
                with(resourceModel) {
                    assertThat(this).isEqualTo(expectedResourceModel)
                    assertThat(getCollaborationIdFromPrimaryIdentifier()).isEqualTo(TEST_COLLAB_ID)
                    assertThat(arn).isEqualTo(TEST_COLLAB_ARN)
                }
            }
        }
    }

    @Test
    fun `UpdateHandler returns SUCCESS with NO UpdateCollaboration called when only stack level tags changed`() {
        verify(exactly = 0) {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(UpdateCollaborationRequest::class),
                any<Function<UpdateCollaborationRequest, UpdateCollaborationResponse>>()
            )
        }

        val firstGetCollaborationResponse = GetCollaborationResponse.builder()
            .collaboration(TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS)
            .build()

        val secondGetCollaborationResponse = GetCollaborationResponse.builder()
            .collaboration(TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } returns firstGetCollaborationResponse andThen secondGetCollaborationResponse

        val firstListTagsForResourceResponse = ListTagsForResourceResponse.builder()
            .tags(TEST_SYSTEM_TAGS)
            .build()
        val secondListTagsForResourceResponse = ListTagsForResourceResponse.builder()
            .tags(TEST_COLLABORATION_NEW_TAGS + TEST_SYSTEM_TAGS)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListTagsForResourceRequest::class),
                any<Function<ListTagsForResourceRequest, ListTagsForResourceResponse>>()
            )
        } returns firstListTagsForResourceResponse andThen secondListTagsForResourceResponse

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_BASE_COLLABORATION_RESOURCE_MODEL)
            .previousResourceState(TEST_BASE_COLLABORATION_RESOURCE_MODEL)
            .previousResourceTags(emptyMap())
            .desiredResourceTags(TEST_COLLABORATION_NEW_TAGS)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .build()

        val response = handler.handleRequest(mockProxy, request, null, logger)
        assertThat(response).isNotNull
        with(response) {
            assertThat(status).isEqualTo(OperationStatus.SUCCESS)
            with(resourceModel) {
                assertThat(this).isEqualTo(TEST_COLLABORATION_RESOURCE_MODEL_WITH_NEW_TAGS)
                assertThat(collaborationIdentifier).isEqualTo(TEST_COLLAB_ID)
                assertThat(arn).isEqualTo(TEST_COLLAB_ARN)
            }
        }
    }

    @Test
    fun `UpdateHandler returns FAILED with NotFound if collaboration is not found during update collaboration`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(UpdateCollaborationRequest::class),
                any<Function<UpdateCollaborationRequest, UpdateCollaborationResponse>>()
            )
        } throws ResourceNotFoundException.builder().resourceId(TEST_COLLAB_ID).build()

        val result = handler.handleRequest(mockProxy, defaultUpdateRequest, null, logger)

        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `UpdateHandler returns FAILED with NotFound if Read after update fails`() {
        // here we want to fail the second read so overriding behavior of getCollaboration.
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } returns getCollaborationResponse andThenThrows ResourceNotFoundException.builder()
            .resourceId(TEST_COLLAB_ID)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(UpdateCollaborationRequest::class),
                any<Function<UpdateCollaborationRequest, UpdateCollaborationResponse>>()
            )
        } returns UpdateCollaborationResponse.builder()
            .collaboration(TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS)
            .build()

        val result = handler.handleRequest(mockProxy, defaultUpdateRequest, null, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
            assertThat(resourceModel).isNull()
        }
    }

    @Test
    fun `UpdateHandler returns FAILED with NotFound if Read before update fails`() {
        // we want to override the default behavior of GetCollaboration since we want first read to fail.
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_COLLAB_ID)
            .build()

        val result = handler.handleRequest(mockProxy, defaultUpdateRequest, null, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
            assertThat(resourceModel).isNull()
        }
    }

    @Test
    fun `UpdateHandler returns FAILED without ResourceModel when update fails and subsequent Read fails`() {
        // Here we want to fail the second read so we override the default behavior.
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } returns getCollaborationResponse andThenThrows ResourceNotFoundException.builder()
            .resourceId(TEST_COLLAB_ID)
            .build()

        // Also making the update fail.
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(UpdateCollaborationRequest::class),
                any<Function<UpdateCollaborationRequest, UpdateCollaborationResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_COLLAB_ID)
            .build()

        val result = handler.handleRequest(mockProxy, defaultUpdateRequest, null, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
            assertThat(resourceModel).isNull()
        }
    }

    @Test
    fun `UpdateHandler returns FAILED with ResourceModel when update fails and subsequent read succeeds`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(UpdateCollaborationRequest::class),
                any<Function<UpdateCollaborationRequest, UpdateCollaborationResponse>>()
            )
        } throws ResourceNotFoundException.builder().resourceId(TEST_COLLAB_ID).build()

        val result = handler.handleRequest(mockProxy, defaultUpdateRequest, null, logger)

        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
            assertThat(resourceModel).isNotNull
            assertThat(resourceModel.getCollaborationIdFromPrimaryIdentifier()).isEqualTo(
                TEST_COLLAB_ID
            )
        }
    }
}
