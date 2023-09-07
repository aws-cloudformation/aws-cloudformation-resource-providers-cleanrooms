package software.amazon.cleanrooms.analysistemplate

import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient

object ClientBuilder {
    fun getCleanRoomsClient(): CleanRoomsClient = CleanRoomsClient.create()
}
