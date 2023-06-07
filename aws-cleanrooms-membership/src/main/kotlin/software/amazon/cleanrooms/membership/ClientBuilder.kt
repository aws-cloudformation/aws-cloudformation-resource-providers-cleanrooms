package software.amazon.cleanrooms.membership

import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient
import software.amazon.cloudformation.LambdaWrapper

object ClientBuilder {
    fun getCleanRoomsClient(): CleanRoomsClient = CleanRoomsClient.create()
}
