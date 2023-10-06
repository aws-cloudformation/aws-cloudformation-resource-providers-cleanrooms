package software.amazon.cleanrooms.membership.typemapper

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import software.amazon.awssdk.services.cleanrooms.model.MembershipProtectedQueryResultConfiguration
import software.amazon.cleanrooms.membership.ResourceModel
import kotlin.test.assertEquals

val expectedResultConfigurationWithAllArgs = """
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
                    """.trimIndent()

val expectedResultConfigurationWithoutRoleArn = """
                    {
                        "outputConfiguration": {
                            "s3": {
                                "resultFormat": "CSV",
                                "bucket": "testbucket",
                                "keyPrefix": "testkeyprefix"
                            }
                        }
                    }
                    """.trimIndent()

val expectedResultConfigurationWithoutRoleArnAndKeyPrefix = """
                    {
                        "outputConfiguration": {
                            "s3": {
                                "resultFormat": "PARQUET",
                                "bucket": "testbucket"

                            }
                        }
                    }
                    """.trimIndent()

val resourceModelWhereResultConfigHasAllProperties = """
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

val resourceModelWhereResultConfigWithoutRoleArn = """
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

val resourceModelWhereResultConfigWithoutRoleArnAndKeyPrefix = """
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
                "ResultFormat" : "PARQUET",
                "Bucket" : "testbucket"
              }
            }
          }
        }
    """.trimIndent()

class ResourceMapperTest {



    companion object {
        data class testInput(val inputResourceModel: String, val expectedResultConfiguration: String)

        @JvmStatic
        fun testInputs() = listOf(
            testInput(resourceModelWhereResultConfigHasAllProperties, expectedResultConfigurationWithAllArgs),
            testInput(resourceModelWhereResultConfigWithoutRoleArn, expectedResultConfigurationWithoutRoleArn),
            testInput(resourceModelWhereResultConfigWithoutRoleArnAndKeyPrefix, expectedResultConfigurationWithoutRoleArnAndKeyPrefix)
        )
    }


    @ParameterizedTest
    @MethodSource("testInputs")
    fun `test conversion of resultconfiguration from resourcemodel`(testInput: testInput){

        val resourceModel = OBJECT_MAPPER.readValue(testInput.inputResourceModel, ResourceModel::class.java)
        val  expectedResultConfiguration = OBJECT_MAPPER.readValue(testInput.expectedResultConfiguration, MembershipProtectedQueryResultConfiguration.serializableBuilderClass())
        val actual = resourceModel.toDefaultResultConfiguration()

        assertEquals(expectedResultConfiguration.build(), actual)
    }

}
