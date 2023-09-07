package software.amazon.cleanrooms.analysistemplate

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.awssdk.services.cleanrooms.model.ListAnalysisTemplatesRequest
import software.amazon.awssdk.services.cleanrooms.model.ListAnalysisTemplatesResponse
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_ANALYSIS_TEMPLATE_SUMMARY_1
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_ANALYSIS_TEMPLATE_SUMMARY_2
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_CREATOR_ACCOUNT_ID
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_AT_ID
import software.amazon.cleanrooms.analysistemplate.TestData.TEST_MEMBERSHIP_ID
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import java.util.function.Function

class HandlerCommonTest {

    private val mockCleanRoomsClient: CleanRoomsClient = mockk()

    private val mockProxy: AmazonWebServicesClientProxy = mockk()

    private val logger: Logger = LoggerProxy()

    private val sdkNextToken: String = "NEXT_TOKEN"

    private val inputResourceModel = ResourceModel.builder()
        .analysisTemplateIdentifier(TEST_AT_ID)
        .membershipIdentifier(TEST_MEMBERSHIP_ID)
        .build()

    private val request = ResourceHandlerRequest.builder<ResourceModel>()
        .desiredResourceState(inputResourceModel)
        .awsAccountId(TEST_CREATOR_ACCOUNT_ID)
        .build()

    @BeforeEach
    fun setup() {
        mockkObject(ClientBuilder)
        every { ClientBuilder.getCleanRoomsClient() } returns mockCleanRoomsClient
    }

    @Test
    fun `verifyListResourceFound return FALSE when resource not found in list`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListAnalysisTemplatesRequest::class),
                any<Function<ListAnalysisTemplatesRequest, ListAnalysisTemplatesResponse>>()
            )
        } returns ListAnalysisTemplatesResponse.builder()
            .build()

        val inputCallbackContext = CallbackContext(stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
            pendingStabilization = true)

        val resourceFound = HandlerCommon.verifyListResourceFound(mockProxy, request, inputCallbackContext, logger)
        assertThat(resourceFound).isFalse
    }

    @Test
    fun `verifyListResourceFound return TRUE when resource found in list`() {
        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListAnalysisTemplatesRequest::class),
                any<Function<ListAnalysisTemplatesRequest, ListAnalysisTemplatesResponse>>()
            )
        } returns ListAnalysisTemplatesResponse.builder()
            .analysisTemplateSummaries(TEST_ANALYSIS_TEMPLATE_SUMMARY_1)
            .build()

        val inputCallbackContext = CallbackContext(stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
            pendingStabilization = true)

        val resourceFound = HandlerCommon.verifyListResourceFound(mockProxy, request, inputCallbackContext, logger)
        assertThat(resourceFound)
    }

    @Test
    fun `verifyListResourceFound return TRUE when resource found in list with multiple pages`() {
        val firstListResponse = ListAnalysisTemplatesResponse.builder()
            .analysisTemplateSummaries(TEST_ANALYSIS_TEMPLATE_SUMMARY_1)
            .nextToken(sdkNextToken)
            .build()

        val secondListResponse = ListAnalysisTemplatesResponse.builder()
            .analysisTemplateSummaries(listOf(TEST_ANALYSIS_TEMPLATE_SUMMARY_2))
            .build()

        every {
            mockProxy.injectCredentialsAndInvokeV2(
                ofType(ListAnalysisTemplatesRequest::class),
                any<Function<ListAnalysisTemplatesRequest, ListAnalysisTemplatesResponse>>()
            )
        } returns firstListResponse andThen secondListResponse

        val inputCallbackContext = CallbackContext(stabilizationRetriesRemaining = HandlerCommon.NUMBER_OF_STATE_POLL_RETRIES,
            pendingStabilization = true)

        val resourceFound = HandlerCommon.verifyListResourceFound(mockProxy, request, inputCallbackContext, logger)
        assertThat(resourceFound)
    }
}
