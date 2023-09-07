package software.amazon.cleanrooms.analysistemplate.typemapper

import software.amazon.cleanrooms.analysistemplate.Tag

fun Collection<Tag>.toSdkTags(): Map<String, String> = associate {
    it.key to it.value
}

fun Map<String, String>.toResourceModelTags(): List<Tag> = mapNotNull {
    Tag(it.key, it.value)
}
