package software.amazon.cleanrooms.membership.typemapper

import software.amazon.cleanrooms.membership.Tag

fun Collection<Tag>.toSdkTags(): Map<String, String> = associate {
    it.key to it.value
}
fun Map<String, String>.toResourceModelTags(): Set<Tag> = mapNotNull {
    Tag(it.key, it.value)
}.toSet()
