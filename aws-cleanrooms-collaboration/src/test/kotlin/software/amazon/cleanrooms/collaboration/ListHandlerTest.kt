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
import software.amazon.awssdk.services.cleanrooms.model.CollaborationSummary
import software.amazon.awssdk.services.cleanrooms.model.ListCollaborationsRequest
import software.amazon.awssdk.services.cleanrooms.model.ListCollaborationsResponse
import software.amazon.awssdk.services.cleanrooms.model.ValidationException
import software.amazon.cleanrooms.collaboration.typemapper.toResourceModels
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.HandlerErrorCode
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import java.util.function.Function

// These tests are written as per contract
// https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html#resource-type-test-contract-list
class ListHandlerTest {

    /**
     * Type to define test arguments for the readHandler tests.
     */
    data class Args(
        val testName: String,
        val collaborationSummariesFromApi: List<CollaborationSummary>,
        val nextToken: String?,
        val nextTokenFromApi: String?,
        val expectedResourceModels: List<ResourceModel>
    )

    private val mockCleanRoomsClient: CleanRoomsClient = mockk()
    private val mockProxy: AmazonWebServicesClientProxy = mockk()
    private val logger: Logger = LoggerProxy()
    private val handler: ListHandler = ListHandler()

    companion object {
        @JvmStatic
        fun listHandlerSuccessTestData(): List<Args> {
            return listOf(
                Args(
                    testName = "1. No nextToken from request returns emptyList when no collaborations exist.",
                    collaborationSummariesFromApi = emptyList(),
                    nextToken = null,
                    nextTokenFromApi = null,
                    // A list request MUST return an empty array if there are no resources found.
                    expectedResourceModels = emptyList()
                ),
                Args(
                    testName = "2. No nextToken from request returns a list of collaboration resources and null nextToken from ListCollaborations API.",
                    collaborationSummariesFromApi = listOf(
                        TEST_COLLABORATION_SUMMARY_1,
                        TEST_COLLABORATION_SUMMARY_2
                    ),
                    nextToken = null,
                    nextTokenFromApi = null,
                    expectedResourceModels = listOf(
                        TEST_COLLABORATION_SUMMARY_1,
                        TEST_COLLABORATION_SUMMARY_2
                    ).toResourceModels()
                ),
                Args(
                    testName = "3. No nextToken from request returns a list of collaboration resources and non-null nextToken from ListCollaborations API.",
                    collaborationSummariesFromApi = listOf(
                        TEST_COLLABORATION_SUMMARY_1,
                        TEST_COLLABORATION_SUMMARY_2
                    ),
                    nextToken = null,
                    nextTokenFromApi = TEST_NEXT_TOKEN,
                    expectedResourceModels = listOf(
                        TEST_COLLABORATION_SUMMARY_1,
                        TEST_COLLABORATION_SUMMARY_2
                    ).toResourceModels()
                ),
                Args(
                    testName = "4. Non-null nextToken from request returns a list of collaboration resources and null nextToken from ListCollaborations API.",
                    collaborationSummariesFromApi = listOf(
                        TEST_COLLABORATION_SUMMARY_1,
                        TEST_COLLABORATION_SUMMARY_2
                    ),
                    nextToken = TEST_NEXT_TOKEN,
                    nextTokenFromApi = null,
                    expectedResourceModels = listOf(
                        TEST_COLLABORATION_SUMMARY_1,
                        TEST_COLLABORATION_SUMMARY_2
                    ).toResourceModels()
                )
            )
        }
    }

    @BeforeEach
    fun setup() {
        mockkObject(ClientBuilder)
        every { ClientBuilder.getCleanRoomsClient() } returns mockCleanRoomsClient
    }

    @ParameterizedTest
    @MethodSource("listHandlerSuccessTestData")
    fun `ListHandler returns SUCCESS with correct resourceModels and nextToken set`(testArgs: Args) {
        with(testArgs) {
            every {
                mockProxy.injectCredentialsAndInvokeV2(
                    ofType(ListCollaborationsRequest::class),
                    any<Function<ListCollaborationsRequest, ListCollaborationsResponse>>()
                )
            } returns ListCollaborationsResponse.builder()
                .collaborationList(collaborationSummariesFromApi)
                .nextToken(nextTokenFromApi)
                .build()

            val testRequest = ResourceHandlerRequest.builder<ResourceModel>()
                .apply {
                    nextToken?.let { nextToken(it) }
                    awsAccountId(TEST_CREATOR_ACCOUNT_ID)
                }.build()

            val result = handler.handleRequest(mockProxy, testRequest, null, logger)

            with(result) {
                // A list handler in happy path MUST return SUCCESS.
                assertThat(status).isEqualTo(OperationStatus.SUCCESS)
                // A list handler MUST return an array of primary identifiers.
                assertThat(resourceModels).hasSameElementsAs(expectedResourceModels)
                assertThat(resourceModels.map { it.getCollaborationIdFromPrimaryIdentifier() }).hasSameElementsAs(expectedResourceModels.map { it.getCollaborationIdFromPrimaryIdentifier() })
                // A list request MUST support pagination by returning a NextToken.
                assertThat(nextToken).isEqualTo(nextTokenFromApi)
            }
        }
    }

    @Test
    fun `ListHandler throws FAILED with INVALID EXCEPTION when incorrect token is provided`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListCollaborationsRequest::class),
                any<Function<ListCollaborationsRequest, ListCollaborationsResponse>>()
            )
        } throws ValidationException.builder().reason("TEST_VALIDATION_ERROR").build()

        val testRequest = ResourceHandlerRequest.builder<ResourceModel>()
            .build()

        val result = handler.handleRequest(mockProxy, testRequest, null, logger)

        with(result) {
            // A list handler in failure path MUST return FAILED
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.InvalidRequest)
        }
    }
}
