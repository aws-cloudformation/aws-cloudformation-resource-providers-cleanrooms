package software.amazon.cleanrooms.membership.typemapper

import org.jetbrains.annotations.TestOnly
import software.amazon.awssdk.services.cleanrooms.model.Membership
import software.amazon.awssdk.services.cleanrooms.model.MembershipProtectedQueryResultConfiguration as SdkResultConfiguration
import software.amazon.awssdk.services.cleanrooms.model.MembershipProtectedQueryOutputConfiguration as SdkOutputConfiguration
import software.amazon.cleanrooms.membership.MembershipProtectedQueryResultConfiguration
import software.amazon.cleanrooms.membership.MembershipProtectedQueryOutputConfiguration
import software.amazon.cleanrooms.membership.ProtectedQueryS3OutputConfiguration


import software.amazon.cleanrooms.membership.ResourceModel
import software.amazon.cleanrooms.membership.Tag

/**
 * Convert a Membership object to a ResourceModel object
 */
fun Membership.toResourceModel(tags: Set<Tag>? = emptySet()): ResourceModel = ResourceModel.builder()
    .arn(arn())
    .tags(tags)
    .collaborationArn(collaborationArn())
    .collaborationCreatorAccountId(collaborationCreatorAccountId())
    .collaborationIdentifier(collaborationId())
    .membershipIdentifier(id())
    .apply { this@toResourceModel.defaultResultConfiguration()?.let { defaultResultConfiguration(it.toResourceModel()) } }
    .queryLogStatus(queryLogStatusAsString())
    .build()


fun SdkResultConfiguration.toResourceModel(): MembershipProtectedQueryResultConfiguration = MembershipProtectedQueryResultConfiguration.builder()
    .apply {
        outputConfiguration(this@toResourceModel.outputConfiguration().toResouceModel())
        this@toResourceModel.roleArn()?.let { roleArn(it) }
    }
    .build()

fun SdkOutputConfiguration.toResouceModel(): MembershipProtectedQueryOutputConfiguration = MembershipProtectedQueryOutputConfiguration.builder()
    .apply {
        s3(
            ProtectedQueryS3OutputConfiguration.builder()
                .apply {
                    with(this@toResouceModel.s3()) {
                        bucket(bucket())
                        resultFormat(resultFormatAsString())
                        this.keyPrefix()?.let { keyPrefix(it) }
                    }
                }
                .build()
        )
    }
    .build()
