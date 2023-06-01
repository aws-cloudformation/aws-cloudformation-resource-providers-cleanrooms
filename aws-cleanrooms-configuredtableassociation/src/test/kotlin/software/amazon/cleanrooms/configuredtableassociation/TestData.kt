package software.amazon.cleanrooms.configuredtableassociation

import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAssociation
import software.amazon.awssdk.services.cleanrooms.model.ConfiguredTableAssociationSummary
import software.amazon.awssdk.services.cleanrooms.model.GetConfiguredTableAssociationResponse

object TestData {
    const val TEST_CTA_ARN = "arn"
    const val TEST_CTA_ARN_2 = "arn2"
    const val TEST_CT_ID = "configuredTableIdentifier"
    const val TEST_CT_ID_2 = "configuredTableIdentifier2j"
    const val TEST_CTA_ID = "configuredTableAssociationId"
    const val TEST_CTA_ID_2 = "configuredTableAssociationId2"
    const val TEST_CREATOR_ACCOUNT_ID = "TestCreatorAccountId"
    const val DESCRIPTION = "description"
    const val DESCRIPTION_2 = "description2"
    const val TEST_MEMBERSHIP_ID = "membershipIdentifier"
    const val TEST_NAME = "name"
    const val ROLE_ARN = "roleArn"
    const val ROLE_ARN_2 = "roleArn2"
    const val TEST_EXPECTED_NEXT_TOKEN = "TestExpectedNextToken"
    const val TEST_NEXT_TOKEN = "nextToken"

    val TEST_SYSTEM_TAGS = mapOf(
        "aws:cloudformation:stackname" to "testname",
        "aws:cloudformation:logicalid" to "testlogicalid",
        "aws:cloudformation:stackid" to "teststackid"
    )

    val TEST_CONFIGURED_TABLE_ASSOCIATION_TAGS = mapOf("key1" to "value1", "key2" to "value2")
    val TEST_RESOURCE_MODEL_TAGS = TEST_CONFIGURED_TABLE_ASSOCIATION_TAGS.map { Tag.builder().key(it.key).value(it.value).build() }
    val TEST_UPDATED_CONFIGURED_TABLE_ASSOCIATION_TAGS = mapOf("key1" to "value2", "key2" to "value2")


    val TEST_CONFIGURED_TABLE_ASSOCIATION_BASE_WITH_REQUIRED_FIELDS: ConfiguredTableAssociation =
        ConfiguredTableAssociation.builder()
            .arn(TEST_CTA_ARN)
            .configuredTableId(TEST_CT_ID).id(TEST_CTA_ID)
            .membershipId(TEST_MEMBERSHIP_ID)
            .name(TEST_NAME)
            .description(null)
            .roleArn(ROLE_ARN)
            .build()

    val TEST_CONFIGURED_TABLE_ASSOCIATION_BASE_WITH_DESCRIPTION: ConfiguredTableAssociation =
        ConfiguredTableAssociation.builder()
            .arn(TEST_CTA_ARN)
            .configuredTableId(TEST_CT_ID).id(TEST_CTA_ID)
            .membershipId(TEST_MEMBERSHIP_ID)
            .name(TEST_NAME)
            .description(DESCRIPTION)
            .roleArn(ROLE_ARN)
            .build()

    val TEST_CONFIGURED_TABLE_ASSOCIATION_REQUIRED_FIELDS: ConfiguredTableAssociation = TEST_CONFIGURED_TABLE_ASSOCIATION_BASE_WITH_REQUIRED_FIELDS.copy {
        it.id(TEST_CTA_ID).arn(TEST_CTA_ARN)
    }

    val TEST_CONFIGURED_TABLE_ASSOCIATION_WITH_DESCRIPTION_FIELD: ConfiguredTableAssociation = TEST_CONFIGURED_TABLE_ASSOCIATION_BASE_WITH_DESCRIPTION.copy {
        it.id(TEST_CTA_ID).arn(TEST_CTA_ARN)
    }

    val TEST_CONFIGURED_TABLE_ASSOCIATION_UPDATED_ROLE_ARN: ConfiguredTableAssociation = TEST_CONFIGURED_TABLE_ASSOCIATION_BASE_WITH_REQUIRED_FIELDS.copy {
        it.id(TEST_CTA_ID).arn(TEST_CTA_ARN).roleArn(ROLE_ARN_2)
    }

    val TEST_RESOURCE_MODEL_REQUIRED_FIELDS: ResourceModel =
        ResourceModel.builder()
            .arn(TEST_CTA_ARN)
            .configuredTableIdentifier(TEST_CT_ID)
            .configuredTableAssociationIdentifier(TEST_CTA_ID)
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .name(TEST_NAME)
            .description(null)
            .roleArn(ROLE_ARN)
            .tags(emptyList())
            .build()

    val TEST_RESOURCE_MODEL_WITH_DESCRIPTION: ResourceModel =
        ResourceModel.builder()
            .arn(TEST_CTA_ARN)
            .configuredTableIdentifier(TEST_CT_ID)
            .configuredTableAssociationIdentifier(TEST_CTA_ID)
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .name(TEST_NAME)
            .description(DESCRIPTION)
            .roleArn(ROLE_ARN)
            .tags(emptyList())
            .build()

    val TEST_UPDATE_ROLE_ARN_RESOURCE_MODEL: ResourceModel  =
        ResourceModel.builder()
            .arn(TEST_CTA_ARN)
            .configuredTableIdentifier(TEST_CT_ID)
            .configuredTableAssociationIdentifier(TEST_CTA_ID)
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .name(TEST_NAME)
            .roleArn(ROLE_ARN_2)
            .tags(emptyList())
            .build()


    val TEST_UPDATE_DESCRIPTION_RESOURCE_MODEL: ResourceModel  =
        ResourceModel.builder()
            .arn(TEST_CTA_ARN)
            .configuredTableIdentifier(TEST_CT_ID)
            .configuredTableAssociationIdentifier(TEST_CTA_ID)
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .name(TEST_NAME)
            .description(DESCRIPTION)
            .roleArn(ROLE_ARN)
            .tags(emptyList())
            .build()


    val TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS: ResourceModel =
        ResourceModel.builder()
            .arn(TEST_CTA_ARN)
            .configuredTableIdentifier(TEST_CT_ID)
            .configuredTableAssociationIdentifier(TEST_CTA_ID)
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .name(TEST_NAME)
            .roleArn(ROLE_ARN)
            .tags(TEST_RESOURCE_MODEL_TAGS)
            .build()

    val TEST_CONFIGURED_TABLE_ASSOCIATION_SUMMARY_1: ConfiguredTableAssociationSummary =
        ConfiguredTableAssociationSummary.builder()
            .id(TEST_CTA_ID)
            .configuredTableId(TEST_CT_ID)
            .arn(TEST_CTA_ARN)
            .name(TEST_NAME)
            .membershipId(TEST_MEMBERSHIP_ID)
            .build()

    val TEST_CONFIGURED_TABLE_ASSOCIATION_SUMMARY_2: ConfiguredTableAssociationSummary =
        ConfiguredTableAssociationSummary.builder()
            .id(TEST_CTA_ID_2)
            .configuredTableId(TEST_CT_ID_2)
            .arn(TEST_CTA_ARN_2)
            .name(TEST_NAME)
            .membershipId(TEST_MEMBERSHIP_ID)
            .build()

    val TEST_RESOURCE_MODEL_SUMMARY_1: ResourceModel =
        ResourceModel.builder()
            .configuredTableAssociationIdentifier(TEST_CTA_ID)
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .build()

    val TEST_RESOURCE_MODEL_SUMMARY_2: ResourceModel =
        ResourceModel.builder()
            .configuredTableAssociationIdentifier(TEST_CTA_ID_2)
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .build()

    val CTA_RESPONSE_OBJECT = ConfiguredTableAssociation.builder()
        .arn(TEST_CTA_ARN)
        .configuredTableId(TEST_CT_ID)
        .id(TEST_CTA_ID)
        .description(DESCRIPTION)
        .membershipId(TEST_MEMBERSHIP_ID)
        .name(TEST_NAME)
        .roleArn(ROLE_ARN)
        .build()

    val TEST_GET_CTA_RESPONSE = GetConfiguredTableAssociationResponse.builder()
        .configuredTableAssociation(CTA_RESPONSE_OBJECT)
        .build()
}
