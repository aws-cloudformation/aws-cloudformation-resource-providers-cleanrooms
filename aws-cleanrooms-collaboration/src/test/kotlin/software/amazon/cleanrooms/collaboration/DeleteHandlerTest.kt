package software.amazon.cleanrooms.collaboration

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.awssdk.services.cleanrooms.model.DeleteCollaborationRequest
import software.amazon.awssdk.services.cleanrooms.model.DeleteCollaborationResponse
import software.amazon.awssdk.services.cleanrooms.model.GetCollaborationRequest
import software.amazon.awssdk.services.cleanrooms.model.GetCollaborationResponse
import software.amazon.awssdk.services.cleanrooms.model.ListCollaborationsRequest
import software.amazon.awssdk.services.cleanrooms.model.ListCollaborationsResponse
import software.amazon.awssdk.services.cleanrooms.model.ListMembersRequest
import software.amazon.awssdk.services.cleanrooms.model.ListMembersResponse
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

val TEST_DELETE_RESOURCE_MODEL: ResourceModel = ResourceModel.builder()
    .collaborationIdentifier(TEST_COLLAB_ID)
    .build()

private val TEST_DELETE_REQUEST = ResourceHandlerRequest.builder<ResourceModel>()
    .desiredResourceState(TEST_DELETE_RESOURCE_MODEL)
    .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
    .build()

class DeleteHandlerTest {
    private val mockCleanRoomsClient: CleanRoomsClient = mockk()

    private val mockProxy: AmazonWebServicesClientProxy = mockk()

    private val logger: Logger = LoggerProxy()

    private val handler: DeleteHandler = DeleteHandler()

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

    @Test
    fun `DeleteHandler returns IN PROGRESS after deleting collaboration`() {
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
                ofType(DeleteCollaborationRequest::class),
                any<Function<DeleteCollaborationRequest, DeleteCollaborationResponse>>()
            )
        } returns DeleteCollaborationResponse.builder().build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, null, logger)

        assertThat(result).isNotNull
        with(result) {
            // Checking the status is IN PROGRESS
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(resourceModel).isEqualTo(TEST_DELETE_RESOURCE_MODEL)
        }
    }

    @Test
    fun `DeleteHandler returns SUCCESS after delete collaboration stabilizes`() {
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
            .build()

        val callbackContext = CallbackContext(
            stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
            pendingStabilization = true
        )

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, callbackContext, logger)

        assertThat(result).isNotNull
        with(result) {
            // Checking the status is SUCCESS
            assertThat(status).isEqualTo(OperationStatus.SUCCESS)
            assertThat(resourceModel).isNull()
        }
    }

    @Test
    fun `DeleteHandler returns IN_PROGRESS if resource identifier is returned from ReadHandler during stabilization`() {
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

        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, callbackContext, logger)
        assertThat(result).isNotNull
        with(result) {
            // Checking the status is IN PROGRESS
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(resourceModel).isEqualTo(TEST_DELETE_RESOURCE_MODEL)
        }
    }

    @Test
    fun `DeleteHandler returns IN_PROGRESS if NON NotFound errorCode is returned from ReadHandler during stabilization`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } throws ValidationException.builder().build()
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

        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, callbackContext, logger)
        assertThat(result).isNotNull
        with(result) {
            // Checking the status is IN PROGRESS
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(resourceModel).isEqualTo(TEST_DELETE_RESOURCE_MODEL)
        }
    }

    @Test
    fun `DeleteHandler returns IN_PROGRESS if resource identifier is returned in ListHandler during stabilization`() {
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

        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, callbackContext, logger)
        assertThat(result).isNotNull
        with(result) {
            // Checking the status is IN PROGRESS
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(resourceModel).isEqualTo(TEST_DELETE_RESOURCE_MODEL)
        }
    }

    @Test
    fun `DeleteHandler returns IN_PROGRESS if exception thrown from ListHandler during stabilization`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } returns GetCollaborationResponse.builder().collaboration(TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS).build()

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

        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, callbackContext, logger)
        assertThat(result).isNotNull
        with(result) {
            // Checking the status is IN PROGRESS
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(resourceModel).isEqualTo(TEST_DELETE_RESOURCE_MODEL)
        }
    }

    @Test
    fun `DeleteHandler returns FAILED with NOT FOUND if collaboration is not found before delete collaboration`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_COLLAB_ID)
            .build()

        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, null, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `DeleteHandler returns FAILED with NOT FOUND if collaboration is not found during delete collaboration`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } returns GetCollaborationResponse.builder()
            .collaboration(TEST_COLLABORATION_RESPONSE_WITH_ALL_ARGS)
            .build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(DeleteCollaborationRequest::class),
                any<Function<DeleteCollaborationRequest, DeleteCollaborationResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_COLLAB_ID)
            .build()

        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, null, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `DeleteHandler returns FAILED with NOT STABILIZED if resource is not stabilized within retry poll count`() {
        val callbackContext = CallbackContext(stabilizationRetriesRemaining = 0, pendingStabilization = true)

        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, callbackContext, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotStabilized)
        }
    }
}
