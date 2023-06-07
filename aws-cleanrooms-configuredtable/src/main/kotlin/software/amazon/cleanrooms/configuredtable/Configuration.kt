package software.amazon.cleanrooms.configuredtable

import software.amazon.cleanrooms.configuredtable.typemapper.toSdkTags


/**
 * This class is marked as internal because we do not want anyone outside our package to use it.
 */
internal class Configuration : BaseConfiguration("aws-cleanrooms-configuredtable.json") {
    /**
     * Providers should implement this method if their resource has a 'Tags' property to define
     * resource-level tags
     */
    override fun resourceDefinedTags(resourceModel: ResourceModel): Map<String, String> {
        return resourceModel.tags?.toSdkTags() ?: emptyMap()
    }
}
