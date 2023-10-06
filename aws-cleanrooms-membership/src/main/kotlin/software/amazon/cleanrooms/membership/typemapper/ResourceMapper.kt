package software.amazon.cleanrooms.membership.typemapper

import software.amazon.awssdk.services.cleanrooms.model.MembershipProtectedQueryOutputConfiguration
import software.amazon.awssdk.services.cleanrooms.model.MembershipProtectedQueryResultConfiguration
import software.amazon.cleanrooms.membership.ResourceModel

fun ResourceModel.toDefaultResultConfiguration(): MembershipProtectedQueryResultConfiguration =
    MembershipProtectedQueryResultConfiguration.builder()
    .apply {
        with(defaultResultConfiguration){
            roleArn?.let { roleArn(it) }
            outputConfiguration(
                with(outputConfiguration.s3) {
                    MembershipProtectedQueryOutputConfiguration.fromS3 {
                        it.bucket(bucket)
                        it.keyPrefix(keyPrefix)
                        it.resultFormat(resultFormat)
                    }
                }
            )
        }
    }
    .build()
