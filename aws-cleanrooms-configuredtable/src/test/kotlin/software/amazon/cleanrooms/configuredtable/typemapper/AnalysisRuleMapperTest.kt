package software.amazon.cleanrooms.configuredtable.typemapper

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import software.amazon.cleanrooms.configuredtable.TEST_AGG_ANALYSIS_RULE
import software.amazon.cleanrooms.configuredtable.TEST_CUSTOM_ANALYSIS_RULE_ANALYSES
import software.amazon.cleanrooms.configuredtable.TEST_CUSTOM_ANALYSIS_RULE_PROVIDERS
import software.amazon.cleanrooms.configuredtable.TEST_LIST_ANALYSIS_RULE
import software.amazon.cleanrooms.configuredtable.TEST_RESOURCE_MODEL_AGG_ANALYSIS_RULE
import software.amazon.cleanrooms.configuredtable.TEST_RESOURCE_MODEL_CUSTOM_ANALYSIS_RULE_ALLOWED_ANALYSIS
import software.amazon.cleanrooms.configuredtable.TEST_RESOURCE_MODEL_CUSTOM_ANALYSIS_RULE_ALLOWED_PROVIDERS
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

    @Test
    fun `toAnalysisRule correctly maps custom with allowed analyses Sdk ConfiguredTableAnalysisRule to resourceModel AnalysisRule`() {
        val expectedResourceModelAnalysisRule = TEST_RESOURCE_MODEL_CUSTOM_ANALYSIS_RULE_ALLOWED_ANALYSIS
        val translatedAnalysisRule = TEST_CUSTOM_ANALYSIS_RULE_ANALYSES.toAnalysisRule()
        Assertions.assertThat(translatedAnalysisRule).isEqualTo(expectedResourceModelAnalysisRule)
    }

    @Test
    fun `toAnalysisRule correctly maps custom with allowed providers Sdk ConfiguredTableAnalysisRule to resourceModel AnalysisRule`() {
        val expectedResourceModelAnalysisRule = TEST_RESOURCE_MODEL_CUSTOM_ANALYSIS_RULE_ALLOWED_PROVIDERS
        val translatedAnalysisRule = TEST_CUSTOM_ANALYSIS_RULE_PROVIDERS.toAnalysisRule()
        Assertions.assertThat(translatedAnalysisRule).isEqualTo(expectedResourceModelAnalysisRule)
    }
}
