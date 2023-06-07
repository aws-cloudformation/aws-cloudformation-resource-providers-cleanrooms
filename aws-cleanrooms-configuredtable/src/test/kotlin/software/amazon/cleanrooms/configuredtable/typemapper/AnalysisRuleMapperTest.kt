package software.amazon.cleanrooms.configuredtable.typemapper

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAnalysisRule
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAnalysisRulePolicy
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAnalysisRulePolicyV1
import software.amazon.cleanrooms.configuredtable.TEST_AGG_ANALYSIS_RULE
import software.amazon.cleanrooms.configuredtable.TEST_LIST_ANALYSIS_RULE
import software.amazon.cleanrooms.configuredtable.TEST_RESOURCE_MODEL_AGG_ANALYSIS_RULE
import software.amazon.cleanrooms.configuredtable.TEST_RESOURCE_MODEL_LIST_ANALYSIS_RULE

class AnalysisRuleMapperTest {

    @Test
    fun `toAnalysisRule correctly maps aggregation Sdk ConfiguredTableAnalysisRule to resourceModel AnalysisRule`() {
        val expectedResourceModelAnalysisRule = TEST_RESOURCE_MODEL_AGG_ANALYSIS_RULE
        val translatedAnalysisRule = TEST_AGG_ANALYSIS_RULE.toAnalysisRule()
        Assertions.assertThat(translatedAnalysisRule).isEqualTo(expectedResourceModelAnalysisRule)
    }

    @Test
    fun `toAnalysisRule correctly maps list Sdk ConfiguredTableAnalysisRule to resourceModel AnalysisRule`() {
        val expectedResourceModelAnalysisRule = TEST_RESOURCE_MODEL_LIST_ANALYSIS_RULE
        val translatedAnalysisRule = TEST_LIST_ANALYSIS_RULE.toAnalysisRule()
        Assertions.assertThat(translatedAnalysisRule).isEqualTo(expectedResourceModelAnalysisRule)
    }
}
