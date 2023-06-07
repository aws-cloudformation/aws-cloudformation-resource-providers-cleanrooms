package software.amazon.cleanrooms.configuredtable.typemapper

import software.amazon.cleanrooms.configuredtable.Tag

fun Collection<Tag>.toSdkTags(): Map<String, String> = associate {
    it.key to it.value
}
