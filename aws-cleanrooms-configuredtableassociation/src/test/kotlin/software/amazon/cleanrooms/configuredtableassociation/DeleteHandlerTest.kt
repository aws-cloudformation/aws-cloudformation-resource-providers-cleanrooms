package software.amazon.cleanrooms.configuredtableassociation

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.awssdk.services.cleanrooms.model.DeleteConfiguredTableAssociationRequest
import software.amazon.awssdk.services.cleanrooms.model.DeleteConfiguredTableAssociationResponse
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableAssociationRequest
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableAssociationResponse
import software.amazon.awssdk.services.cleanrooms.model.ListConfiguredTableAssociationsRequest
import software.amazon.awssdk.services.cleanrooms.model.ListConfiguredTableAssociationsResponse
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.ResourceNotFoundException
import software.amazon.awssdk.services.cleanrooms.model.ValidationException
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_CONFIGURED_TABLE_ASSOCIATION_REQUIRED_FIELDS
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_CONFIGURED_TABLE_ASSOCIATION_SUMMARY_1
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_CONFIGURED_TABLE_ASSOCIATION_SUMMARY_2
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_CREATOR_ACCOUNT_ID
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_CTA_ID
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_MEMBERSHIP_ID
import software.amazon.cleanrooms.configuredtableassociation.TestData.TEST_SYSTEM_TAGS
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.HandlerErrorCode
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import java.util.function.Function

val TEST_DELETE_RESOURCE_MODEL: ResourceModel = ResourceModel.builder()
    .configuredTableAssociationIdentifier(TEST_CTA_ID)
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
                ofType(GetConfiguredTableAssociationRequest::class),
                any<Function<GetConfiguredTableAssociationRequest, GetConfiguredTableAssociationResponse>>()
            )
        } returns GetConfiguredTableAssociationResponse.builder()
            .configuredTableAssociation(TEST_CONFIGURED_TABLE_ASSOCIATION_REQUIRED_FIELDS)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(DeleteConfiguredTableAssociationRequest::class),
                any<Function<DeleteConfiguredTableAssociationRequest, DeleteConfiguredTableAssociationResponse>>()
            )
        } returns DeleteConfiguredTableAssociationResponse.builder().build()

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
                ofType(GetConfiguredTableAssociationRequest::class),
                any<Function<GetConfiguredTableAssociationRequest, GetConfiguredTableAssociationResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_MEMBERSHIP_ID)
            .build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListConfiguredTableAssociationsRequest::class),
                any<Function<ListConfiguredTableAssociationsRequest, ListConfiguredTableAssociationsResponse>>()
            )
        } returns ListConfiguredTableAssociationsResponse.builder()
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
                ofType(GetConfiguredTableAssociationRequest::class),
                any<Function<GetConfiguredTableAssociationRequest, GetConfiguredTableAssociationResponse>>()
            )
        } returns GetConfiguredTableAssociationResponse.builder()
            .configuredTableAssociation(TEST_CONFIGURED_TABLE_ASSOCIATION_REQUIRED_FIELDS)
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListConfiguredTableAssociationsRequest::class),
                any<Function<ListConfiguredTableAssociationsRequest, ListConfiguredTableAssociationsResponse>>()
            )
        } returns ListConfiguredTableAssociationsResponse.builder()
            .configuredTableAssociationSummaries(listOf(TEST_CONFIGURED_TABLE_ASSOCIATION_SUMMARY_2))
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
                ofType(GetConfiguredTableAssociationRequest::class),
                any<Function<GetConfiguredTableAssociationRequest, GetConfiguredTableAssociationResponse>>()
            )
        } throws ValidationException.builder().build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListConfiguredTableAssociationsRequest::class),
                any<Function<ListConfiguredTableAssociationsRequest, ListConfiguredTableAssociationsResponse>>()
            )
        } returns ListConfiguredTableAssociationsResponse.builder()
            .configuredTableAssociationSummaries(listOf(TEST_CONFIGURED_TABLE_ASSOCIATION_SUMMARY_2))
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
                ofType(GetConfiguredTableAssociationRequest::class),
                any<Function<GetConfiguredTableAssociationRequest, GetConfiguredTableAssociationResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_CTA_ID)
            .build()
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListConfiguredTableAssociationsRequest::class),
                any<Function<ListConfiguredTableAssociationsRequest, ListConfiguredTableAssociationsResponse>>()
            )
        } returns ListConfiguredTableAssociationsResponse.builder()
            .configuredTableAssociationSummaries(listOf(TEST_CONFIGURED_TABLE_ASSOCIATION_SUMMARY_1))
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
                ofType(GetConfiguredTableAssociationRequest::class),
                any<Function<GetConfiguredTableAssociationRequest, GetConfiguredTableAssociationResponse>>()
            )
        } returns GetConfiguredTableAssociationResponse.builder().configuredTableAssociation(TEST_CONFIGURED_TABLE_ASSOCIATION_REQUIRED_FIELDS).build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListConfiguredTableAssociationsRequest::class),
                any<Function<ListConfiguredTableAssociationsRequest, ListConfiguredTableAssociationsResponse>>()
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
    fun `DeleteHandler returns FAILED with NOT FOUND if cta is not found before delete cta`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableAssociationRequest::class),
                any<Function<GetConfiguredTableAssociationRequest, GetConfiguredTableAssociationResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_CTA_ID)
            .build()

        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, null, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `DeleteHandler returns FAILED with NOT FOUND if cta is not found during delete cta`() {
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
                ofType(DeleteConfiguredTableAssociationRequest::class),
                any<Function<DeleteConfiguredTableAssociationRequest, DeleteConfiguredTableAssociationResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_CTA_ID)
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
