package software.amazon.cleanrooms.configuredtableassociation

import software.amazon.cleanrooms.configuredtableassociation.typemapper.toResourceModel
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ResourceHandlerRequest

class ReadHandler : BaseHandler<CallbackContext?>() {
    override fun handleRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel>,
        callbackContext: CallbackContext?,
        logger: Logger
    ): ProgressEvent<ResourceModel, CallbackContext?> {
        logger.log("Inside READ handler for ConfiguredTableAssociation resource. Request AWS AccountId:${request.awsAccountId}")
        val resourceModel = request.desiredResourceState
        val cleanRoomsClient = ClientBuilder.getCleanRoomsClient()

        return try {
            with(resourceModel) {
                logger.log("ConfiguredAssociationTableId from resourceModel: $configuredTableAssociationIdentifier")

                val configuredTableAssociation = getConfiguredTableAssociation(
                    configuredTableAssociationIdentifier,
                    membershipIdentifier,
                    proxy,
                    cleanRoomsClient
                )

                val tags = listTagsForResource(
                    configuredTableAssociation.arn(),
                    proxy,
                    cleanRoomsClient
                ).map { Tag(it.key, it.value) }

                logger.log("[SUCCESS] ReadHandler succeeded. Returning resourcemodel with arn: ${configuredTableAssociation.arn()}")
                ProgressEvent.defaultSuccessHandler(configuredTableAssociation.toResourceModel(tags))
            }
        } catch (e: Exception) {
            val error = e.toCfnException()
            logger.log("[EXCEPTION] ReadHandler failed for account:${request.awsAccountId}. Original error:$e, Mapped error:$error. Full stack trace:${e.stackTraceToString()}")
            ProgressEvent.defaultFailureHandler(error, error.errorCode)
        }
    }
}
