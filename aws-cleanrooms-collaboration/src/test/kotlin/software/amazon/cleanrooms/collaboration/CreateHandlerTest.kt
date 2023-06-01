package software.amazon.cleanrooms.collaboration

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.awssdk.services.cleanrooms.model.Collaboration
import software.amazon.awssdk.services.cleanrooms.model.CreateCollaborationRequest
import software.amazon.awssdk.services.cleanrooms.model.CreateCollaborationResponse
import software.amazon.awssdk.services.cleanrooms.model.GetCollaborationRequest
import software.amazon.awssdk.services.cleanrooms.model.GetCollaborationResponse
import software.amazon.awssdk.services.cleanrooms.model.InternalServerException
import software.amazon.awssdk.services.cleanrooms.model.ListCollaborationsRequest
import software.amazon.awssdk.services.cleanrooms.model.ListCollaborationsResponse
import software.amazon.awssdk.services.cleanrooms.model.ListMembersRequest
import software.amazon.awssdk.services.cleanrooms.model.ListMembersResponse
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.MemberSummary
import software.amazon.awssdk.services.cleanrooms.model.ResourceNotFoundException
import software.amazon.awssdk.services.cleanrooms.model.ValidationException
import software.amazon.cleanrooms.collaboration.typemapper.toMemberSpecification
import software.amazon.cleanrooms.collaboration.typemapper.toResourceModel
import software.amazon.cleanrooms.collaboration.typemapper.toResourceModelTags
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.HandlerErrorCode
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import java.util.function.Function

private val TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS.toResourceModel(TEST_CREATOR_MEMBER_SUMMARY.toMemberSpecification())

private val TEST_COLLABORATION_INPUT_RESOURCE_MODEL_WITH_ALL_ARGS = TEST_BASE_COLLABORATION_WITH_ALL_ARGS.toResourceModel(TEST_CREATOR_MEMBER_SUMMARY.toMemberSpecification())

private val STACK_LEVEL_TAGS = mapOf("stackTag" to "stackTagValue")

private val TEST_CREATE_INPUT_RESOURCE_MODEL = TEST_BASE_COLLABORATION_WITH_REQUIRED_FIELDS.toResourceModel(TEST_CREATOR_MEMBER_SUMMARY.toMemberSpecification())

class CreateHandlerTest {

    /**
     * Type to define test arguments for the readHandler tests.
     */
    data class Args(
        val testName: String,
        val requestInputModel: ResourceModel,
        val inputResourceLevelTags: Map<String, String> = emptyMap(),
        val inputStackLevelTags: Map<String, String> = emptyMap(),
        val collaborationFromApi: Collaboration,
        val membersFromApi: List<MemberSummary> = listOf(TEST_CREATOR_MEMBER_SUMMARY),
        val tagsFromApi: Map<String, String> = emptyMap(),
        val expectedResourceModel: ResourceModel
    )

    companion object {
        @JvmStatic
        fun createHandlerSuccessTestData() = listOf(
            Args(
                testName = "Collaboration with only required fields, no members and no resource or stack level tags created successfully.",
                requestInputModel = TEST_CREATE_INPUT_RESOURCE_MODEL,
                collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE
            ),
            Args(
                testName = "Collaboration with only required fields with members and no resource or stack level tags created successfully.",
                requestInputModel = TEST_CREATE_INPUT_RESOURCE_MODEL,
                collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                membersFromApi = listOf(TEST_MEMBER_SUMMARY, TEST_CREATOR_MEMBER_SUMMARY),
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE.toBuilder()
                    .members(listOf(TEST_MEMBER_SUMMARY).toMemberSpecification())
                    .build()
            ),
            Args(
                testName = "Collaboration with only required fields with members, with resource level tags and no stack level tags created successfully.",
                requestInputModel = TEST_CREATE_INPUT_RESOURCE_MODEL,
                inputResourceLevelTags = TEST_COLLABORATION_TAGS,
                collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                membersFromApi = listOf(TEST_MEMBER_SUMMARY, TEST_CREATOR_MEMBER_SUMMARY),
                tagsFromApi = TEST_COLLABORATION_TAGS + TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE.toBuilder()
                    .tags(TEST_COLLABORATION_TAGS.toResourceModelTags())
                    .members(listOf(TEST_MEMBER_SUMMARY).toMemberSpecification())
                    .build()
            ),
            Args(
                testName = "Collaboration with only required fields with no members, resource level tags and stack level tags created successfully. ",
                requestInputModel = TEST_CREATE_INPUT_RESOURCE_MODEL,
                inputResourceLevelTags = TEST_COLLABORATION_TAGS,
                inputStackLevelTags = STACK_LEVEL_TAGS,
                collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                tagsFromApi = TEST_COLLABORATION_TAGS + STACK_LEVEL_TAGS + TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE.toBuilder()
                    .tags((TEST_COLLABORATION_TAGS + STACK_LEVEL_TAGS).toResourceModelTags())
                    .build()
            ),
            Args(
                testName = "Collaboration with all args with members, resource level tags and stack level tags created successfully. ",
                requestInputModel = TEST_COLLABORATION_INPUT_RESOURCE_MODEL_WITH_ALL_ARGS,
                inputResourceLevelTags = TEST_COLLABORATION_TAGS,
                inputStackLevelTags = STACK_LEVEL_TAGS,
                collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_ALL_ARGS,
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_COLLABORATION_RESPONSE_WITH_ALL_ARGS.toResourceModel(TEST_CREATOR_MEMBER_SUMMARY.toMemberSpecification())
            )
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

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListMembersRequest::class),
                any<Function<ListMembersRequest, ListMembersResponse>>()
            )
        } returns ListMembersResponse.builder().memberSummaries(listOf(TEST_CREATOR_MEMBER_SUMMARY)).build()
    }

    @ParameterizedTest
    @MethodSource("createHandlerSuccessTestData")
    fun `Test Complete Resource Creation and stabilization returns successful`(testArgs: Args) {
        // This test assumes that our resource does stabilize after creation immediately and does not require extra retries.
        // This is because, we already have tests for stabilization cases other than the end to end case which is being tested by this method.
        with(testArgs) {
            // setting the mocks up
            every {
                mockProxy.injectCredentialsAndInvokeV2(
                    ofType(CreateCollaborationRequest::class),
                    any<Function<CreateCollaborationRequest, CreateCollaborationResponse>>()
                )
            } returns CreateCollaborationResponse.builder().collaboration(collaborationFromApi).build()

            every {
                mockProxy.injectCredentialsAndInvokeV2(
                    ofType(ListMembersRequest::class),
                    any<Function<ListMembersRequest, ListMembersResponse>>()
                )
            } returns ListMembersResponse.builder().memberSummaries(membersFromApi).build()

            every {
                mockProxy.injectCredentialsAndInvokeV2(
                    ofType(ListTagsForResourceRequest::class),
                    any<Function<ListTagsForResourceRequest, ListTagsForResourceResponse>>()
                )
            } returns ListTagsForResourceResponse.builder().tags(tagsFromApi).build()

            every {
                mockProxy.injectCredentialsAndInvokeV2(
                    ofType(GetCollaborationRequest::class),
                    any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
                )
            } returns GetCollaborationResponse.builder().collaboration(collaborationFromApi).build()

            every {
                mockProxy.injectCredentialsAndInvokeV2(
                    ofType(ListCollaborationsRequest::class),
                    any<Function<ListCollaborationsRequest, ListCollaborationsResponse>>()
                )
            } returns ListCollaborationsResponse.builder().collaborationList(TEST_COLLABORATION_SUMMARY_1, TEST_COLLABORATION_SUMMARY_2).build()

            // building the request and making the call, and processing the callback.
            val request = ResourceHandlerRequest.builder<ResourceModel>()
                .desiredResourceState(requestInputModel)
                .desiredResourceTags(inputResourceLevelTags + inputStackLevelTags)
                .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
                .systemTags(TEST_SYSTEM_TAGS)
                .build()

            val resultAfterCreation = handler.handleRequest(mockProxy, request, null, logger)

            // This is the stage where we test resource is correctly created, and verify three things.
            // 1. the resource has arn and id
            // 2. we have correct callback context.
            // 3. we have correct retry count.
            // 4. we have status of IN_PROGRESS.
            with(resultAfterCreation) {
                assertThat(callbackContext).isEqualTo(
                    CallbackContext(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES, pendingStabilization = true)
                )
                assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
                assertThat(callbackDelaySeconds).isEqualTo(HandlerCommon.CALLBACK_DELAY_IN_SECONDS)
                assertThat(resourceModel).isEqualTo(collaborationFromApi.toResourceModel(TEST_CREATOR_MEMBER_SUMMARY.toMemberSpecification()))
                assertThat(resourceModel.getCollaborationIdFromPrimaryIdentifier()).isEqualTo(TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE.getCollaborationIdFromPrimaryIdentifier())
                assertThat(resourceModel.arn).isEqualTo(TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE.arn)
            }

            // Now we will make another request to pass into the stabilizers.
            val requestForStabilization = ResourceHandlerRequest.builder<ResourceModel>()
                .desiredResourceState(resultAfterCreation.resourceModel)
                .desiredResourceTags(request.desiredResourceTags)
                .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
                .build()

            val resultFromStabilizer = handler.handleRequest(mockProxy, requestForStabilization, resultAfterCreation.callbackContext, logger)

            // This is the stage where we verify that our resource is correctly read and returned successfully.
            // We will verify the following things:
            // 1. the resource arn and id is correct.
            // 2. the resource has all the information such as tags and other properties as we want in the final expectedResourceModel.
            with(resultFromStabilizer) {
                assertThat(resourceModel).isEqualTo(expectedResourceModel)
                assertThat(status).isEqualTo(OperationStatus.SUCCESS)
                assertThat(resourceModel.arn).isEqualTo(expectedResourceModel.arn)
                assertThat(resourceModel.getCollaborationIdFromPrimaryIdentifier()).isEqualTo(expectedResourceModel.getCollaborationIdFromPrimaryIdentifier())
            }
        }
    }

    @Test
    fun `CreateHandler returns IN PROGRESS once the collaboration is created and before it is stabilized`() {
        // setup the mocks.
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(CreateCollaborationRequest::class),
                any<Function<CreateCollaborationRequest, CreateCollaborationResponse>>()
            )
        } returns CreateCollaborationResponse.builder().collaboration(TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS).build()

        // make the actual call.
        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .desiredResourceState(TEST_CREATE_INPUT_RESOURCE_MODEL)
            .desiredResourceTags(TEST_COLLABORATION_TAGS)
            .build()

        val result = handler.handleRequest(mockProxy, request, null, logger)
        with(result) {
            assertThat(callbackContext).isEqualTo(
                CallbackContext(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES, pendingStabilization = true)
            )
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(callbackDelaySeconds).isEqualTo(HandlerCommon.CALLBACK_DELAY_IN_SECONDS)
            assertThat(resourceModel).isEqualTo(TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
            assertThat(resourceModel.getCollaborationIdFromPrimaryIdentifier()).isEqualTo(TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE.getCollaborationIdFromPrimaryIdentifier())
            assertThat(resourceModel.arn).isEqualTo(TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE.arn)
        }
    }

    @Test
    fun `CreateHandler returns SUCCESS after create collaboration stabilizes`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } returns GetCollaborationResponse.builder()
            .collaboration(TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS)
            .build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListCollaborationsRequest::class),
                any<Function<ListCollaborationsRequest, ListCollaborationsResponse>>()
            )
        } returns ListCollaborationsResponse.builder()
            .collaborationList(listOf(TEST_COLLABORATION_SUMMARY_1))
            .build()

        val callbackContext = CallbackContext(
            stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
            pendingStabilization = true
        )

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, request, callbackContext, logger)

        assertThat(result).isNotNull
        with(result) {
            // Checking the status is SUCCESS
            assertThat(status).isEqualTo(OperationStatus.SUCCESS)
            assertThat(resourceModel).isEqualTo(TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
        }
    }

    @Test
    fun `CreateHandler returns IN_PROGRESS if resource identifier is not returned in ListHandler during stabilization`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } returns GetCollaborationResponse.builder()
            .collaboration(TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS)
            .build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListCollaborationsRequest::class),
                any<Function<ListCollaborationsRequest, ListCollaborationsResponse>>()
            )
        } returns ListCollaborationsResponse.builder()
            .collaborationList(listOf(TEST_COLLABORATION_SUMMARY_2))
            .build()

        val callbackContext = CallbackContext(
            stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
            pendingStabilization = true
        )

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, request, callbackContext, logger)

        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(result.callbackContext?.stabilizationRetriesRemaining).isEqualTo(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES - 1)
            with(resourceModel) {
                assertThat(this).isEqualTo(TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
                assertThat(getCollaborationIdFromPrimaryIdentifier()).isEqualTo(TEST_COLLAB_ID)
                assertThat(arn).isEqualTo(TEST_COLLAB_ARN)
            }
        }
    }

    @Test
    fun `CreateHandler returns IN_PROGRESS if failed returned from ReadHandler during stabilization`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_COLLAB_ID)
            .build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListCollaborationsRequest::class),
                any<Function<ListCollaborationsRequest, ListCollaborationsResponse>>()
            )
        } returns ListCollaborationsResponse.builder()
            .collaborationList(listOf(TEST_COLLABORATION_SUMMARY_1))
            .build()

        val callbackContext = CallbackContext(
            stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
            pendingStabilization = true
        )

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, request, callbackContext, logger)

        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(result.callbackContext?.stabilizationRetriesRemaining).isEqualTo(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES - 1)
            with(resourceModel) {
                assertThat(this).isEqualTo(TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
                assertThat(getCollaborationIdFromPrimaryIdentifier()).isEqualTo(TEST_COLLAB_ID)
                assertThat(arn).isEqualTo(TEST_COLLAB_ARN)
            }
        }
    }

    @Test
    fun `CreateHandler returns IN_PROGRESS if exception thrown from ListHandler during stabilization`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } returns GetCollaborationResponse.builder()
            .collaboration(TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS)
            .build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListCollaborationsRequest::class),
                any<Function<ListCollaborationsRequest, ListCollaborationsResponse>>()
            )
        } throws ValidationException.builder().build()

        val callbackContext = CallbackContext(
            stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
            pendingStabilization = true
        )

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, request, callbackContext, logger)

        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(result.callbackContext?.stabilizationRetriesRemaining).isEqualTo(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES - 1)
            with(resourceModel) {
                assertThat(this).isEqualTo(TEST_COLLABORATION_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
                assertThat(getCollaborationIdFromPrimaryIdentifier()).isEqualTo(TEST_COLLAB_ID)
                assertThat(arn).isEqualTo(TEST_COLLAB_ARN)
            }
        }
    }

    @Test
    fun `CreateHandler returns FAILED with NotFound if createHandler fails during createCollaboration call`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(CreateCollaborationRequest::class),
                any<Function<CreateCollaborationRequest, CreateCollaborationResponse>>()
            )
        } throws InternalServerException.builder().build()

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_CREATE_INPUT_RESOURCE_MODEL)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .desiredResourceTags(TEST_COLLABORATION_TAGS)
            .systemTags(TEST_SYSTEM_TAGS)
            .build()

        val result = handler.handleRequest(mockProxy, request, null, logger)

        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(resourceModel).isEqualTo(TEST_CREATE_INPUT_RESOURCE_MODEL)
        }
    }

    @Test
    fun `CreateHandler returns FAILED with NOT STABILIZED if resource is not stabilized within retry poll count`() {
        val callbackContext = CallbackContext(stabilizationRetriesRemaining = 0, pendingStabilization = true)

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(
                TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS.toResourceModel(
                    TEST_CREATOR_MEMBER_SUMMARY.toMemberSpecification(),
                    listOf(TEST_MEMBER_SUMMARY, TEST_CREATOR_MEMBER_SUMMARY).toMemberSpecification(),
                    emptySet()
                )
            )
            .build()

        val result = handler.handleRequest(mockProxy, request, callbackContext, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotStabilized)
        }
    }
}
