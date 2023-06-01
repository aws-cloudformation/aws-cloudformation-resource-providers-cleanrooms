package software.amazon.cleanrooms.membership.typemapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.cleanrooms.membership.TEST_MEMBERSHIP_TAGS
import software.amazon.cleanrooms.membership.TEST_RESOURCE_MODEL_TAGS

class TagMapperTest {

    @Test
    fun `toSdkTags correctly maps resourceModel tags to sdk tags`() {
        val expectedSdkTags = TEST_MEMBERSHIP_TAGS
        val translatedTags = TEST_RESOURCE_MODEL_TAGS.toSdkTags()
        assertThat(translatedTags).isEqualTo(expectedSdkTags)
    }
}
