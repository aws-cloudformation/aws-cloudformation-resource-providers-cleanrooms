package software.amazon.cleanrooms.configuredtable

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.awssdk.services.cleanrooms.model.DeleteConfiguredTableRequest
import software.amazon.awssdk.services.cleanrooms.model.DeleteConfiguredTableResponse
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
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.OperationStatus
import java.util.function.Function

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
    fun `DeleteHandler returns IN PROGRESS after deleting configured table`() {
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
                ofType(DeleteConfiguredTableRequest::class),
                any<Function<DeleteConfiguredTableRequest, DeleteConfiguredTableResponse>>()
            )
        } returns DeleteConfiguredTableResponse.builder().build()

                // Actual call to the handler
        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, null, logger)

        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(resourceModel).isEqualTo(TEST_DELETE_RESOURCE_MODEL)
            assertThat(callbackContext).isNotNull
            with(callbackContext) {
                assertThat(this?.stabilizationRetriesRemaining).isEqualTo(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES)
                assertThat(this?.pendingStabilization)
            }
        }
    }

    @Test
    fun `DeleteHandler returns SUCCESS after delete configured table stabilizes`() {
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
            .build()

        val inputCallbackContext = CallbackContext(stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
            pendingStabilization = true)

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, inputCallbackContext, logger)

        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.SUCCESS)
            assertThat(resourceModel).isNull()
            assertThat(callbackContext).isNull()
        }
    }

    @Test
    fun `DeleteHandler returns IN_PROGRESS if resource identifier is returned from ReadHandler during stabilization`() {
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

        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, inputCallbackContext, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(resourceModel).isEqualTo(TEST_DELETE_RESOURCE_MODEL)
            assertThat(callbackContext).isNotNull
            with(callbackContext) {
                assertThat(this?.stabilizationRetriesRemaining).isEqualTo(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES - 1)
                assertThat(this?.pendingStabilization)
            }
        }
    }

    @Test
    fun `DeleteHandler returns IN_PROGRESS if NON NotFound errorCode is returned from ReadHandler during stabilization`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableRequest::class),
                any<Function<GetConfiguredTableRequest, GetConfiguredTableResponse>>()
            )
        } throws ValidationException.builder().build()
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

        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, inputCallbackContext, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(resourceModel).isEqualTo(TEST_DELETE_RESOURCE_MODEL)
            assertThat(callbackContext).isNotNull
            with(callbackContext) {
                assertThat(this?.stabilizationRetriesRemaining).isEqualTo(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES - 1)
                assertThat(this?.pendingStabilization)
            }
        }
    }

    @Test
    fun `DeleteHandler returns IN_PROGRESS if resource identifier is returned in ListHandler during stabilization`() {
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

        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, inputCallbackContext, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(resourceModel).isEqualTo(TEST_DELETE_RESOURCE_MODEL)
            assertThat(callbackContext).isNotNull
            with(callbackContext) {
                assertThat(this?.stabilizationRetriesRemaining).isEqualTo(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES - 1)
                assertThat(this?.pendingStabilization)
            }
        }
    }

    @Test
    fun `DeleteHandler returns IN_PROGRESS if exception thrown from ListHandler during stabilization`() {
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
        } throws ValidationException.builder().build()

        val inputCallbackContext = CallbackContext(stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
            pendingStabilization = true)

        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, inputCallbackContext, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.IN_PROGRESS)
            assertThat(resourceModel).isEqualTo(TEST_DELETE_RESOURCE_MODEL)
            assertThat(callbackContext).isNotNull
            with(callbackContext) {
                assertThat(this?.stabilizationRetriesRemaining).isEqualTo(HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES - 1)
                assertThat(this?.pendingStabilization)
            }
        }
    }

    @Test
    fun `DeleteHandler returns FAILED with NOT FOUND if configured table is not found before delete configured table`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetConfiguredTableRequest::class),
                any<Function<GetConfiguredTableRequest, GetConfiguredTableResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_CONFIGURED_TABLE_ID)
            .build()

        val result = handler.handleRequest(mockProxy, TEST_DELETE_REQUEST, null, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `DeleteHandler returns FAILED with NOT FOUND if configured table is not found during delete configured table`() {
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
                ofType(DeleteConfiguredTableRequest::class),
                any<Function<DeleteConfiguredTableRequest, DeleteConfiguredTableResponse>>()
            )
        } throws ResourceNotFoundException.builder()
            .resourceId(TEST_CONFIGURED_TABLE_ID)
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
