package software.amazon.cleanrooms.analysistemplate

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.ResourceHandlerRequest

/**
 * Singleton class for common methods used by CRUD handlers
 */
object HandlerCommon {
    const val CALLBACK_DELAY_IN_SECONDS = 1
    const val NUMBER_OF_STATE_POLL_RETRIES = 60

    /**
     * Method to verify if primaryIdentifier is returned from ListHandler
     */
    fun verifyListResourceFound(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel>,
        callbackContext: CallbackContext,
        logger: Logger
    ): Boolean {
        val primaryIdentifier = request.desiredResourceState.getResourcePrimaryIdentifier()

        logger.log("Verify resource: $primaryIdentifier exists in ListHandler response")

        val resourceModelList = listAllResources(proxy, request, callbackContext, logger)
        logger.log("ListHandler returned ${resourceModelList.size} resources")
        val primaryIdentifierResourceModel = resourceModelList.find { it.analysisTemplateIdentifier == primaryIdentifier }
        logger.log("Resource $primaryIdentifier found in ListHandler response: ${primaryIdentifierResourceModel != null}")
        return primaryIdentifierResourceModel != null
    }

    /**
     * Method to accumulate all results
     */
    private fun listAllResources(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel>,
        callbackContext: CallbackContext,
        logger: Logger
    ): List<ResourceModel> = sequence {
        var token: String? = null
        do {
            val updatedRequest = request.apply {
                nextToken = token
            }
            val listResult = ListHandler().handleRequest(proxy, updatedRequest, callbackContext, logger)
            yieldAll(listResult.resourceModels)
            token = listResult.nextToken
        } while (token != null)
    }.toList()
}
