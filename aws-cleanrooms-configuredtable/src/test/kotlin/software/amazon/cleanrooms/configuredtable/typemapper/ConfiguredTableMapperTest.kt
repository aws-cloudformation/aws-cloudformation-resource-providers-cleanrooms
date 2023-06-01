package software.amazon.cleanrooms.configuredtable.typemapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.cleanrooms.configuredtable.TEST_CONFIGURED_TABLE
import software.amazon.cleanrooms.configuredtable.TEST_RESOURCE_MODEL
import software.amazon.cleanrooms.configuredtable.TEST_RESOURCE_MODEL_AGG_ANALYSIS_RULE
import software.amazon.cleanrooms.configuredtable.TEST_RESOURCE_MODEL_LIST_ANALYSIS_RULE
import software.amazon.cleanrooms.configuredtable.TEST_RESOURCE_MODEL_TAGS
import software.amazon.cleanrooms.configuredtable.TEST_RESOURCE_MODEL_WITH_AGG_ANALYSIS_RULE_AND_TAGS
import software.amazon.cleanrooms.configuredtable.TEST_RESOURCE_MODEL_WITH_REQUIRED_LIST_ANALYSIS_RULE

class ConfiguredTableMapperTest {

    @Test
    fun `Sdk ConfiguredTable correctly maps to resource model`() {
        val translatedResourceModel = TEST_CONFIGURED_TABLE.toResourceModel(emptyList())
        assertThat(translatedResourceModel).isEqualTo(TEST_RESOURCE_MODEL)
    }

    @Test
    fun `Sdk ConfiguredTable correctly maps to resource model with analysis rule`() {
        val translatedResourceModel = TEST_CONFIGURED_TABLE.toResourceModel(listOf(TEST_RESOURCE_MODEL_LIST_ANALYSIS_RULE))
        assertThat(translatedResourceModel).isEqualTo(TEST_RESOURCE_MODEL_WITH_REQUIRED_LIST_ANALYSIS_RULE)
    }

    @Test
    fun `Sdk ConfiguredTable correctly maps to resource model with analysis rule and tags`() {
        val translatedResourceModel = TEST_CONFIGURED_TABLE.toResourceModel(listOf(TEST_RESOURCE_MODEL_AGG_ANALYSIS_RULE), TEST_RESOURCE_MODEL_TAGS)
        assertThat(translatedResourceModel).isEqualTo(TEST_RESOURCE_MODEL_WITH_AGG_ANALYSIS_RULE_AND_TAGS)
    }
}
