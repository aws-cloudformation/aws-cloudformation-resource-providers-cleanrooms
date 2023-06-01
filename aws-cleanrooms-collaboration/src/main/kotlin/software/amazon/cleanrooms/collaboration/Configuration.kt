package software.amazon.cleanrooms.collaboration

import software.amazon.cleanrooms.collaboration.typemapper.toSdkTags

/**
 * This class is marked as internal because we do not want anyone outside our package to use it.
 */
internal class Configuration : BaseConfiguration("aws-cleanrooms-collaboration.json") {

    override fun resourceDefinedTags(resourceModel: ResourceModel): Map<String, String> = with(resourceModel) {
        tags?.toSdkTags() ?: emptyMap()
    }
}
