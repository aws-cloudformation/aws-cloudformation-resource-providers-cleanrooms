package software.amazon.cleanrooms.analysistemplate

import software.amazon.awssdk.services.cleanrooms.model.AnalysisTemplate
import software.amazon.awssdk.services.cleanrooms.model.AnalysisTemplateSummary
import software.amazon.awssdk.services.cleanrooms.model.GetAnalysisTemplateResponse
import software.amazon.awssdk.services.cleanrooms.model.AnalysisParameter as SdkAnalysisParameter
import software.amazon.awssdk.services.cleanrooms.model.AnalysisSchema as SdkAnalysisSchema
import software.amazon.awssdk.services.cleanrooms.model.AnalysisSource as SdkAnalysisSource

object TestData {
    const val TEST_AT_ARN = "arn"
    const val TEST_AT_ARN_2 = "arn2"
    const val TEST_AT_ID = "analysisTemplateId"
    const val TEST_AT_ID_2 = "analysisTemplateId2"
    const val TEST_CREATOR_ACCOUNT_ID = "TestCreatorAccountId"
    const val DESCRIPTION = "description"
    const val DESCRIPTION2 = "description2"
    const val TEST_MEMBERSHIP_ID = "membershipIdentifier"
    const val TEST_MEMBERSHIP_ARN = "membershipArn"
    const val TEST_COLLABORATION_ID = "collaborationId"
    const val TEST_COLLABORATION_ARN = "collaborationArn"
    const val TEST_NAME = "name"
    const val TEST_EXPECTED_NEXT_TOKEN = "TestExpectedNextToken"
    const val TEST_NEXT_TOKEN = "nextToken"

    val TEST_SYSTEM_TAGS = mapOf(
        "aws:cloudformation:stackname" to "testname",
        "aws:cloudformation:logicalid" to "testlogicalid",
        "aws:cloudformation:stackid" to "teststackid"
    )

    val TEST_TABLES = listOf("tables")
    val TEST_SDK_ANALYSIS_SCHEMA = SdkAnalysisSchema.builder()
        .referencedTables(TEST_TABLES)
        .build()
    const val TEST_SOURCE_TEXT = "query"
    val TEST_SDK_ANALYSIS_SOURCE = SdkAnalysisSource.builder()
        .text(TEST_SOURCE_TEXT)
        .build()
    const val TEST_FORMAT = "SQL"
    const val TEST_PARAM_NAME = "PARAM_NAME"
    const val TEST_PARAM_TYPE = "CHAR"
    const val TEST_DEFAULT_VALUE = "abc"
    val TEST_SDK_ANALYSIS_PARAMETER = SdkAnalysisParameter.builder()
        .name(TEST_PARAM_NAME)
        .type(TEST_PARAM_TYPE)
        .defaultValue(TEST_DEFAULT_VALUE)
        .build()
    val TEST_SDK_ANALYSIS_PARAMETER_NO_DEFAULT = SdkAnalysisParameter.builder()
        .name(TEST_PARAM_NAME)
        .type(TEST_PARAM_TYPE)
        .build()

    val TEST_ANALYSIS_SCHEMA = AnalysisSchema.builder()
        .referencedTables(TEST_TABLES)
        .build()
    val TEST_ANALYSIS_SOURCE = AnalysisSource.builder()
        .text(TEST_SOURCE_TEXT)
        .build()
    val TEST_ANALYSIS_PARAMETER = AnalysisParameter.builder()
        .name(TEST_PARAM_NAME)
        .type(TEST_PARAM_TYPE)
        .defaultValue(TEST_DEFAULT_VALUE)
        .build()
    val TEST_ANALYSIS_PARAMETER_NO_DEFAULT = AnalysisParameter.builder()
        .name(TEST_PARAM_NAME)
        .type(TEST_PARAM_TYPE)
        .build()

    val TEST_ANALYSIS_TEMPLATE_TAGS = mapOf("key1" to "value1", "key2" to "value2")
    val TEST_RESOURCE_MODEL_TAGS = TEST_ANALYSIS_TEMPLATE_TAGS.map { Tag.builder().key(it.key).value(it.value).build() }
    val TEST_UPDATED_ANALYSIS_TEMPLATE_TAGS = mapOf("key1" to "value2", "key2" to "value2")


    val TEST_ANALYSIS_TEMPLATE_BASE_WITH_REQUIRED_FIELDS: AnalysisTemplate =
        AnalysisTemplate.builder()
            .arn(TEST_AT_ARN)
            .membershipArn(TEST_MEMBERSHIP_ARN)
            .membershipId(TEST_MEMBERSHIP_ID)
            .collaborationId(TEST_COLLABORATION_ID)
            .collaborationArn(TEST_COLLABORATION_ARN)
            .name(TEST_NAME)
            .schema(TEST_SDK_ANALYSIS_SCHEMA)
            .source(TEST_SDK_ANALYSIS_SOURCE)
            .format(TEST_FORMAT)
            .build()



    val TEST_ANALYSIS_TEMPLATE_BASE_WITH_ALL_FIELDS: AnalysisTemplate =
        AnalysisTemplate.builder()
            .arn(TEST_AT_ARN)
            .membershipArn(TEST_MEMBERSHIP_ARN)
            .membershipId(TEST_MEMBERSHIP_ID)
            .collaborationId(TEST_COLLABORATION_ID)
            .collaborationArn(TEST_COLLABORATION_ARN)
            .name(TEST_NAME)
            .schema(TEST_SDK_ANALYSIS_SCHEMA)
            .source(TEST_SDK_ANALYSIS_SOURCE)
            .format(TEST_FORMAT)
            .description(DESCRIPTION)
            .analysisParameters(listOf(TEST_SDK_ANALYSIS_PARAMETER))
            .build()

    val TEST_UPDATE_DESCRIPTION_BASE_ANALYSIS_TEMPLATE: AnalysisTemplate =
        AnalysisTemplate.builder()
            .arn(TEST_AT_ARN)
            .membershipArn(TEST_MEMBERSHIP_ARN)
            .membershipId(TEST_MEMBERSHIP_ID)
            .collaborationId(TEST_COLLABORATION_ID)
            .collaborationArn(TEST_COLLABORATION_ARN)
            .name(TEST_NAME)
            .schema(TEST_SDK_ANALYSIS_SCHEMA)
            .source(TEST_SDK_ANALYSIS_SOURCE)
            .format(TEST_FORMAT)
            .description(DESCRIPTION2)
            .analysisParameters(listOf(TEST_SDK_ANALYSIS_PARAMETER))
            .build()

    val TEST_ANALYSIS_TEMPLATE_BASE_WITH_ALL_FIELDS_NO_DEFAULT_VALUE: AnalysisTemplate =
        AnalysisTemplate.builder()
            .arn(TEST_AT_ARN)
            .membershipArn(TEST_MEMBERSHIP_ARN)
            .membershipId(TEST_MEMBERSHIP_ID)
            .collaborationId(TEST_COLLABORATION_ID)
            .collaborationArn(TEST_COLLABORATION_ARN)
            .name(TEST_NAME)
            .schema(TEST_SDK_ANALYSIS_SCHEMA)
            .source(TEST_SDK_ANALYSIS_SOURCE)
            .format(TEST_FORMAT)
            .description(DESCRIPTION)
            .analysisParameters(listOf(TEST_SDK_ANALYSIS_PARAMETER_NO_DEFAULT))
            .build()



    val TEST_ANALYSIS_TEMPLATE_REQUIRED_FIELDS: AnalysisTemplate = TEST_ANALYSIS_TEMPLATE_BASE_WITH_REQUIRED_FIELDS.copy {
        it.id(TEST_AT_ID).arn(TEST_AT_ARN)
    }

    val TEST_ANALYSIS_TEMPLATE_WITH_ALL_FIELDS: AnalysisTemplate = TEST_ANALYSIS_TEMPLATE_BASE_WITH_ALL_FIELDS.copy {
        it.id(TEST_AT_ID).arn(TEST_AT_ARN)
    }

    val TEST_UPDATE_DESCRIPTION_ANALYSIS_TEMPLATE: AnalysisTemplate = TEST_UPDATE_DESCRIPTION_BASE_ANALYSIS_TEMPLATE.copy {
        it.id(TEST_AT_ID).arn(TEST_AT_ARN)
    }

    val TEST_ANALYSIS_TEMPLATE_WITH_ALL_FIELDS_NO_DEFAULT: AnalysisTemplate = TEST_ANALYSIS_TEMPLATE_BASE_WITH_ALL_FIELDS_NO_DEFAULT_VALUE.copy {
        it.id(TEST_AT_ID).arn(TEST_AT_ARN)
    }

    val TEST_CREATE_INPUT_RESOURCE_MODEL: ResourceModel =
        ResourceModel.builder()
            .arn(TEST_AT_ARN)
            .membershipArn(TEST_MEMBERSHIP_ARN)
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .collaborationIdentifier(TEST_COLLABORATION_ID)
            .collaborationArn(TEST_COLLABORATION_ARN)
            .name(TEST_NAME)
            .schema(TEST_ANALYSIS_SCHEMA)
            .source(TEST_ANALYSIS_SOURCE)
            .format(TEST_FORMAT)
            .analysisParameters(emptyList())
            .tags(emptyList())
            .build()

    val TEST_RESOURCE_MODEL_REQUIRED_FIELDS: ResourceModel =
        ResourceModel.builder()
            .arn(TEST_AT_ARN)
            .analysisTemplateIdentifier(TEST_AT_ID)
            .membershipArn(TEST_MEMBERSHIP_ARN)
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .collaborationIdentifier(TEST_COLLABORATION_ID)
            .collaborationArn(TEST_COLLABORATION_ARN)
            .name(TEST_NAME)
            .schema(TEST_ANALYSIS_SCHEMA)
            .source(TEST_ANALYSIS_SOURCE)
            .format(TEST_FORMAT)
            .analysisParameters(emptyList())
            .tags(emptyList())
            .build()

    val TEST_RESOURCE_MODEL_WITH_ALL_FIELDS: ResourceModel =
        ResourceModel.builder()
            .arn(TEST_AT_ARN)
            .analysisTemplateIdentifier(TEST_AT_ID)
            .membershipArn(TEST_MEMBERSHIP_ARN)
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .collaborationIdentifier(TEST_COLLABORATION_ID)
            .collaborationArn(TEST_COLLABORATION_ARN)
            .name(TEST_NAME)
            .schema(TEST_ANALYSIS_SCHEMA)
            .source(TEST_ANALYSIS_SOURCE)
            .format(TEST_FORMAT)
            .description(DESCRIPTION)
            .analysisParameters(listOf(TEST_ANALYSIS_PARAMETER))
            .tags(emptyList())
            .build()

    val TEST_RESOURCE_MODEL_WITH_ALL_FIELDS_NO_DEFAULT: ResourceModel =
        ResourceModel.builder()
            .arn(TEST_AT_ARN)
            .analysisTemplateIdentifier(TEST_AT_ID)
            .membershipArn(TEST_MEMBERSHIP_ARN)
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .collaborationIdentifier(TEST_COLLABORATION_ID)
            .collaborationArn(TEST_COLLABORATION_ARN)
            .name(TEST_NAME)
            .schema(TEST_ANALYSIS_SCHEMA)
            .source(TEST_ANALYSIS_SOURCE)
            .format(TEST_FORMAT)
            .description(DESCRIPTION)
            .analysisParameters(listOf(TEST_ANALYSIS_PARAMETER_NO_DEFAULT))
            .tags(emptyList())
            .build()


    val TEST_UPDATE_DESCRIPTION_RESOURCE_MODEL: ResourceModel =
        ResourceModel.builder()
            .arn(TEST_AT_ARN)
            .analysisTemplateIdentifier(TEST_AT_ID)
            .membershipArn(TEST_MEMBERSHIP_ARN)
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .collaborationIdentifier(TEST_COLLABORATION_ID)
            .collaborationArn(TEST_COLLABORATION_ARN)
            .name(TEST_NAME)
            .schema(TEST_ANALYSIS_SCHEMA)
            .source(TEST_ANALYSIS_SOURCE)
            .format(TEST_FORMAT)
            .description(DESCRIPTION2)
            .analysisParameters(listOf(TEST_ANALYSIS_PARAMETER))
            .tags(emptyList())
            .build()


    val TEST_RESOURCE_MODEL_REQUIRED_FIELDS_WITH_TAGS: ResourceModel =
        ResourceModel.builder()
            .arn(TEST_AT_ARN)
            .analysisTemplateIdentifier(TEST_AT_ID)
            .membershipArn(TEST_MEMBERSHIP_ARN)
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .collaborationIdentifier(TEST_COLLABORATION_ID)
            .collaborationArn(TEST_COLLABORATION_ARN)
            .name(TEST_NAME)
            .schema(TEST_ANALYSIS_SCHEMA)
            .source(TEST_ANALYSIS_SOURCE)
            .format(TEST_FORMAT)
            .analysisParameters(emptyList())
            .tags(TEST_RESOURCE_MODEL_TAGS)
            .build()

    val TEST_ANALYSIS_TEMPLATE_SUMMARY_1: AnalysisTemplateSummary =
        AnalysisTemplateSummary.builder()
            .id(TEST_AT_ID)
            .collaborationId(TEST_COLLABORATION_ID)
            .collaborationArn(TEST_COLLABORATION_ARN)
            .arn(TEST_AT_ARN_2)
            .name(TEST_NAME)
            .membershipArn(TEST_MEMBERSHIP_ARN)
            .membershipId(TEST_MEMBERSHIP_ID)
            .build()

    val TEST_ANALYSIS_TEMPLATE_SUMMARY_2: AnalysisTemplateSummary =
        AnalysisTemplateSummary.builder()
            .id(TEST_AT_ID_2)
            .collaborationId(TEST_COLLABORATION_ID)
            .collaborationArn(TEST_COLLABORATION_ARN)
            .arn(TEST_AT_ARN_2)
            .name(TEST_NAME)
            .membershipArn(TEST_MEMBERSHIP_ARN)
            .membershipId(TEST_MEMBERSHIP_ID)
            .build()

    val TEST_RESOURCE_MODEL_SUMMARY_1: ResourceModel =
        ResourceModel.builder()
            .analysisTemplateIdentifier(TEST_AT_ID)
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .build()

    val TEST_RESOURCE_MODEL_SUMMARY_2: ResourceModel =
        ResourceModel.builder()
            .analysisTemplateIdentifier(TEST_AT_ID_2)
            .membershipIdentifier(TEST_MEMBERSHIP_ID)
            .build()

    val AT_RESPONSE_OBJECT = AnalysisTemplate.builder()
        .arn(TEST_AT_ARN)
        .id(TEST_AT_ID)
        .membershipArn(TEST_MEMBERSHIP_ARN)
        .membershipId(TEST_MEMBERSHIP_ID)
        .collaborationId(TEST_COLLABORATION_ID)
        .collaborationArn(TEST_COLLABORATION_ARN)
        .name(TEST_NAME)
        .schema(TEST_SDK_ANALYSIS_SCHEMA)
        .source(TEST_SDK_ANALYSIS_SOURCE)
        .format(TEST_FORMAT)
        .build()

    val TEST_GET_AT_RESPONSE = GetAnalysisTemplateResponse.builder()
        .analysisTemplate(AT_RESPONSE_OBJECT)
        .build()
}
