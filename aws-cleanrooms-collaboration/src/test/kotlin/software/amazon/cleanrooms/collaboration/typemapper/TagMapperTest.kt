package software.amazon.cleanrooms.collaboration.typemapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.cleanrooms.collaboration.TEST_COLLABORATION_TAGS
import software.amazon.cleanrooms.collaboration.TEST_RESOURCE_MODEL_TAGS

class TagMapperTest {
    @Test
    fun `toSdkTags correctly maps resourceModel tags to sdk tags`() {
        val expectedSdkTags = TEST_COLLABORATION_TAGS
        val translatedTags = TEST_RESOURCE_MODEL_TAGS.toSdkTags()
        assertThat(translatedTags).isEqualTo(expectedSdkTags)
    }
}
