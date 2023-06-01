package software.amazon.cleanrooms.membership

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.awssdk.services.cleanrooms.model.DeleteMembershipRequest
import software.amazon.awssdk.services.cleanrooms.model.DeleteMembershipResponse
import software.amazon.awssdk.services.cleanrooms.model.GetMembershipRequest
import software.amazon.awssdk.services.cleanrooms.model.GetMembershipResponse
import software.amazon.awssdk.services.cleanrooms.model.ListMembershipsRequest
import software.amazon.awssdk.services.cleanrooms.model.ListMembershipsResponse
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
    .membershipIdentifier(TEST_MEMBERSHIP_ID)
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
    }

    @Test
    fun `DeleteHandler returns IN PROGRESS after deleting membership`() {
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
                ofType(DeleteMembershipRequest::class),
                any<Function<DeleteMembershipRequest, DeleteMembershipResponse>>()
            )
        } returns DeleteMembershipResponse.builder().build()

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
    fun `DeleteHandler returns SUCCESS after delete membership stabilizes`() {
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
                ofType(GetMembershipRequest::class),
                any<Function<GetMembershipRequest, GetMembershipResponse>>()
            )
        } throws ValidationException.builder().build()
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
                ofType(GetMembershipRequest::class),
                any<Function<GetMembershipRequest, GetMembershipResponse>>()
            )
        } returns GetMembershipResponse.builder().membership(TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS).build()

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

        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, callbackContext, logger)
        assertThat(result).isNotNull
        with(result) {
            // Checking the status is IN PROGRESS
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(resourceModel).isEqualTo(TEST_DELETE_RESOURCE_MODEL)
        }
    }

    @Test
    fun `DeleteHandler returns FAILED with NOT FOUND if membership is not found before delete membership`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetMembershipRequest::class),
                any<Function<GetMembershipRequest, GetMembershipResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_MEMBERSHIP_ID)
            .build()

        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, null, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `DeleteHandler returns FAILED with NOT FOUND if membership is not found during delete membership`() {
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
                ofType(DeleteMembershipRequest::class),
                any<Function<DeleteMembershipRequest, DeleteMembershipResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_MEMBERSHIP_ID)
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
