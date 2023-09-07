package software.amazon.cleanrooms.membership


import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.awssdk.services.cleanrooms.model.CreateMembershipRequest
import software.amazon.awssdk.services.cleanrooms.model.CreateMembershipResponse
import software.amazon.awssdk.services.cleanrooms.model.GetMembershipRequest
import software.amazon.awssdk.services.cleanrooms.model.GetMembershipResponse
import software.amazon.awssdk.services.cleanrooms.model.ListMembershipsRequest
import software.amazon.awssdk.services.cleanrooms.model.ListMembershipsResponse
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.Membership
import software.amazon.awssdk.services.cleanrooms.model.ResourceNotFoundException
import software.amazon.awssdk.services.cleanrooms.model.ValidationException
import software.amazon.cleanrooms.membership.typemapper.toResourceModel
import software.amazon.cleanrooms.membership.typemapper.toResourceModelTags
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.HandlerErrorCode
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import java.util.function.Function

private val TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE = TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS.toResourceModel()
private val TEST_MEMBERSHIP_RESOURCE_MODEL_WITH_RESULT_CONFIG_AFTER_CREATE_COMPLETE = TEST_MEMBERSHIP_RESPONSE_WITH_ALL_ARGS.toResourceModel()

private val STACK_LEVEL_TAGS = mapOf("stackTag" to "stackTagValue")

private val TEST_CREATE_INPUT_RESOURCE_MODEL = TEST_BASE_MEMBERSHIP_WITH_REQUIRED_FIELDS.toResourceModel()

private val TEST_CREATE_INPUT_RESOURCE_MODEL_WITH_RESULT_CONFIG = TEST_MEMBERSHIP_WITH_RESULT_CONFIGURATION.toResourceModel()

class CreateHandlerTest {

    /**
     * Type to define test arguments for the readHandler tests.
     */
    data class Args(
        val testName: String,
        val requestInputModel: ResourceModel,
        val inputResourceLevelTags: Map<String, String> = emptyMap(),
        val inputStackLevelTags: Map<String, String> = emptyMap(),
        val membershipFromApi: Membership,
        val tagsFromApi: Map<String, String> = emptyMap(),
        val expectedResourceModel: ResourceModel
    )

    companion object {
        @JvmStatic
        fun createHandlerSuccessTestData() = listOf(
            Args(
                testName = "Membership with required fields and no resource or stack level tags created successfully.",
                requestInputModel = TEST_CREATE_INPUT_RESOURCE_MODEL,
                membershipFromApi = TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS,
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE
            ),
            Args(
                testName = "Membership with resource level tags and no stack level tags created successfully.",
                requestInputModel = TEST_CREATE_INPUT_RESOURCE_MODEL,
                inputResourceLevelTags = TEST_MEMBERSHIP_TAGS,
                membershipFromApi = TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS,
                tagsFromApi = TEST_MEMBERSHIP_TAGS + TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE.toBuilder()
                    .tags(TEST_MEMBERSHIP_TAGS.toResourceModelTags())
                    .build()
            ),
            Args(
                testName = "Membership with resource level tags and stack level tags created successfully. ",
                requestInputModel = TEST_CREATE_INPUT_RESOURCE_MODEL,
                inputResourceLevelTags = TEST_MEMBERSHIP_TAGS,
                inputStackLevelTags = STACK_LEVEL_TAGS,
                membershipFromApi = TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS,
                tagsFromApi = TEST_MEMBERSHIP_TAGS + STACK_LEVEL_TAGS + TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE.toBuilder()
                    .tags((TEST_MEMBERSHIP_TAGS + STACK_LEVEL_TAGS).toResourceModelTags())
                    .build()
            ),
            Args(
                testName = "Membership with result configuration for reciprocal query works successfully. ",
                requestInputModel = TEST_CREATE_INPUT_RESOURCE_MODEL_WITH_RESULT_CONFIG,
                inputResourceLevelTags = TEST_MEMBERSHIP_TAGS,
                inputStackLevelTags = STACK_LEVEL_TAGS,
                membershipFromApi = TEST_MEMBERSHIP_RESPONSE_WITH_ALL_ARGS,
                tagsFromApi = TEST_MEMBERSHIP_TAGS + STACK_LEVEL_TAGS + TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_MEMBERSHIP_RESOURCE_MODEL_WITH_RESULT_CONFIG_AFTER_CREATE_COMPLETE.toBuilder()
                    .tags((TEST_MEMBERSHIP_TAGS + STACK_LEVEL_TAGS).toResourceModelTags())
                    .build()
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
                    ofType(CreateMembershipRequest::class),
                    any<Function<CreateMembershipRequest, CreateMembershipResponse>>()
                )
            } returns CreateMembershipResponse.builder().membership(membershipFromApi).build()

            every {
                mockProxy.injectCredentialsAndInvokeV2(
                    ofType(ListTagsForResourceRequest::class),
                    any<Function<ListTagsForResourceRequest, ListTagsForResourceResponse>>()
                )
            } returns ListTagsForResourceResponse.builder().tags(tagsFromApi).build()

            every {
                mockProxy.injectCredentialsAndInvokeV2(
                    ofType(GetMembershipRequest::class),
                    any<Function<GetMembershipRequest, GetMembershipResponse>>()
                )
            } returns GetMembershipResponse.builder().membership(membershipFromApi).build()

            every {
                mockProxy.injectCredentialsAndInvokeV2(
                    ofType(ListMembershipsRequest::class),
                    any<Function<ListMembershipsRequest, ListMembershipsResponse>>()
                )
            } returns ListMembershipsResponse.builder().membershipSummaries(TEST_MEMBERSHIP_SUMMARY_1, TEST_MEMBERSHIP_SUMMARY_2).build()

            // building the request and making the call, and processing the callback.
            val request = ResourceHandlerRequest.builder<ResourceModel>()
                .desiredResourceState(requestInputModel)
                .desiredResourceTags(inputResourceLevelTags + inputStackLevelTags)
                .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
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
                assertThat(resourceModel).isEqualTo(membershipFromApi.toResourceModel())
                assertThat(resourceModel.getMembershipIdFromPrimaryIdentifier()).isEqualTo(TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE.getMembershipIdFromPrimaryIdentifier())
                assertThat(resourceModel.arn).isEqualTo(TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE.arn)
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
                assertThat(resourceModel.getMembershipIdFromPrimaryIdentifier()).isEqualTo(expectedResourceModel.getMembershipIdFromPrimaryIdentifier())
            }
        }
    }

    @Test
    fun `CreateHandler returns IN PROGRESS once the membership is created and before it is stabilized`() {
        // setup the mocks.
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(CreateMembershipRequest::class),
                any<Function<CreateMembershipRequest, CreateMembershipResponse>>()
            )
        } returns CreateMembershipResponse.builder().membership(TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS).build()

        // make the actual call.
        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .desiredResourceState(TEST_CREATE_INPUT_RESOURCE_MODEL)
            .desiredResourceTags(TEST_MEMBERSHIP_TAGS)
            .build()

        val result = handler.handleRequest(mockProxy, request, null, logger)
        with(result) {
            assertThat(callbackContext).isEqualTo(
                CallbackContext(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES, pendingStabilization = true)
            )
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(callbackDelaySeconds).isEqualTo(HandlerCommon.CALLBACK_DELAY_IN_SECONDS)
            assertThat(resourceModel).isEqualTo(TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
            assertThat(resourceModel.getMembershipIdFromPrimaryIdentifier()).isEqualTo(TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE.getMembershipIdFromPrimaryIdentifier())
            assertThat(resourceModel.arn).isEqualTo(TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE.arn)
        }
    }

    @Test
    fun `CreateHandler returns SUCCESS after create membership stabilizes`() {
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
                ofType(ListMembershipsRequest::class),
                any<Function<ListMembershipsRequest, ListMembershipsResponse>>()
            )
        } returns ListMembershipsResponse.builder()
            .membershipSummaries(listOf(TEST_MEMBERSHIP_SUMMARY_1))
            .build()

        val callbackContext = CallbackContext(
            stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
            pendingStabilization = true
        )

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, request, callbackContext, logger)

        assertThat(result).isNotNull
        with(result) {
            // Checking the status is SUCCESS
            assertThat(status).isEqualTo(OperationStatus.SUCCESS)
            assertThat(resourceModel).isEqualTo(TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
        }
    }

    @Test
    fun `CreateHandler returns IN_PROGRESS if resource identifier is not returned in ListHandler during stabilization`() {
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
                ofType(ListMembershipsRequest::class),
                any<Function<ListMembershipsRequest, ListMembershipsResponse>>()
            )
        } returns ListMembershipsResponse.builder()
            .membershipSummaries(listOf(TEST_MEMBERSHIP_SUMMARY_2))
            .build()

        val callbackContext = CallbackContext(
            stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
            pendingStabilization = true
        )

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, request, callbackContext, logger)

        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(result.callbackContext?.stabilizationRetriesRemaining).isEqualTo(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES - 1)
            with(resourceModel) {
                assertThat(this).isEqualTo(TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
                assertThat(getMembershipIdFromPrimaryIdentifier()).isEqualTo(TEST_MEMBERSHIP_ID)
                assertThat(arn).isEqualTo(TEST_MEMBERSHIP_ARN)
            }
        }
    }

    @Test
    fun `CreateHandler returns IN_PROGRESS if failed returned from ReadHandler during stabilization`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetMembershipRequest::class),
                any<Function<GetMembershipRequest, GetMembershipResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_MEMBERSHIP_ID)
            .build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListMembershipsRequest::class),
                any<Function<ListMembershipsRequest, ListMembershipsResponse>>()
            )
        } returns ListMembershipsResponse.builder()
            .membershipSummaries(listOf(TEST_MEMBERSHIP_SUMMARY_1))
            .build()

        val callbackContext = CallbackContext(
            stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
            pendingStabilization = true
        )

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, request, callbackContext, logger)

        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(result.callbackContext?.stabilizationRetriesRemaining).isEqualTo(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES - 1)
            with(resourceModel) {
                assertThat(this).isEqualTo(TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
                assertThat(getMembershipIdFromPrimaryIdentifier()).isEqualTo(TEST_MEMBERSHIP_ID)
                assertThat(arn).isEqualTo(TEST_MEMBERSHIP_ARN)
            }
        }
    }

    @Test
    fun `CreateHandler returns IN_PROGRESS if exception thrown from ListHandler during stabilization`() {
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
                ofType(ListMembershipsRequest::class),
                any<Function<ListMembershipsRequest, ListMembershipsResponse>>()
            )
        } throws ValidationException.builder().build()

        val callbackContext = CallbackContext(
            stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
            pendingStabilization = true
        )

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, request, callbackContext, logger)

        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(result.callbackContext?.stabilizationRetriesRemaining).isEqualTo(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES - 1)
            with(resourceModel) {
                assertThat(this).isEqualTo(TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
                assertThat(getMembershipIdFromPrimaryIdentifier()).isEqualTo(TEST_MEMBERSHIP_ID)
                assertThat(arn).isEqualTo(TEST_MEMBERSHIP_ARN)
            }
        }
    }

    @Test
    fun `CreateHandler returns FAILED with NotFound if collaboration is not found during create membership`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(CreateMembershipRequest::class),
                any<Function<CreateMembershipRequest, CreateMembershipResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_COLLABORATION_ID)
            .build()

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(TEST_MEMBERSHIP_RESOURCE_MODEL_AFTER_CREATE_COMPLETE)
            .desiredResourceTags(emptyMap())
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .systemTags(TEST_SYSTEM_TAGS)
            .build()

        val result = handler.handleRequest(mockProxy, request, null, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `CreateHandler returns FAILED with NOT STABILIZED if resource is not stabilized within retry poll count`() {
        val callbackContext = CallbackContext(stabilizationRetriesRemaining = 0, pendingStabilization = true)

        val request = ResourceHandlerRequest.builder<ResourceModel>()
            .desiredResourceState(
                TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS.toResourceModel(
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
