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
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableSummary
import software.amazon.awssdk.services.cleanrooms.model.ListConfiguredTablesRequest
import software.amazon.awssdk.services.cleanrooms.model.ListConfiguredTablesResponse
import software.amazon.awssdk.services.cleanrooms.model.ValidationException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.HandlerErrorCode
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.OperationStatus
import java.util.function.Function

private val TEST_LIST_REQUEST = ResourceHandlerRequest.builder<ResourceModel>()
    .nextToken(TEST_NEXT_TOKEN)
    .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
    .build()

class ListHandlerTest {

    /**
     * Type to define test arguments for the listHandler tests.
     */
    data class Args(
        val testName: String,
        val configuredTableSummariesFromApi: List<ConfiguredTableSummary>,
        val nextToken: String?,
        val nextTokenFromApi: String?,
        val expectedResourceModels: List<ResourceModel>
    )

    companion object {
        @JvmStatic
        fun listHandlerSuccessTestData() = listOf(
            Args(
                testName = "No nextToken input returns empty list when no configured tables found successfully.",
                configuredTableSummariesFromApi = emptyList(),
                nextToken = null,
                nextTokenFromApi = null,
                expectedResourceModels = emptyList()
            ),
            Args(
                testName = "No nextToken input returns configured table id list and no next token successfully.",
                configuredTableSummariesFromApi = listOf(TEST_CONFIGURED_TABLE_SUMMARY_1),
                nextToken = null,
                nextTokenFromApi = null,
                expectedResourceModels = listOf(TEST_RESOURCE_MODEL_SUMMARY_1)
            ),
            Args(
                testName = "nextToken input returns configured table id list and no next token successfully.",
                configuredTableSummariesFromApi = listOf(TEST_CONFIGURED_TABLE_SUMMARY_1),
                nextToken = TEST_NEXT_TOKEN,
                nextTokenFromApi = null,
                expectedResourceModels = listOf(TEST_RESOURCE_MODEL_SUMMARY_1)
            ),
            Args(
                testName = "No nextToken input returns configured table id list and next token successfully.",
                configuredTableSummariesFromApi = listOf(TEST_CONFIGURED_TABLE_SUMMARY_1, TEST_CONFIGURED_TABLE_SUMMARY_2),
                nextToken = null,
                nextTokenFromApi = TEST_EXPECTED_NEXT_TOKEN,
                expectedResourceModels = listOf(TEST_RESOURCE_MODEL_SUMMARY_1, TEST_RESOURCE_MODEL_SUMMARY_2)
            ),
        )
    }

    private val mockCleanRoomsClient: CleanRoomsClient = mockk()

    private val mockProxy: AmazonWebServicesClientProxy = mockk()

    private val logger: Logger = LoggerProxy()

    private val handler: ListHandler = ListHandler()

    @BeforeEach
    fun setup() {
        mockkObject(ClientBuilder)
        every { ClientBuilder.getCleanRoomsClient() } returns mockCleanRoomsClient
    }

    @ParameterizedTest
    @MethodSource("listHandlerSuccessTestData")
    fun `ListHandler returns SUCCESS with correct resourceModels and nextToken set`(testArgs: Args) {
        // Setup mock behavior.
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListConfiguredTablesRequest::class),
                any<Function<ListConfiguredTablesRequest, ListConfiguredTablesResponse>>()
            )
        } returns ListConfiguredTablesResponse.builder()
            .configuredTableSummaries(testArgs.configuredTableSummariesFromApi)
            .nextToken(testArgs.nextTokenFromApi)
            .build()

        val testRequest = ResourceHandlerRequest.builder<ResourceModel>()
            .nextToken(testArgs.nextToken)
            .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
            .build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, testRequest, null, logger)

        assertThat(result).isNotNull
        with(result) {
            // Checking the status is SUCCESS
            assertThat(status).isEqualTo(OperationStatus.SUCCESS)
            assertThat(nextToken).isEqualTo(testArgs.nextTokenFromApi)
            with(resourceModels) {
                assertThat(this).isEqualTo(testArgs.expectedResourceModels)
            }
        }
    }

    @Test
    fun `ListHandler returns FAILED with INVALID REQUEST if list configured table returns validation exception `() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListConfiguredTablesRequest::class),
                any<Function<ListConfiguredTablesRequest, ListConfiguredTablesResponse>>()
            )
        } throws ValidationException.builder()
            .build()

        val result = handler.handleRequest(mockProxy, TEST_LIST_REQUEST, null, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.InvalidRequest)
        }
    }
}
