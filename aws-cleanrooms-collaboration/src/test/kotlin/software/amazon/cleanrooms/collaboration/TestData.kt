package software.amazon.cleanrooms.collaboration

import software.amazon.awssdk.services.cleanrooms.model.Collaboration
import software.amazon.awssdk.services.cleanrooms.model.CollaborationQueryLogStatus
import software.amazon.awssdk.services.cleanrooms.model.CollaborationSummary
import software.amazon.awssdk.services.cleanrooms.model.DataEncryptionMetadata
import software.amazon.awssdk.services.cleanrooms.model.GetCollaborationResponse
import software.amazon.awssdk.services.cleanrooms.model.MemberAbility
import software.amazon.awssdk.services.cleanrooms.model.MemberStatus
import software.amazon.awssdk.services.cleanrooms.model.MemberSummary
import software.amazon.cleanrooms.collaboration.typemapper.toMemberSpecification
import software.amazon.cleanrooms.collaboration.typemapper.toResourceModel
import software.amazon.cleanrooms.collaboration.typemapper.toResourceModelTags

const val TEST_COLLABORATION_NAME = "TestCollab1"
const val TEST_NEXT_TOKEN = "TEST_NEXT_TOKEN"
const val TEST_DESCRIPTION = "TestDescription"
const val TEST_DISPLAY_NAME = "TestDisplayName"
const val TEST_COLLAB_NEW_NAME = "NewName"
const val TEST_COLLAB_NEW_DESCRIPTION = "DESCRIPTION_CHANGED"
const val TEST_MEMBER_DISPLAY_NAME = "TestMemberDisplayName"
const val TEST_CREATOR_ACCOUNT_ID = "TestCreatorAccountId"
const val TEST_MEMBER_ACCOUNT_ID = "TestMemberAccountId"
const val TEST_COLLAB_ID = "TestCollabId"
const val TEST_COLLAB_ARN = "arn:aws:cleanrooms:us-east-1:123456789012:collaboration/fbc58dc6-7d91-4e53-b75e-23275d50a032"

val TEST_SYSTEM_TAGS = mapOf(
    "aws:cloudformation:stackname" to "testname",
    "aws:cloudformation:logicalid" to "testlogicalid",
    "aws:cloudformation:stackid" to "teststackid"
)

val TEST_COLLABORATION_TAGS = mapOf("key1" to "value1", "key2" to "value2")
val TEST_COLLABORATION_NEW_TAGS = mapOf("newKey" to "newValue")
val TEST_RESOURCE_MODEL_TAGS = TEST_COLLABORATION_TAGS.map { Tag.builder().key(it.key).value(it.value).build() }.toSet()

val TEST_BASE_COLLABORATION_WITH_REQUIRED_FIELDS = Collaboration.builder()
    .name(TEST_COLLABORATION_NAME)
    .queryLogStatus(CollaborationQueryLogStatus.ENABLED)
    .description(TEST_DESCRIPTION)
    .creatorDisplayName(TEST_DISPLAY_NAME)
    .queryLogStatus(CollaborationQueryLogStatus.ENABLED)
    .creatorAccountId(TEST_CREATOR_ACCOUNT_ID)
    .build()

val TEST_BASE_COLLABORATION_WITH_ALL_ARGS = Collaboration.builder()
    .name(TEST_COLLABORATION_NAME)
    .queryLogStatus(CollaborationQueryLogStatus.ENABLED)
    .description(TEST_DESCRIPTION)
    .creatorDisplayName(TEST_DISPLAY_NAME)
    .queryLogStatus(CollaborationQueryLogStatus.ENABLED)
    .creatorAccountId(TEST_CREATOR_ACCOUNT_ID)
    .dataEncryptionMetadata(
        DataEncryptionMetadata.builder()
            .allowDuplicates(true)
            .allowCleartext(true)
            .preserveNulls(true)
            .allowJoinsOnColumnsWithDifferentNames(true)
            .build()
    )
    .build()

val TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS = TEST_BASE_COLLABORATION_WITH_REQUIRED_FIELDS.copy {
    it.id(TEST_COLLAB_ID).arn(TEST_COLLAB_ARN)
}

val TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS_AND_NEW_NAME = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS.copy {
    it.name(TEST_COLLAB_NEW_NAME)
}

val TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS_AND_NEW_DESCRIPTION = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS.copy {
    it.description(TEST_COLLAB_NEW_DESCRIPTION)
}

val TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS_WITH_NEW_NAME_AND_DESCRIPTION = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS.copy {
    it.name(TEST_COLLAB_NEW_NAME)
    it.description(TEST_COLLAB_NEW_DESCRIPTION)
}

val TEST_COLLABORATION_RESPONSE_WITH_ALL_ARGS = TEST_BASE_COLLABORATION_WITH_ALL_ARGS.copy {
    it.arn(TEST_COLLAB_ARN).id(TEST_COLLAB_ID)
}

val TEST_GET_COLLABORATION_RESPONSE = GetCollaborationResponse.builder()
    .collaboration(TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS)
    .build()

val TEST_MEMBER_SUMMARY = MemberSummary.builder()
    .accountId(TEST_MEMBER_ACCOUNT_ID)
    .displayName(TEST_MEMBER_DISPLAY_NAME)
    .status(MemberStatus.INVITED)
    .membershipArn("TestArn")
    .membershipId("TEST_MEMBER_ID")
    .abilities(MemberAbility.CAN_QUERY, MemberAbility.CAN_RECEIVE_RESULTS)
    .build()

val TEST_CREATOR_MEMBER_SUMMARY = TEST_MEMBER_SUMMARY.copy { it.accountId(TEST_CREATOR_ACCOUNT_ID) }

val TEST_COLLABORATION_SUMMARY_1 = CollaborationSummary.builder()
    .arn(TEST_COLLAB_ARN)
    .id(TEST_COLLAB_ID)
    .creatorAccountId(TEST_CREATOR_ACCOUNT_ID)
    .membershipId(TEST_MEMBER_ACCOUNT_ID)
    .creatorDisplayName(TEST_DISPLAY_NAME)
    .memberStatus(MemberStatus.ACTIVE)
    .name(TEST_COLLABORATION_NAME)
    .build()

val TEST_COLLABORATION_SUMMARY_2 = TEST_COLLABORATION_SUMMARY_1.copy { it.id("$TEST_COLLAB_ID-2").arn("$TEST_COLLAB_ARN-2") }

val TEST_BASE_COLLABORATION_RESOURCE_MODEL = TEST_COLLABORATION_RESPONSE_WITH_REQUIRED_FIELDS.toResourceModel(
    TEST_CREATOR_MEMBER_SUMMARY.toMemberSpecification()
)

val TEST_COLLABORATION_RESOURCE_MODEL_WITH_NEW_NAME = TEST_BASE_COLLABORATION_RESOURCE_MODEL.toBuilder()
    .name(TEST_COLLAB_NEW_NAME)
    .build()

val TEST_COLLABORATION_RESOURCE_MODEL_WITH_NEW_DESCRIPTION = TEST_BASE_COLLABORATION_RESOURCE_MODEL.toBuilder()
    .description(TEST_COLLAB_NEW_DESCRIPTION)
    .build()

val TEST_COLLABORATION_RESOURCE_MODEL_WITH_NEW_NAME_AND_DESCRIPTION = TEST_COLLABORATION_RESOURCE_MODEL_WITH_NEW_NAME.toBuilder()
    .description(TEST_COLLAB_NEW_DESCRIPTION)
    .build()

val TEST_COLLABORATION_RESOURCE_MODEL_WITH_NULL_NAME = TEST_BASE_COLLABORATION_RESOURCE_MODEL.toBuilder()
    .name(null)
    .build()

val TEST_COLLABORATION_RESOURCE_MODEL_WITH_NULL_DESCRIPTION = TEST_BASE_COLLABORATION_RESOURCE_MODEL.toBuilder()
    .description(null)
    .build()

val TEST_COLLABORATION_RESOURCE_MODEL_WITH_PREVIOUS_TAGS = TEST_BASE_COLLABORATION_RESOURCE_MODEL.toBuilder()
    .tags(TEST_COLLABORATION_TAGS.toResourceModelTags())
    .build()

val TEST_COLLABORATION_RESOURCE_MODEL_WITH_NEW_TAGS = TEST_BASE_COLLABORATION_RESOURCE_MODEL.toBuilder()
    .tags(TEST_COLLABORATION_NEW_TAGS.toResourceModelTags())
    .build()

val TEST_COLLABORATION_RESOURCE_MODEL_WITH_OLD_AND_NEW_TAGS = TEST_BASE_COLLABORATION_RESOURCE_MODEL.toBuilder()
    .tags((TEST_COLLABORATION_TAGS + TEST_COLLABORATION_NEW_TAGS).toResourceModelTags())
    .build()
