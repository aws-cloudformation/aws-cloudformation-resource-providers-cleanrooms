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
import software.amazon.awssdk.services.cleanrooms.model.AccessDeniedException
import software.amazon.awssdk.services.cleanrooms.model.Collaboration
import software.amazon.awssdk.services.cleanrooms.model.GetCollaborationRequest
import software.amazon.awssdk.services.cleanrooms.model.GetCollaborationResponse
import software.amazon.awssdk.services.cleanrooms.model.ListMembersRequest
import software.amazon.awssdk.services.cleanrooms.model.ListMembersResponse
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceResponse
import software.amazon.awssdk.services.cleanrooms.model.MemberSummary
import software.amazon.awssdk.services.cleanrooms.model.ResourceNotFoundException
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

private val TEST_READ_REQUEST = ResourceHandlerRequest.builder<ResourceModel>()
    .desiredResourceState(
        ResourceModel.builder().collaborationIdentifier(TEST_COLLAB_ID).build()
    )
    .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
    .build()

// These tests are written as per contract:
// https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html#resource-type-test-contract-read
class ReadHandlerTest {

    /**
     * Type to define test arguments for the readHandler tests.
     */
    data class Args(
        val testName: String,
        val collaborationFromApi: Collaboration,
        val membersFromApi: List<MemberSummary>,
        val tagsFromApi: Map<String, String>,
        val expectedResourceModel: ResourceModel
    )

    companion object {
        @JvmStatic
        fun readHandlerSuccessTestData() = listOf(
            // 1. collaboration with only required args.
            Args(
                testName = "Collaboration with all args reads successfully.",
                collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                membersFromApi = listOf(TEST_MEMBER_SUMMARY, TEST_CREATOR_MEMBER_SUMMARY),
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS.toResourceModel(
                    TEST_CREATOR_MEMBER_SUMMARY.toMemberSpecification(),
                    listOf(TEST_MEMBER_SUMMARY).toMemberSpecification()
                )
            ),
            // 2. collaboration without tags, and required fields only.
            Args(
                testName = "Collaboration without tags reads successfully.",
                collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                membersFromApi = listOf(TEST_MEMBER_SUMMARY, TEST_CREATOR_MEMBER_SUMMARY),
                // No tags
                tagsFromApi = TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS.toResourceModel(
                    TEST_CREATOR_MEMBER_SUMMARY.toMemberSpecification(),
                    listOf(TEST_MEMBER_SUMMARY).toMemberSpecification()
                )
            ),
            // 3. collaboration without dataEncryptionMetadata.
            Args(
                testName = "Collaboration without dataEncryptionMetadata reads successfully.",
                collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS,
                membersFromApi = listOf(TEST_MEMBER_SUMMARY, TEST_CREATOR_MEMBER_SUMMARY),
                tagsFromApi = TEST_COLLABORATION_TAGS + TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS.toResourceModel(
                    TEST_CREATOR_MEMBER_SUMMARY.toMemberSpecification(),
                    listOf(TEST_MEMBER_SUMMARY).toMemberSpecification(),
                    TEST_COLLABORATION_TAGS.toResourceModelTags()
                )
            ),
            // 4. Collaboration with no members from list Members call.
            Args(
                testName = "Collaboration with no members from listMembers call.",
                collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_ALL_ARGS,
                membersFromApi = listOf(TEST_CREATOR_MEMBER_SUMMARY),
                tagsFromApi = TEST_COLLABORATION_TAGS + TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_COLLABORATION_RESPONSE_WITH_ALL_ARGS.toResourceModel(
                    TEST_CREATOR_MEMBER_SUMMARY.toMemberSpecification(),
                    tags = TEST_COLLABORATION_TAGS.toResourceModelTags()
                )
            ),
            // 5. Collaboration with all attributes.
            Args(
                testName = "Collaboration with all attributes.",
                collaborationFromApi = TEST_COLLABORATION_RESPONSE_WITH_ALL_ARGS,
                membersFromApi = listOf(TEST_MEMBER_SUMMARY, TEST_CREATOR_MEMBER_SUMMARY),
                tagsFromApi = TEST_COLLABORATION_TAGS + TEST_SYSTEM_TAGS,
                expectedResourceModel = TEST_COLLABORATION_RESPONSE_WITH_ALL_ARGS.toResourceModel(
                    TEST_CREATOR_MEMBER_SUMMARY.toMemberSpecification(),
                    listOf(TEST_MEMBER_SUMMARY).toMemberSpecification(),
                    TEST_COLLABORATION_TAGS.toResourceModelTags()
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
                ofType(ListMembersRequest::class),
                any<Function<ListMembersRequest, ListMembersResponse>>()
            )
        } returns ListMembersResponse.builder().memberSummaries(testArgs.membersFromApi).build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } returns GetCollaborationResponse.builder().collaboration(testArgs.collaborationFromApi).build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListTagsForResourceRequest::class),
                any<Function<ListTagsForResourceRequest, ListTagsForResourceResponse>>()
            )
        } returns ListTagsForResourceResponse.builder().tags(testArgs.tagsFromApi).build()

        // Actual call to the handler
        val result = handler.handleRequest(mockProxy, TEST_READ_REQUEST, null, logger)

        assertThat(result).isNotNull
        with(result) {
            // Checking the status is SUCCESS
            assertThat(status).isEqualTo(OperationStatus.SUCCESS)
            with(resourceModel) {
                // A read handler MUST return a model representation that conforms to the shape of the resource schema.
                assertThat(this).isEqualTo(testArgs.expectedResourceModel)
                // The model MUST contain all properties that have values, including any properties that have default values
                // and any readOnlyProperties as defined in the resource schema.
                assertThat(getCollaborationIdFromPrimaryIdentifier()).isEqualTo(TEST_COLLAB_ID)
                assertThat(arn).isEqualTo(TEST_COLLAB_ARN)
            }
        }
    }

    @Test
    fun `ReadHandler returns FAILED if creatorMember is not in listMembers response`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListMembersRequest::class),
                any<Function<ListMembersRequest, ListMembersResponse>>()
            )
        } returns ListMembersResponse.builder().memberSummaries(listOf(TEST_MEMBER_SUMMARY)).build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } returns GetCollaborationResponse.builder().collaboration(TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS).build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListTagsForResourceRequest::class),
                any<Function<ListTagsForResourceRequest, ListTagsForResourceResponse>>()
            )
        } returns ListTagsForResourceResponse.builder().tags(TEST_SYSTEM_TAGS).build()

        val result = handler.handleRequest(mockProxy, TEST_READ_REQUEST, null, logger)
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
        }
    }

    @Test
    fun `ReadHandler returns FAILED with NOT FOUND if tags for a collaboration are not found`() {
        // Setup mock to throw exception
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListMembersRequest::class),
                any<Function<ListMembersRequest, ListMembersResponse>>()
            )
        } returns ListMembersResponse.builder().memberSummaries(listOf(TEST_MEMBER_SUMMARY, TEST_CREATOR_MEMBER_SUMMARY)).build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } returns GetCollaborationResponse.builder().collaboration(TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS).build()

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
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `ReadHandler returns FAILED with NOT FOUND if members of a collaboration are not found`() {
        // Setup mocks to throw exception
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } returns GetCollaborationResponse.builder().collaboration(TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS).build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListMembersRequest::class),
                any<Function<ListMembersRequest, ListMembersResponse>>()
            )
            // OUR APIs throw AccessDeniedException instead of ResourceNotFound exception for List APIs.
        } throws AccessDeniedException.builder().reason("Members not found").build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListTagsForResourceRequest::class),
                any<Function<ListTagsForResourceRequest, ListTagsForResourceResponse>>()
            )
        } returns ListTagsForResourceResponse.builder().tags(TEST_COLLABORATION_TAGS + TEST_SYSTEM_TAGS).build()

        // Actual call to the handler
        val result = ReadHandler().handleRequest(mockProxy, TEST_READ_REQUEST, null, logger)

        // A read handler MUST return FAILED with a NotFound error code if the resource doesn't exist.
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }

    @Test
    fun `ReadHandler returns FAILED with NOT FOUND if collaboration is not found`() {
        // Setup mock to throw exception
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(GetCollaborationRequest::class),
                any<Function<GetCollaborationRequest, GetCollaborationResponse>>()
            )
        } throws ResourceNotFoundException.builder().resourceId(TEST_COLLAB_ID).build()

        // Actual call to the handler
        val result = ReadHandler().handleRequest(mockProxy, TEST_READ_REQUEST, null, logger)

        // A read handler MUST return FAILED with a NotFound error code if the resource doesn't exist.
        assertThat(result).isNotNull
        with(result) {
            assertThat(status).isEqualTo(OperationStatus.FAILED)
            assertThat(errorCode).isEqualTo(HandlerErrorCode.NotFound)
        }
    }
}
