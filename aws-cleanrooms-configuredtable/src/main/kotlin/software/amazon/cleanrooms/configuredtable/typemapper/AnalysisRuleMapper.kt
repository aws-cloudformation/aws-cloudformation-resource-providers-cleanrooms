package software.amazon.cleanrooms.configuredtable.typemapper

import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAnalysisRule
import software.amazon.cleanrooms.configuredtable.AggregateColumn
import software.amazon.cleanrooms.configuredtable.AggregationConstraint
import software.amazon.cleanrooms.configuredtable.AnalysisRule
import software.amazon.cleanrooms.configuredtable.AnalysisRuleAggregation
import software.amazon.cleanrooms.configuredtable.AnalysisRuleList
import software.amazon.cleanrooms.configuredtable.ConfiguredTableAnalysisRulePolicy
import software.amazon.cleanrooms.configuredtable.ConfiguredTableAnalysisRulePolicyV1
import software.amazon.awssdk.services.cleanrooms.model.AnalysisRuleAggregation as SdkAnalysisRuleAggregation
import software.amazon.awssdk.services.cleanrooms.model.AnalysisRuleList as SdkAnalysisRuleList
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAnalysisRulePolicyV1 as SdkConfiguredTableAnalysisRulePolicyV1

/**
 * Convert a ConfiguredTableAnalysisRule object to an AnalysisRule object
 */
fun ConfiguredTableAnalysisRule.toAnalysisRule(): AnalysisRule {
    return AnalysisRule.builder()
        .type(typeAsString())
        .policy(
            ConfiguredTableAnalysisRulePolicy.builder()
                .v1(policy().v1().toConfiguredTableAnalysisRulePolicyV1())
                .build()
        )
        .build()
}

private fun SdkConfiguredTableAnalysisRulePolicyV1.toConfiguredTableAnalysisRulePolicyV1(): ConfiguredTableAnalysisRulePolicyV1 {
    return when (type()) {
        SdkConfiguredTableAnalysisRulePolicyV1.Type.AGGREGATION ->
            ConfiguredTableAnalysisRulePolicyV1.builder()
                .aggregation(aggregation().toAnalysisRuleAggregation())
                .build()
        SdkConfiguredTableAnalysisRulePolicyV1.Type.LIST ->
            ConfiguredTableAnalysisRulePolicyV1.builder()
                .list(list().toAnalysisRuleList())
                .build()
        else -> throw Exception("Unsupported AnalysisRule Type")
    }
}

private fun SdkAnalysisRuleAggregation.toAnalysisRuleAggregation(): AnalysisRuleAggregation {
    return AnalysisRuleAggregation.builder()
        .joinColumns(joinColumns())
        .joinRequired(joinRequiredAsString())
        .dimensionColumns(dimensionColumns())
        .aggregateColumns(
            aggregateColumns().map { aggregateColumn ->
                AggregateColumn.builder()
                    .columnNames(aggregateColumn.columnNames())
                    .function(aggregateColumn.functionAsString())
                    .build()
            }
        )
        .outputConstraints(
            outputConstraints().map { aggregationConstraint ->
                AggregationConstraint.builder()
                    .columnName(aggregationConstraint.columnName())
                    .type(aggregationConstraint.typeAsString())
                    .minimum(aggregationConstraint.minimum().toDouble())
                    .build()
            }
        )
        .scalarFunctions(
            scalarFunctions().map { scalarFunctions ->
                scalarFunctions.name
            }
        )
        .build()
}

private fun SdkAnalysisRuleList.toAnalysisRuleList(): AnalysisRuleList {
    return AnalysisRuleList.builder()
        .joinColumns(joinColumns())
        .listColumns(listColumns())
        .build()
}
