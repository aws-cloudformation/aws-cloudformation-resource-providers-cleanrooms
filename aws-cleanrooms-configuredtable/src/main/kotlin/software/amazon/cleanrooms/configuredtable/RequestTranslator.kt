package software.amazon.cleanrooms.configuredtable

import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.awssdk.services.cleanrooms.model.AggregateColumn
import software.amazon.awssdk.services.cleanrooms.model.AggregationConstraint
import software.amazon.awssdk.services.cleanrooms.model.AnalysisRuleAggregation
import software.amazon.awssdk.services.cleanrooms.model.AnalysisRuleList
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTable
import software.amazon.awssdk.services.cleanrooms.model.DeleteConfiguredTableRequest
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAnalysisRule
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAnalysisRulePolicyV1
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAnalysisRulePolicy as SdkConfiguredTableAnalysisRulePolicy
import software.amazon.awssdk.services.cleanrooms.model.CreateConfiguredTableAnalysisRuleRequest
import software.amazon.awssdk.services.cleanrooms.model.CreateConfiguredTableRequest
import software.amazon.awssdk.services.cleanrooms.model.DeleteConfiguredTableAnalysisRuleRequest
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableAnalysisRuleRequest
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableAnalysisRuleResponse
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableRequest
import software.amazon.awssdk.services.cleanrooms.model.GlueTableReference
import software.amazon.awssdk.services.cleanrooms.model.ListConfiguredTablesRequest
import software.amazon.awssdk.services.cleanrooms.model.ListConfiguredTablesResponse
import software.amazon.awssdk.services.cleanrooms.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.UpdateConfiguredTableRequest
import software.amazon.awssdk.services.cleanrooms.model.UpdateConfiguredTableResponse
import software.amazon.awssdk.services.cleanrooms.model.ScalarFunctions
import software.amazon.awssdk.services.cleanrooms.model.TableReference
import software.amazon.awssdk.services.cleanrooms.model.TagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.UntagResourceRequest
import software.amazon.awssdk.services.cleanrooms.model.UpdateConfiguredTableAnalysisRuleRequest
import software.amazon.awssdk.services.cloudformation.model.Tag
import software.amazon.cleanrooms.configuredtable.typemapper.toCfnException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger

/**
 * Method to parse configuredTableIdentifier from primaryIdentifier if it exists returns null otherwise
 */
fun ResourceModel.getResourcePrimaryIdentifier(): String {
    return primaryIdentifier.getString(ResourceModel.IDENTIFIER_KEY_CONFIGUREDTABLEIDENTIFIER)
        ?: error(
            "ConfiguredTableIdentifier key not found. " +
                "Check if the 'configuredTableIdentifier' is defined in the resource template before reading it."
        )
}

/**
 * Build Request and Makes service call for CreateConfiguredTable
 */
fun createConfiguredTable(
    resourceModel: ResourceModel,
    desiredResourceTags: Map<String, String>?,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
): ConfiguredTable {
    with(resourceModel) {
        val createConfiguredTableRequest = CreateConfiguredTableRequest.builder()
            .name(name)
            .description(description)
            .allowedColumns(allowedColumns)
            .analysisMethod(analysisMethod)
            .tableReference(
                TableReference.builder()
                    .glue(
                        GlueTableReference.builder()
                            .databaseName(tableReference.glue.databaseName)
                            .tableName(tableReference.glue.tableName)
                            .build()
                    ).build()
            )
            .apply {
                desiredResourceTags?.let { tags(it) }
            }.build()
        val createConfiguredTableResponse = proxy.injectCredentialsAndInvokeV2(
            createConfiguredTableRequest,
            cleanRoomsClient::createConfiguredTable
        )

        return createConfiguredTableResponse.configuredTable()
    }
}

/**
 * Build Request and Makes service call for CreateConfiguredTableAnalysisRule
 */
fun createConfiguredTableAnalysisRule(
    analysisRule: AnalysisRule,
    configuredTableIdentifier: String,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
) {
    val createConfiguredTableAnalysisRuleRequest = CreateConfiguredTableAnalysisRuleRequest.builder()
        .configuredTableIdentifier(configuredTableIdentifier)
        .analysisRuleType(analysisRule.type)
        .analysisRulePolicy(analysisRule.toSdkModel())
        .build()

    proxy.injectCredentialsAndInvokeV2(
        createConfiguredTableAnalysisRuleRequest,
        cleanRoomsClient::createConfiguredTableAnalysisRule
    )
}


/**
 * Build Request and Makes service call for DeleteConfiguredTable
 */
fun deleteConfiguredTable(
    configuredTableIdentifier: String,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
) {
    val deleteConfiguredTableRequest = DeleteConfiguredTableRequest.builder()
        .configuredTableIdentifier(configuredTableIdentifier)
        .build()

    proxy.injectCredentialsAndInvokeV2(
        deleteConfiguredTableRequest,
        cleanRoomsClient::deleteConfiguredTable
    )
}

/**
 * Build Request and Makes service call for DeleteConfiguredTableAnalysisRule
 */
fun deleteConfiguredTableAnalysisRule(
    analysisRuleType: String,
    configuredTableIdentifier: String,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
) {
    val deleteConfiguredTableAnalysisRuleRequest: DeleteConfiguredTableAnalysisRuleRequest = DeleteConfiguredTableAnalysisRuleRequest.builder()
        .configuredTableIdentifier(configuredTableIdentifier)
        .analysisRuleType(analysisRuleType)
        .build()

    proxy.injectCredentialsAndInvokeV2(
        deleteConfiguredTableAnalysisRuleRequest,
        cleanRoomsClient::deleteConfiguredTableAnalysisRule
    )
}

/**
 * Build Request and Makes service call for GetConfiguredTable
 */
fun getConfiguredTable(
    configuredTableIdentifier: String,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
): ConfiguredTable {
    val getConfiguredTableRequest: GetConfiguredTableRequest = GetConfiguredTableRequest.builder()
        .configuredTableIdentifier(configuredTableIdentifier)
        .build()

    val getConfiguredTableResponse = proxy.injectCredentialsAndInvokeV2(
        getConfiguredTableRequest,
        cleanRoomsClient::getConfiguredTable
    )

    return getConfiguredTableResponse.configuredTable()
}

/**
 * Build Request and Makes service call for GetConfiguredTableAnalysisRule
 */
fun getConfiguredTableAnalysisRule(
    analysisRuleType: String,
    configuredTableIdentifier: String,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
): GetConfiguredTableAnalysisRuleResponse {

    val getConfiguredTableAnalysisRuleRequest: GetConfiguredTableAnalysisRuleRequest = GetConfiguredTableAnalysisRuleRequest.builder()
        .configuredTableIdentifier(configuredTableIdentifier)
        .analysisRuleType(analysisRuleType)
        .build()

    return proxy.injectCredentialsAndInvokeV2(
        getConfiguredTableAnalysisRuleRequest,
        cleanRoomsClient::getConfiguredTableAnalysisRule
    )
}

/**
 * Build Request and Makes service call for UpdateConfiguredTable
 */
fun updateConfiguredTable(
    resourceModel: ResourceModel,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
) {
    with(resourceModel) {
        val updateConfiguredTableRequest: UpdateConfiguredTableRequest = UpdateConfiguredTableRequest.builder()
            .configuredTableIdentifier(configuredTableIdentifier)
            .name(name)
            .description(description)
            .build()

        proxy.injectCredentialsAndInvokeV2(
            updateConfiguredTableRequest,
            cleanRoomsClient::updateConfiguredTable
        )
    }
}

/**
 * Build Request and Makes service call for UpdateConfiguredTableAnalysisRule
 */
fun updateConfiguredTableAnalysisRule(
    analysisRule: AnalysisRule,
    configuredTableIdentifier: String,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
): ConfiguredTableAnalysisRule {

    with(analysisRule) {
        val updateConfiguredTableAnalysisRuleRequest: UpdateConfiguredTableAnalysisRuleRequest = UpdateConfiguredTableAnalysisRuleRequest.builder()
            .configuredTableIdentifier(configuredTableIdentifier)
            .analysisRuleType(type)
            .analysisRulePolicy(this.toSdkModel())
            .build()

        val updateConfiguredTableAnalysisRuleResponse = proxy.injectCredentialsAndInvokeV2(
            updateConfiguredTableAnalysisRuleRequest,
            cleanRoomsClient::updateConfiguredTableAnalysisRule
        )

        return updateConfiguredTableAnalysisRuleResponse.analysisRule()
    }
}

/**
 * Build Request and Makes service call for ListConfiguredTables
 */
fun listConfiguredTables(
    nextToken: String?,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
): ListConfiguredTablesResponse {
    val listConfiguredTablesRequest = ListConfiguredTablesRequest.builder()
        .apply {
            nextToken?.let { nextToken(it) }
        }.build()

    return proxy.injectCredentialsAndInvokeV2(
        listConfiguredTablesRequest,
        cleanRoomsClient::listConfiguredTables
    )
}

/**
 * Build Request and Makes service call for ListTagsForResource
 */
fun listTagsForResource(
    resourceArn: String,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
): Map<String, String> {
    val listTagsRequest = ListTagsForResourceRequest.builder()
        .resourceArn(resourceArn)
        .build()
    return proxy.injectCredentialsAndInvokeV2(listTagsRequest, cleanRoomsClient::listTagsForResource).tags().filter {
        !it.key.startsWith("aws:")
    }
}

/**
 * Build Request and Makes service call for Add Tags
 */
fun tagResource(
    resourceArn: String,
    tagsToAdd: Map<String, String>,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
) {
    val tagResourceRequest: TagResourceRequest = TagResourceRequest.builder()
        .resourceArn(resourceArn)
        .tags(tagsToAdd)
        .build()

    proxy.injectCredentialsAndInvokeV2(tagResourceRequest, cleanRoomsClient::tagResource)
}

/**
 * Build Request and Makes service call for Remove Tags
 */
fun untagResource(
    resourceArn: String,
    tagKeysToRemove: Set<String>,
    proxy: AmazonWebServicesClientProxy,
    cleanRoomsClient: CleanRoomsClient
) {
    val untagResourceRequest: UntagResourceRequest = UntagResourceRequest.builder()
        .resourceArn(resourceArn)
        .tagKeys(tagKeysToRemove)
        .build()

    proxy.injectCredentialsAndInvokeV2(untagResourceRequest, cleanRoomsClient::untagResource)
}

/**
 * Method to convert ResourceModel AnalysisRule to CleanRooms Sdk ConfiguredTableAnalysisRulePolicy
 */
private fun AnalysisRule.toSdkModel(): SdkConfiguredTableAnalysisRulePolicy {
    val policyV1 = policy.v1
    return when (type) {
        ConfiguredTableAnalysisRulePolicyV1.Type.AGGREGATION.name ->
            SdkConfiguredTableAnalysisRulePolicy.builder()
                .v1(
                    ConfiguredTableAnalysisRulePolicyV1.builder()
                        .aggregation(
                            AnalysisRuleAggregation.builder()
                                .joinColumns(policyV1.aggregation.joinColumns)
                                .joinRequired(policyV1.aggregation.joinRequired)
                                .dimensionColumns(policyV1.aggregation.dimensionColumns)
                                .aggregateColumns(
                                    policyV1.aggregation.aggregateColumns.map { aggColumn ->
                                        AggregateColumn.builder()
                                            .columnNames(aggColumn.columnNames)
                                            .function(aggColumn.function)
                                            .build()
                                    }
                                )
                                .outputConstraints(
                                    policyV1.aggregation.outputConstraints.map { aggConstraint ->
                                        AggregationConstraint.builder()
                                            .columnName(aggConstraint.columnName)
                                            .minimum(aggConstraint.minimum.toInt())
                                            .type(aggConstraint.type)
                                            .build()
                                    }
                                )
                                .scalarFunctions(
                                    policyV1.aggregation.scalarFunctions.map { scalarFunctionString ->
                                        ScalarFunctions.fromValue(scalarFunctionString)
                                    }
                                )
                                .build()
                        )
                        .build()
                )
                .build()
        ConfiguredTableAnalysisRulePolicyV1.Type.LIST.name ->
            SdkConfiguredTableAnalysisRulePolicy.builder()
                .v1(
                    ConfiguredTableAnalysisRulePolicyV1.builder()
                        .list(
                            AnalysisRuleList.builder()
                                .joinColumns(policyV1.list.joinColumns)
                                .listColumns(policyV1.list.listColumns)
                                .build()
                        )
                        .build()
                )
                .build()
        else -> throw Exception()
    }
}
