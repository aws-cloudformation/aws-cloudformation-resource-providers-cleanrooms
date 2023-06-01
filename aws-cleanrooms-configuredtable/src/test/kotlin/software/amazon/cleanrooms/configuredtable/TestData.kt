package software.amazon.cleanrooms.configuredtable

import software.amazon.awssdk.services.cleanrooms.model.AggregateColumn as SdkAggregateColumn
import software.amazon.awssdk.services.cleanrooms.model.AggregationConstraint as SdkAggregationConstraint
import software.amazon.awssdk.services.cleanrooms.model.AnalysisRuleAggregation as SdkAnalysisRuleAggregation
import software.amazon.awssdk.services.cleanrooms.model.AnalysisRuleList as SdkAnalysisRuleList
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTable
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAnalysisRule
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAnalysisRuleType
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableSummary
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAnalysisRulePolicy as SdkConfiguredTableAnalysisRulePolicy
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAnalysisRulePolicyV1 as SdkConfiguredTableAnalysisRulePolicyV1
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableResponse
import software.amazon.awssdk.services.cleanrooms.model.ScalarFunctions
import software.amazon.awssdk.services.cleanrooms.model.GlueTableReference as SdkGlueTableReference
import software.amazon.awssdk.services.cleanrooms.model.TableReference as SdkTableReference
import java.time.Instant


const val TEST_NEXT_TOKEN = "TestNextToken"
const val TEST_EXPECTED_NEXT_TOKEN = "TestExpectedNextToken"
const val TEST_CREATOR_ACCOUNT_ID = "TestCreatorAccountId"
const val TEST_CONFIGURED_TABLE_ARN = "TestConfiguredTableArn"
const val TEST_CONFIGURED_TABLE_ARN_2 = "TestConfiguredTableArn2"
const val TEST_CONFIGURED_TABLE_NAME = "TestConfiguredTableName"
const val TEST_CONFIGURED_TABLE_ID = "TestConfiguredTableId"
const val TEST_CONFIGURED_TABLE_ID_2 = "TestConfiguredTableId2"
const val TEST_CONFIGURED_TABLE_DESC = "TestConfiguredTableDesc"
const val TEST_CONFIGURED_TABLE_ANALYSIS_METHOD = "TestConfiguredTableAnalysisMethod"
val TEST_CONFIGURED_TABLE_ALLOWED_COLUMNS = listOf("Column1")

const val TEST_PREVIOUS_CONFIGURED_TABLE_NAME = "TestPreviousConfiguredTableName"

private val glueDbName = "glueDbName"
private val glueTableName = "glueTableName"
private val testCreateTime: Instant = Instant.now()
private val testUpdateTime: Instant = Instant.now()

private val aggregationAnalysisRuleType = "AGGREGATION"
private val listAnalysisRuleType = "LIST"
private val listColumns = listOf("salesdate")
private val joinColumns = listOf("totalpurchasesales")
private val joinRequired = "QUERY_RUNNER"
private val dimensionColumns = listOf("salesdate", "totalpurchasesales")
private val aggregateColumnNames = listOf("userid")
private val aggregateColumnFunction = "COUNT"
private val constraintColumnName = "userid"
private val constraintColumnType = "COUNT_DISTINCT"
private val constraintMinimum = 100
private val scalarFunction = ScalarFunctions.ABS

val TEST_SYSTEM_TAGS = mapOf(
    "aws:cloudformation:stackname" to "testname",
    "aws:cloudformation:logicalid" to "testlogicalid",
    "aws:cloudformation:stackid" to "teststackid"
)
val TEST_CONFIGURED_TABLE_TAGS = mapOf("key1" to "value1", "key2" to "value2")
val TEST_UPDATED_CONFIGURED_TABLE_TAGS = mapOf("key1" to "value2", "key2" to "value2")
val TEST_RESOURCE_MODEL_TAGS = TEST_CONFIGURED_TABLE_TAGS.map { Tag.builder().key(it.key).value(it.value).build() }.toSet()

val TEST_CONFIGURED_TABLE_SUMMARY_1: ConfiguredTableSummary =
    ConfiguredTableSummary.builder()
        .id(TEST_CONFIGURED_TABLE_ID)
        .arn(TEST_CONFIGURED_TABLE_ARN)
        .name(TEST_CONFIGURED_TABLE_NAME)
        .analysisMethod(TEST_CONFIGURED_TABLE_ANALYSIS_METHOD)
        .build()

val TEST_CONFIGURED_TABLE_SUMMARY_2: ConfiguredTableSummary =
    ConfiguredTableSummary.builder()
        .id(TEST_CONFIGURED_TABLE_ID_2)
        .arn(TEST_CONFIGURED_TABLE_ARN_2)
        .name(TEST_CONFIGURED_TABLE_NAME)
        .analysisMethod(TEST_CONFIGURED_TABLE_ANALYSIS_METHOD)
        .build()

val TEST_CONFIGURED_TABLE: ConfiguredTable =
    ConfiguredTable.builder()
        .id(TEST_CONFIGURED_TABLE_ID)
        .arn(TEST_CONFIGURED_TABLE_ARN)
        .name(TEST_CONFIGURED_TABLE_NAME)
        .description(TEST_CONFIGURED_TABLE_DESC)
        .analysisMethod(TEST_CONFIGURED_TABLE_ANALYSIS_METHOD)
        .allowedColumns(TEST_CONFIGURED_TABLE_ALLOWED_COLUMNS)
        .tableReference(
            SdkTableReference.builder()
                .glue(
                    SdkGlueTableReference.builder()
                        .databaseName(glueDbName)
                        .tableName(glueTableName)
                        .build()
                )
                .build()
        )
        .createTime(testCreateTime)
        .updateTime(testUpdateTime)
        .build()

val TEST_CONFIGURED_TABLE_REQUIRED_FIELDS: ConfiguredTable = TEST_CONFIGURED_TABLE.copy {
    it.description(null)
}

val TEST_CONFIGURED_TABLE_WITH_AGG_ANALYSIS_RULE: ConfiguredTable = TEST_CONFIGURED_TABLE.copy {
    it.analysisRuleTypes(listOf(ConfiguredTableAnalysisRuleType.AGGREGATION))
}

val TEST_CONFIGURED_TABLE_WITH_LIST_ANALYSIS_RULE: ConfiguredTable = TEST_CONFIGURED_TABLE.copy {
    it.analysisRuleTypes(listOf(ConfiguredTableAnalysisRuleType.LIST))
}

val TEST_GET_CONFIGURED_TABLE_WITH_AGG_ANALYSIS_RULE_RESPONSE: GetConfiguredTableResponse =
    GetConfiguredTableResponse.builder()
        .configuredTable(TEST_CONFIGURED_TABLE_WITH_AGG_ANALYSIS_RULE)
        .build()

val TEST_AGG_ANALYSIS_RULE: ConfiguredTableAnalysisRule = ConfiguredTableAnalysisRule.builder()
    .type(aggregationAnalysisRuleType)
    .policy(
        SdkConfiguredTableAnalysisRulePolicy.builder()
            .v1(
                SdkConfiguredTableAnalysisRulePolicyV1.builder()
                    .aggregation(
                        SdkAnalysisRuleAggregation.builder()
                            .joinColumns(joinColumns)
                            .joinRequired(joinRequired)
                            .dimensionColumns(dimensionColumns)
                            .aggregateColumns(
                                listOf(
                                    SdkAggregateColumn.builder()
                                        .columnNames(aggregateColumnNames)
                                        .function(aggregateColumnFunction)
                                        .build()
                                )
                            )
                            .outputConstraints(listOf(
                                SdkAggregationConstraint.builder()
                                    .columnName(constraintColumnName)
                                    .minimum(constraintMinimum)
                                    .type(constraintColumnType)
                                    .build()
                            ))
                            .scalarFunctions(listOf(scalarFunction))
                            .build()
                    )
                    .build()
            )
            .build()
    )
    .build()

val TEST_AGG_ANALYSIS_RULE_REQUIRED_FIELDS: ConfiguredTableAnalysisRule = ConfiguredTableAnalysisRule.builder()
    .type(aggregationAnalysisRuleType)
    .policy(
        SdkConfiguredTableAnalysisRulePolicy.builder()
            .v1(
                SdkConfiguredTableAnalysisRulePolicyV1.builder()
                    .aggregation(
                        SdkAnalysisRuleAggregation.builder()
                            .joinColumns(joinColumns)
                            .dimensionColumns(dimensionColumns)
                            .aggregateColumns(
                                listOf(
                                    SdkAggregateColumn.builder()
                                        .columnNames(aggregateColumnNames)
                                        .function(aggregateColumnFunction)
                                        .build()
                                )
                            )
                            .outputConstraints(listOf(
                                SdkAggregationConstraint.builder()
                                    .columnName(constraintColumnName)
                                    .minimum(constraintMinimum)
                                    .type(constraintColumnType)
                                    .build()
                            ))
                            .scalarFunctions(listOf(scalarFunction))
                            .build()
                    )
                    .build()
            )
            .build()
    )
    .build()

val TEST_LIST_ANALYSIS_RULE: ConfiguredTableAnalysisRule = ConfiguredTableAnalysisRule.builder()
    .type(listAnalysisRuleType)
    .policy(
        SdkConfiguredTableAnalysisRulePolicy.builder()
            .v1(
                SdkConfiguredTableAnalysisRulePolicyV1.builder()
                    .list(
                        SdkAnalysisRuleList.builder()
                            .listColumns(listColumns)
                            .joinColumns(joinColumns)
                            .build()
                    )
                    .build()
            )
            .build()
    )
    .build()

val TEST_RESOURCE_MODEL_AGG_ANALYSIS_RULE: AnalysisRule = AnalysisRule.builder()
    .type(aggregationAnalysisRuleType)
    .policy(
        ConfiguredTableAnalysisRulePolicy.builder()
            .v1(
                ConfiguredTableAnalysisRulePolicyV1.builder()
                    .aggregation(
                        AnalysisRuleAggregation.builder()
                            .joinColumns(joinColumns)
                            .joinRequired(joinRequired)
                            .dimensionColumns(dimensionColumns)
                            .aggregateColumns(
                                listOf(
                                    AggregateColumn.builder()
                                        .columnNames(aggregateColumnNames)
                                        .function(aggregateColumnFunction)
                                        .build()
                                )
                            )
                            .outputConstraints(listOf(
                                AggregationConstraint.builder()
                                    .columnName(constraintColumnName)
                                    .minimum(constraintMinimum.toDouble())
                                    .type(constraintColumnType)
                                    .build()
                            ))
                            .scalarFunctions(listOf(scalarFunction.name))
                            .build()
                    )
                    .build()
            )
            .build()
    )
    .build()

val TEST_RESOURCE_MODEL_AGG_ANALYSIS_RULE_REQUIRED_FIELDS: AnalysisRule = AnalysisRule.builder()
    .type(aggregationAnalysisRuleType)
    .policy(
        ConfiguredTableAnalysisRulePolicy.builder()
            .v1(
                ConfiguredTableAnalysisRulePolicyV1.builder()
                    .aggregation(
                        AnalysisRuleAggregation.builder()
                            .joinColumns(joinColumns)
                            .dimensionColumns(dimensionColumns)
                            .aggregateColumns(
                                listOf(
                                    AggregateColumn.builder()
                                        .columnNames(aggregateColumnNames)
                                        .function(aggregateColumnFunction)
                                        .build()
                                )
                            )
                            .outputConstraints(listOf(
                                AggregationConstraint.builder()
                                    .columnName(constraintColumnName)
                                    .minimum(constraintMinimum.toDouble())
                                    .type(constraintColumnType)
                                    .build()
                            ))
                            .scalarFunctions(listOf(scalarFunction.name))
                            .build()
                    )
                    .build()
            )
            .build()
    )
    .build()

val TEST_RESOURCE_MODEL_LIST_ANALYSIS_RULE: AnalysisRule = AnalysisRule.builder()
    .type(listAnalysisRuleType)
    .policy(
        ConfiguredTableAnalysisRulePolicy.builder()
            .v1(
                ConfiguredTableAnalysisRulePolicyV1.builder()
                    .list(
                        AnalysisRuleList.builder()
                            .listColumns(listColumns)
                            .joinColumns(joinColumns)
                            .build()
                    )
                    .build()
            )
            .build()
    )
    .build()

val TEST_DELETE_RESOURCE_MODEL: ResourceModel = ResourceModel.builder()
    .configuredTableIdentifier(TEST_CONFIGURED_TABLE_ID)
    .build()

val TEST_RESOURCE_MODEL: ResourceModel = ResourceModel.builder()
    .configuredTableIdentifier(TEST_CONFIGURED_TABLE_ID)
    .arn(TEST_CONFIGURED_TABLE_ARN)
    .name(TEST_CONFIGURED_TABLE_NAME)
    .description(TEST_CONFIGURED_TABLE_DESC)
    .analysisMethod(TEST_CONFIGURED_TABLE_ANALYSIS_METHOD)
    .allowedColumns(TEST_CONFIGURED_TABLE_ALLOWED_COLUMNS)
    .tableReference(
        TableReference.builder()
            .glue(
                GlueTableReference.builder()
                    .databaseName(glueDbName)
                    .tableName(glueTableName)
                    .build()
            )
            .build()
    )
    .tags(emptySet())
    .build()

val TEST_PREVIOUS_NAME_RESOURCE_MODEL: ResourceModel  =
    ResourceModel.builder()
        .configuredTableIdentifier(TEST_CONFIGURED_TABLE_ID)
        .arn(TEST_CONFIGURED_TABLE_ARN)
        .name(TEST_PREVIOUS_CONFIGURED_TABLE_NAME)
        .description(TEST_CONFIGURED_TABLE_DESC)
        .analysisMethod(TEST_CONFIGURED_TABLE_ANALYSIS_METHOD)
        .allowedColumns(TEST_CONFIGURED_TABLE_ALLOWED_COLUMNS)
        .tableReference(
            TableReference.builder()
                .glue(
                    GlueTableReference.builder()
                        .databaseName(glueDbName)
                        .tableName(glueTableName)
                        .build()
                )
                .build()
        )
        .tags(emptySet())
        .build()

val TEST_NULL_DESC_RESOURCE_MODEL: ResourceModel  =
    ResourceModel.builder()
        .configuredTableIdentifier(TEST_CONFIGURED_TABLE_ID)
        .arn(TEST_CONFIGURED_TABLE_ARN)
        .name(TEST_CONFIGURED_TABLE_NAME)
        .analysisMethod(TEST_CONFIGURED_TABLE_ANALYSIS_METHOD)
        .allowedColumns(TEST_CONFIGURED_TABLE_ALLOWED_COLUMNS)
        .tableReference(
            TableReference.builder()
                .glue(
                    GlueTableReference.builder()
                        .databaseName(glueDbName)
                        .tableName(glueTableName)
                        .build()
                )
                .build()
        )
        .tags(emptySet())
        .build()

val TEST_CREATE_RESOURCE_MODEL_REQUIRED_FIELDS: ResourceModel =
    ResourceModel.builder()
        .name(TEST_CONFIGURED_TABLE_NAME)
        .analysisMethod(TEST_CONFIGURED_TABLE_ANALYSIS_METHOD)
        .allowedColumns(TEST_CONFIGURED_TABLE_ALLOWED_COLUMNS)
        .tableReference(
            TableReference.builder()
                .glue(
                    GlueTableReference.builder()
                        .databaseName(glueDbName)
                        .tableName(glueTableName)
                        .build()
                )
                .build()
        )
        .tags(emptySet())
        .build()

val TEST_RESOURCE_MODEL_REQUIRED_FIELDS: ResourceModel =
    ResourceModel.builder()
        .configuredTableIdentifier(TEST_CONFIGURED_TABLE_ID)
        .arn(TEST_CONFIGURED_TABLE_ARN)
        .name(TEST_CONFIGURED_TABLE_NAME)
        .analysisMethod(TEST_CONFIGURED_TABLE_ANALYSIS_METHOD)
        .allowedColumns(TEST_CONFIGURED_TABLE_ALLOWED_COLUMNS)
        .tableReference(
            TableReference.builder()
                .glue(
                    GlueTableReference.builder()
                        .databaseName(glueDbName)
                        .tableName(glueTableName)
                        .build()
                )
                .build()
        )
        .tags(emptySet())
        .build()

val TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS: ResourceModel =
    ResourceModel.builder()
        .configuredTableIdentifier(TEST_CONFIGURED_TABLE_ID)
        .arn(TEST_CONFIGURED_TABLE_ARN)
        .name(TEST_CONFIGURED_TABLE_NAME)
        .analysisMethod(TEST_CONFIGURED_TABLE_ANALYSIS_METHOD)
        .allowedColumns(TEST_CONFIGURED_TABLE_ALLOWED_COLUMNS)
        .tableReference(
            TableReference.builder()
                .glue(
                    GlueTableReference.builder()
                        .databaseName(glueDbName)
                        .tableName(glueTableName)
                        .build()
                )
                .build()
        )
        .tags(TEST_RESOURCE_MODEL_TAGS)
        .build()

val TEST_CREATE_RESOURCE_MODEL_WITH_REQUIRED_AGG_ANALYSIS_RULE: ResourceModel = ResourceModel.builder()
    .name(TEST_CONFIGURED_TABLE_NAME)
    .description(TEST_CONFIGURED_TABLE_DESC)
    .analysisMethod(TEST_CONFIGURED_TABLE_ANALYSIS_METHOD)
    .allowedColumns(TEST_CONFIGURED_TABLE_ALLOWED_COLUMNS)
    .tableReference(
        TableReference.builder()
            .glue(
                GlueTableReference.builder()
                    .databaseName(glueDbName)
                    .tableName(glueTableName)
                    .build()
            )
            .build()
    )
    .analysisRules(listOf(TEST_RESOURCE_MODEL_AGG_ANALYSIS_RULE_REQUIRED_FIELDS))
    .tags(emptySet())
    .build()

val TEST_RESOURCE_MODEL_WITH_REQUIRED_AGG_ANALYSIS_RULE: ResourceModel = ResourceModel.builder()
    .configuredTableIdentifier(TEST_CONFIGURED_TABLE_ID)
    .arn(TEST_CONFIGURED_TABLE_ARN)
    .name(TEST_CONFIGURED_TABLE_NAME)
    .description(TEST_CONFIGURED_TABLE_DESC)
    .analysisMethod(TEST_CONFIGURED_TABLE_ANALYSIS_METHOD)
    .allowedColumns(TEST_CONFIGURED_TABLE_ALLOWED_COLUMNS)
    .tableReference(
        TableReference.builder()
            .glue(
                GlueTableReference.builder()
                    .databaseName(glueDbName)
                    .tableName(glueTableName)
                    .build()
            )
            .build()
    )
    .analysisRules(listOf(TEST_RESOURCE_MODEL_AGG_ANALYSIS_RULE_REQUIRED_FIELDS))
    .tags(emptySet())
    .build()

val TEST_RESOURCE_MODEL_WITH_AGG_ANALYSIS_RULE_AND_TAGS: ResourceModel = ResourceModel.builder()
    .configuredTableIdentifier(TEST_CONFIGURED_TABLE_ID)
    .arn(TEST_CONFIGURED_TABLE_ARN)
    .name(TEST_CONFIGURED_TABLE_NAME)
    .description(TEST_CONFIGURED_TABLE_DESC)
    .analysisMethod(TEST_CONFIGURED_TABLE_ANALYSIS_METHOD)
    .allowedColumns(TEST_CONFIGURED_TABLE_ALLOWED_COLUMNS)
    .tableReference(
        TableReference.builder()
            .glue(
                GlueTableReference.builder()
                    .databaseName(glueDbName)
                    .tableName(glueTableName)
                    .build()
            )
            .build()
    )
    .analysisRules(listOf(TEST_RESOURCE_MODEL_AGG_ANALYSIS_RULE))
    .tags(TEST_RESOURCE_MODEL_TAGS)
    .build()

val TEST_CREATE_RESOURCE_MODEL_WITH_REQUIRED_LIST_ANALYSIS_RULE: ResourceModel = ResourceModel.builder()
    .name(TEST_CONFIGURED_TABLE_NAME)
    .description(TEST_CONFIGURED_TABLE_DESC)
    .analysisMethod(TEST_CONFIGURED_TABLE_ANALYSIS_METHOD)
    .allowedColumns(TEST_CONFIGURED_TABLE_ALLOWED_COLUMNS)
    .tableReference(
        TableReference.builder()
            .glue(
                GlueTableReference.builder()
                    .databaseName(glueDbName)
                    .tableName(glueTableName)
                    .build()
            )
            .build()
    )
    .analysisRules(listOf(TEST_RESOURCE_MODEL_LIST_ANALYSIS_RULE))
    .tags(emptySet())
    .build()

val TEST_RESOURCE_MODEL_WITH_REQUIRED_LIST_ANALYSIS_RULE: ResourceModel = ResourceModel.builder()
    .configuredTableIdentifier(TEST_CONFIGURED_TABLE_ID)
    .arn(TEST_CONFIGURED_TABLE_ARN)
    .name(TEST_CONFIGURED_TABLE_NAME)
    .description(TEST_CONFIGURED_TABLE_DESC)
    .analysisMethod(TEST_CONFIGURED_TABLE_ANALYSIS_METHOD)
    .allowedColumns(TEST_CONFIGURED_TABLE_ALLOWED_COLUMNS)
    .tableReference(
        TableReference.builder()
            .glue(
                GlueTableReference.builder()
                    .databaseName(glueDbName)
                    .tableName(glueTableName)
                    .build()
            )
            .build()
    )
    .analysisRules(listOf(TEST_RESOURCE_MODEL_LIST_ANALYSIS_RULE))
    .tags(emptySet())
    .build()

val TEST_RESOURCE_MODEL_SUMMARY_1:ResourceModel =
    ResourceModel.builder()
        .configuredTableIdentifier(TEST_CONFIGURED_TABLE_ID)
        .build()

val TEST_RESOURCE_MODEL_SUMMARY_2:ResourceModel =
    ResourceModel.builder()
        .configuredTableIdentifier(TEST_CONFIGURED_TABLE_ID_2)
        .build()
