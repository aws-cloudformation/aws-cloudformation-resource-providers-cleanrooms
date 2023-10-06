package software.amazon.cleanrooms.membership.typemapper

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import software.amazon.awssdk.services.cleanrooms.model.Membership
import software.amazon.cleanrooms.membership.MembershipProtectedQueryResultConfiguration
import software.amazon.cleanrooms.membership.ResourceModel
import kotlin.test.assertEquals
import software.amazon.awssdk.services.cleanrooms.model.MembershipProtectedQueryResultConfiguration as SdkResultConfiguration

val OBJECT_MAPPER = ObjectMapper()

val expectedResourceModelWithoutResultConfig = """
      {
        "Arn" : "arn:aws:cleanrooms:us-east-1:123456789012:membership/5ac65a10-dde7-4916-890e-3abc51e07839",
        "Tags" : [ ],
        "CollaborationArn" : "arn:aws:cleanrooms:us-east-1:123456789012:collaboration/fbc58dc6-7d91-4e53-b75e-23275d50a032",
        "CollaborationCreatorAccountId" : "TestCollabCreatorAccountId",
        "CollaborationIdentifier" : "TestCollaborationId",
        "MembershipIdentifier" : "TestMembershipId",
        "QueryLogStatus" : "ENABLED"
      }
  """.trimIndent()

val expectedResourceModelWhereResultConfigHasAllProperties = """
        {
          "Arn" : "arn:aws:cleanrooms:us-east-1:123456789012:membership/5ac65a10-dde7-4916-890e-3abc51e07839",
          "Tags" : [ ],
          "CollaborationArn" : "arn:aws:cleanrooms:us-east-1:123456789012:collaboration/fbc58dc6-7d91-4e53-b75e-23275d50a032",
          "CollaborationCreatorAccountId" : "TestCollabCreatorAccountId",
          "CollaborationIdentifier" : "TestCollaborationId",
          "MembershipIdentifier" : "TestMembershipId",
          "QueryLogStatus" : "ENABLED",
          "DefaultResultConfiguration" : {
            "OutputConfiguration" : {
              "S3" : {
                "ResultFormat" : "CSV",
                "Bucket" : "testbucket",
                "KeyPrefix" : "testkeyprefix"
              }
            },
            "RoleArn" : "0000000000000000000000000:role/TestRole"
          }
        }
        """.trimIndent()

val expectedResourceModelWhereResultConfigWithoutRoleArn = """
        {
          "Arn" : "arn:aws:cleanrooms:us-east-1:123456789012:membership/5ac65a10-dde7-4916-890e-3abc51e07839",
          "Tags" : [ ],
          "CollaborationArn" : "arn:aws:cleanrooms:us-east-1:123456789012:collaboration/fbc58dc6-7d91-4e53-b75e-23275d50a032",
          "CollaborationCreatorAccountId" : "TestCollabCreatorAccountId",
          "CollaborationIdentifier" : "TestCollaborationId",
          "MembershipIdentifier" : "TestMembershipId",
          "QueryLogStatus" : "ENABLED",
          "DefaultResultConfiguration" : {
            "OutputConfiguration" : {
              "S3" : {
                "ResultFormat" : "CSV",
                "Bucket" : "testbucket",
                "KeyPrefix" : "testkeyprefix"
              }
            }
          }
        }
    """.trimIndent()

val expectedResourceModelWhereResultConfigWithoutRoleArnAndKeyPrefix = """
        {
          "Arn" : "arn:aws:cleanrooms:us-east-1:123456789012:membership/5ac65a10-dde7-4916-890e-3abc51e07839",
          "Tags" : [ ],
          "CollaborationArn" : "arn:aws:cleanrooms:us-east-1:123456789012:collaboration/fbc58dc6-7d91-4e53-b75e-23275d50a032",
          "CollaborationCreatorAccountId" : "TestCollabCreatorAccountId",
          "CollaborationIdentifier" : "TestCollaborationId",
          "MembershipIdentifier" : "TestMembershipId",
          "QueryLogStatus" : "ENABLED",
          "DefaultResultConfiguration" : {
            "OutputConfiguration" : {
              "S3" : {
                "ResultFormat" : "CSV",
                "Bucket" : "testbucket"
              }
            }
          }
        }
    """.trimIndent()

val baseMembership = """
    {
      "id" : "TestMembershipId",
       "arn" : "arn:aws:cleanrooms:us-east-1:123456789012:membership/5ac65a10-dde7-4916-890e-3abc51e07839",
      "collaborationArn" : "arn:aws:cleanrooms:us-east-1:123456789012:collaboration/fbc58dc6-7d91-4e53-b75e-23275d50a032",
      "collaborationId" : "TestCollaborationId",
      "collaborationCreatorAccountId" : "TestCollabCreatorAccountId",
      "collaborationCreatorDisplayName" : "TestCollabCreatorName",
      "collaborationName" : "TestCollabName",
      "queryLogStatus" : "ENABLED"
    }
""".trimIndent()



class MembershipMapperTest {

    companion object{
        data class testInput(
            val baseMembershipAsString : String = baseMembership,
            val resultConfigAsString: String? = "null",
            val expectedResourceModelAsString : String
        )

        @JvmStatic
        fun provideTestData() : List<testInput> {
            return listOf(
                testInput(
                    expectedResourceModelAsString = expectedResourceModelWithoutResultConfig
                    ),
            testInput(
                resultConfigAsString = """
                    {
                        "outputConfiguration": {
                            "s3": {
                                "resultFormat": "CSV",
                                "bucket": "testbucket",
                                "keyPrefix": "testkeyprefix"
                            }
                        },
                        "roleArn": "0000000000000000000000000:role/TestRole"
                    }
                    """.trimIndent(),
                expectedResourceModelAsString = expectedResourceModelWhereResultConfigHasAllProperties)
            ,
            testInput(
                resultConfigAsString = """
                    {
                        "outputConfiguration": {
                            "s3": {
                                "resultFormat": "CSV",
                                "bucket": "testbucket",
                                "keyPrefix": "testkeyprefix"
                            }
                        }
                    }
                    """.trimIndent(),
                expectedResourceModelAsString = expectedResourceModelWhereResultConfigWithoutRoleArn),
                testInput(
                    resultConfigAsString = """
                    {
                        "outputConfiguration": {
                            "s3": {
                                "resultFormat": "CSV",
                                "bucket": "testbucket"
                            }
                        }
                    }
                    """.trimIndent(),
                    expectedResourceModelAsString = expectedResourceModelWhereResultConfigWithoutRoleArnAndKeyPrefix)
            )
        }
    }




    @ParameterizedTest
    @MethodSource("provideTestData")
    fun `test membership toResourceModel converts successfully`(testData: testInput) {
        val baseMembership = OBJECT_MAPPER.readValue(testData.baseMembershipAsString, Membership.serializableBuilderClass())
        val resultConfig = OBJECT_MAPPER.readValue(testData.resultConfigAsString, SdkResultConfiguration.serializableBuilderClass())?.build()
        val membership : Membership = resultConfig?.let{ baseMembership.apply {
            defaultResultConfiguration(it)
        }.build()} ?: baseMembership.build()

        val expectedResourceModel : ResourceModel = OBJECT_MAPPER.readValue(testData.expectedResourceModelAsString, ResourceModel::class.java)

        val actual = membership.toResourceModel()

        assertEquals(expectedResourceModel, actual)
    }






}
