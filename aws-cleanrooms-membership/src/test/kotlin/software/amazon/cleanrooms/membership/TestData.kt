package software.amazon.cleanrooms.membership

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import software.amazon.awssdk.services.cleanrooms.model.Membership
import software.amazon.awssdk.services.cleanrooms.model.MembershipProtectedQueryOutputConfiguration
import software.amazon.awssdk.services.cleanrooms.model.MembershipProtectedQueryResultConfiguration
import software.amazon.awssdk.services.cleanrooms.model.MembershipQueryLogStatus
import software.amazon.awssdk.services.cleanrooms.model.MembershipSummary
import software.amazon.awssdk.services.cleanrooms.model.ProtectedQueryS3OutputConfiguration
import software.amazon.awssdk.services.cleanrooms.model.ResultFormat
import software.amazon.cleanrooms.membership.typemapper.toResourceModel
import software.amazon.cleanrooms.membership.typemapper.toResourceModelTags


const val TEST_COLLABORATION_ID = "TestCollaborationId"
const val TEST_MEMBERSHIP_ID = "TestMembershipId"
const val TEST_MEMBERSHIP_ARN = "arn:aws:cleanrooms:us-east-1:123456789012:membership/5ac65a10-dde7-4916-890e-3abc51e07839"
const val TEST_COLLAB_ARN = "arn:aws:cleanrooms:us-east-1:123456789012:collaboration/fbc58dc6-7d91-4e53-b75e-23275d50a032"
const val TEST_COLLAB_CREATOR_ACCOUNT_ID = "TestCollabCreatorAccountId"
const val TEST_COLLAB_CREATOR_NAME = "TestCollabCreatorName"
const val TEST_COLLAB_NAME = "TestCollabName"
const val TEST_CREATOR_ACCOUNT_ID = "TestCreatorAccountId"
const val TEST_NEXT_TOKEN = "TestNextToken"

val TEST_SYSTEM_TAGS = mapOf(
    "aws:cloudformation:stackname" to "testname",
    "aws:cloudformation:logicalid" to "testlogicalid",
    "aws:cloudformation:stackid" to "teststackid"
)

val TEST_MEMBERSHIP_TAGS = mapOf("key1" to "value1", "key2" to "value2")
val TEST_RESOURCE_MODEL_TAGS = TEST_MEMBERSHIP_TAGS.map { Tag.builder().key(it.key).value(it.value).build() }.toSet()

// All fields in Membership resource are required,
// https://docs.aws.amazon.com/clean-rooms/latest/apireference/API_Membership.html
val TEST_BASE_MEMBERSHIP_WITH_REQUIRED_FIELDS: Membership = Membership.builder()
    .id(TEST_MEMBERSHIP_ID)
    .arn(TEST_MEMBERSHIP_ARN)
    .collaborationId(TEST_COLLABORATION_ID)
    .collaborationArn(TEST_COLLAB_ARN)
    .collaborationCreatorAccountId(TEST_COLLAB_CREATOR_ACCOUNT_ID)
    .collaborationCreatorDisplayName(TEST_COLLAB_CREATOR_NAME)
    .collaborationName(TEST_COLLAB_NAME)
    .queryLogStatus(MembershipQueryLogStatus.ENABLED)
    .build()

val TEST_MEMBERSHIP_WITH_RESULT_CONFIGURATION  = TEST_BASE_MEMBERSHIP_WITH_REQUIRED_FIELDS.copy {
    it.defaultResultConfiguration(MembershipProtectedQueryResultConfiguration.builder()
        .outputConfiguration(
            MembershipProtectedQueryOutputConfiguration.builder()
                .s3(
                    ProtectedQueryS3OutputConfiguration.builder()
                        .resultFormat(ResultFormat.CSV.name)
                        .bucket("testbucket")
                        .keyPrefix("testkeyprefix")
                        .build()
                )
                .build()
        )
        .roleArn("0000000000000000000000000:role/TestRole")
        .build())
}

val TEST_MEMBERSHIP_RESPONSE_WITH_ALL_ARGS: Membership = TEST_MEMBERSHIP_WITH_RESULT_CONFIGURATION.toBuilder()
    .arn(TEST_MEMBERSHIP_ARN)
    .id(TEST_MEMBERSHIP_ID)
    .build()

val TEST_MEMBERSHIP_RESPONSE_WITH_REQUIRED_FIELDS: Membership = TEST_BASE_MEMBERSHIP_WITH_REQUIRED_FIELDS.copy {
    it.id(TEST_MEMBERSHIP_ID).arn(TEST_MEMBERSHIP_ARN)
}

val TEST_MEMBERSHIP_SUMMARY_1: MembershipSummary = MembershipSummary.builder()
    .id(TEST_MEMBERSHIP_ID)
    .arn(TEST_MEMBERSHIP_ARN)
    .collaborationArn(TEST_COLLAB_ARN)
    .collaborationId(TEST_COLLABORATION_ID)
    .collaborationCreatorAccountId(TEST_COLLAB_CREATOR_ACCOUNT_ID)
    .collaborationCreatorDisplayName(TEST_COLLAB_CREATOR_NAME)
    .collaborationName(TEST_COLLAB_NAME)
    .build()

val TEST_MEMBERSHIP_SUMMARY_2: MembershipSummary = TEST_MEMBERSHIP_SUMMARY_1.copy { it.id("$TEST_MEMBERSHIP_ID-2").arn("$TEST_MEMBERSHIP_ARN-2") }

val TEST_MEMBERSHIP_RESPONSE_WITH_QUERYLOG_ENABLED: Membership = TEST_BASE_MEMBERSHIP_WITH_REQUIRED_FIELDS.copy {
    it.queryLogStatus(MembershipQueryLogStatus.ENABLED)
}

val TEST_MEMBERSHIP_RESPONSE_WITH_QUERYLOG_DISABLED: Membership = TEST_BASE_MEMBERSHIP_WITH_REQUIRED_FIELDS.copy {
    it.queryLogStatus(MembershipQueryLogStatus.DISABLED)
}

val TEST_QUERYLOG_DISABLED_RESOURCE_MODEL: ResourceModel = ResourceModel.builder()
    .arn(TEST_MEMBERSHIP_ARN)
    .collaborationArn(TEST_COLLAB_ARN)
    .collaborationIdentifier(TEST_COLLABORATION_ID)
    .queryLogStatus(MembershipQueryLogStatus.DISABLED.name)
    .membershipIdentifier(TEST_MEMBERSHIP_ID)
    .collaborationCreatorAccountId(TEST_COLLAB_CREATOR_ACCOUNT_ID)
    .tags(emptySet())
    .build()

val TEST_QUERYLOG_ENABLED_RESOURCE_MODEL: ResourceModel = ResourceModel.builder()
    .arn(TEST_MEMBERSHIP_ARN)
    .collaborationArn(TEST_COLLAB_ARN)
    .collaborationIdentifier(TEST_COLLABORATION_ID)
    .queryLogStatus(MembershipQueryLogStatus.ENABLED.name)
    .membershipIdentifier(TEST_MEMBERSHIP_ID)
    .collaborationCreatorAccountId(TEST_COLLAB_CREATOR_ACCOUNT_ID)
    .tags(emptySet())
    .build()


val TEST_RESOURCE_MODEL_WITH_TAGS: ResourceModel = ResourceModel.builder()
    .arn(TEST_MEMBERSHIP_ARN)
    .collaborationArn(TEST_COLLAB_ARN)
    .collaborationIdentifier(TEST_COLLABORATION_ID)
    .queryLogStatus(MembershipQueryLogStatus.ENABLED.name)
    .membershipIdentifier(TEST_MEMBERSHIP_ID)
    .collaborationCreatorAccountId(TEST_COLLAB_CREATOR_ACCOUNT_ID)
    .tags(TEST_MEMBERSHIP_TAGS.toResourceModelTags())
    .build()

val TEST_RESOURCE_MODEL_WITHOUT_TAGS: ResourceModel = ResourceModel.builder()
    .arn(TEST_MEMBERSHIP_ARN)
    .collaborationArn(TEST_COLLAB_ARN)
    .collaborationIdentifier(TEST_COLLABORATION_ID)
    .queryLogStatus(MembershipQueryLogStatus.ENABLED.name)
    .membershipIdentifier(TEST_MEMBERSHIP_ID)
    .collaborationCreatorAccountId(TEST_COLLAB_CREATOR_ACCOUNT_ID)
    .tags(emptySet())
    .build()

val TEST_MEMBERSHIP_TAGS_BEFORE_UPDATE = mapOf("key1" to "oldVal1", "key2" to "oldVal2", "key3" to "sameval")
val TEST_RESOURCE_MODEL_WITH_TAGS_BEFORE_UPDATE: ResourceModel = ResourceModel.builder()
    .arn(TEST_MEMBERSHIP_ARN)
    .collaborationArn(TEST_COLLAB_ARN)
    .collaborationIdentifier(TEST_COLLABORATION_ID)
    .queryLogStatus(MembershipQueryLogStatus.ENABLED.name)
    .membershipIdentifier(TEST_MEMBERSHIP_ID)
    .collaborationCreatorAccountId(TEST_COLLAB_CREATOR_ACCOUNT_ID)
    .tags(TEST_MEMBERSHIP_TAGS_BEFORE_UPDATE.toResourceModelTags())
    .build()

val TEST_MEMBERSHIP_TAGS_AFTER_UPDATE = mapOf("key1" to "newVal1", "key2" to "newVal2", "key3" to "sameval", "key4" to "newVal4")

val TEST_RESOURCE_MODEL_WITH_TAGS_AFTER_UPDATE: ResourceModel = ResourceModel.builder()
    .arn(TEST_MEMBERSHIP_ARN)
    .collaborationArn(TEST_COLLAB_ARN)
    .collaborationIdentifier(TEST_COLLABORATION_ID)
    .queryLogStatus(MembershipQueryLogStatus.ENABLED.name)
    .membershipIdentifier(TEST_MEMBERSHIP_ID)
    .collaborationCreatorAccountId(TEST_COLLAB_CREATOR_ACCOUNT_ID)
    .tags(TEST_MEMBERSHIP_TAGS_AFTER_UPDATE.toResourceModelTags())
    .build()
