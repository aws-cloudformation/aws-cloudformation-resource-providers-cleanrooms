package software.amazon.cleanrooms.membership

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.awssdk.services.cleanrooms.model.AccessDeniedException
import software.amazon.awssdk.services.cleanrooms.model.GetMembershipRequest
import software.amazon.awssdk.services.cleanrooms.model.GetMembershipResponse
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.Membership
import software.amazon.awssdk.services.cleanrooms.model.ResourceNotFoundException
import software.amazon.cleanrooms.membership.typemapper.toResourceModel
import software.amazon.cleanrooms.membership.typemapper.toResourceModelTags
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import java.util.function.Function

private val TEST_READ_REQUEST = ResourceHandlerRequest.builder<ResourceModel>()
    .desiredResourceState(
        ResourceModel.builder()
            .arn(TEST_MEMBERSHIP_ARN)
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .build()
    )
    .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
    .build()

private val INVALID_TEST_READ_REQUEST = ResourceHandlerRequest.builder<ResourceModel>()
    .desiredResourceState(
        ResourceModel.builder()
            .arn(TEST_MEMBERSHIP_ARN)
            .build()
    )
    .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
    .build()
class ReadHandlerTest {

    /**
     * Type to define test arguments for the readHandler tests.
     */
    data class Args(
        val testName: String,
        val membershipFromApi: Membership,
        val tagsFromApi: Map<String, String>,
        val expectedResourceModel: ResourceModel
    )

    companion object {
        @JvmStatic
        fun readHandlerSuccessTestData() = listOf(
            // 1. membership with only required args.
            Args(
                testName = "Membership with all args but no tags reads successfully.",
                membershipFromApi = TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS,
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS.toResourceModel()
                ),
            // 2. Membership with all attributes.
            Args(
                testName = "Membership with all attributes.",
                membershipFromApi = TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS,
                tagsFromApi = TEST_MEMBERSHIP_TAGS + TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS.toResourceModel(
                    TEST_MEMBERSHIP_TAGS.toResourceModelTags()
                )
            )
        )
    }

    private val mockClient: CleanRoomsClient = mockk()
    private val mockProxy: AmazonWebServicesClientProxy = mockk()
    private val logger: Logger = LoggerProxy()
    private val handler: ReadHandler = ReadHandler()

    @BeforeEach
    fun setup() {
        mockkObject(ClientBuilder)
        every { ClientBuilder.getCleanRoomsClient() } returns mockClient
    }

    @ParameterizedTest
    @MethodSource("readHandlerSuccessTestData")
    fun `ReadHandler returns SUCCESS with correct resourceModel and primaryIdentifier set`(testArgs: Args) {
        // Setup mock behavior.
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetMembershipRequest::class),
                any<Function<GetMembershipRequest, GetMembershipResponse>>()
            )
        } returns GetMembershipResponse.builder().membership(testArgs.membershipFromApi).build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListTagsForResourceRequest::class),
                any<Function<ListTagsForResourceRequest, ListTagsForResourceResponse>>()
            )
        } returns ListTagsForResourceResponse.builder().tags(testArgs.tagsFromApi).build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, TEST_READ_REQUEST, null, logger)

        Assertions.assertThat(result).isNotNull
        with(result) {
            // Checking the status is SUCCESS
            Assertions.assertThat(status).isEqualTo(OperationStatus.SUCCESS)
            with(resourceModel) {
                // A read handler MUST return a model representation that conforms to the shape of the resource schema.
                Assertions.assertThat(this).isEqualTo(testArgs.expectedResourceModel)
                // The model MUST contain all properties that have values, including any properties that have default values
                // and any readOnlyProperties as defined in the resource schema.
                Assertions.assertThat(getMembershipIdFromPrimaryIdentifier()).isEqualTo(TEST_MEMBERSHIP_ID)
                Assertions.assertThat(arn).isEqualTo(TEST_MEMBERSHIP_ARN)
            }
        }
    }

    @Test
    fun `ReadHandler returns FAILED with NOT FOUND if tags for a membership are not found`() {
        // Setup mock to throw exception

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetMembershipRequest::class),
                any<Function<GetMembershipRequest, GetMembershipResponse>>()
            )
        } returns GetMembershipResponse.builder().membership(TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS).build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListTagsForResourceRequest::class),
                any<Function<ListTagsForResourceRequest, ListTagsForResourceResponse>>()
            )
            // OUR APIs throw AccessDeniedException instead of ResourceNotFound exception for List APIs.
        } throws AccessDeniedException.builder().reason("Tags not found").build()

        // Actual call to the handler
        val result = ReadHandler().handleRequest(mockProxy, TEST_READ_REQUEST, null, logger)

        // A read handler MUST return FAILED with a NotFound error code if the resource doesn't exist.
        Assertions.assertThat(result).isNotNull
        with(result) {
            Assertions.assertThat(status).isEqualTo(software.amazon.cloudformation.proxy.OperationStatus.FAILED)
            Assertions.assertThat(errorCode).isEqualTo(software.amazon.cloudformation.proxy.HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `ReadHandler returns FAILED with NOT FOUND if membership is not found`() {
        // Setup mock to throw exception
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetMembershipRequest::class),
                any<Function<GetMembershipRequest, GetMembershipResponse>>()
            )
        } throws ResourceNotFoundException.builder().resourceId(TEST_MEMBERSHIP_ID).build()

        // Actual call to the handler
        val result = ReadHandler().handleRequest(mockProxy, TEST_READ_REQUEST, null, logger)

        // A read handler MUST return FAILED with a NotFound error code if the resource doesn't exist.
        Assertions.assertThat(result).isNotNull
        with(result) {
            Assertions.assertThat(status).isEqualTo(software.amazon.cloudformation.proxy.OperationStatus.FAILED)
            Assertions.assertThat(errorCode).isEqualTo(software.amazon.cloudformation.proxy.HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `ReadHandler returns FAILED with NOT FOUND if primaryIdentifier is not found`() {
        // Setup mock to throw exception
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetMembershipRequest::class),
                any<Function<GetMembershipRequest, GetMembershipResponse>>()
            )
        } throws ResourceNotFoundException.builder().resourceId(TEST_MEMBERSHIP_ID).build()

        // Actual call to the handler
        val result = ReadHandler().handleRequest(mockProxy, INVALID_TEST_READ_REQUEST, null, logger)

        // A read handler MUST return FAILED with a NotFound error code if the resource doesn't exist.
        Assertions.assertThat(result).isNotNull
        with(result) {
            Assertions.assertThat(status).isEqualTo(software.amazon.cloudformation.proxy.OperationStatus.FAILED)
            Assertions.assertThat(errorCode).isEqualTo(software.amazon.cloudformation.proxy.HandlerErrorCode.GeneralServiceException)
        }
    }
}
