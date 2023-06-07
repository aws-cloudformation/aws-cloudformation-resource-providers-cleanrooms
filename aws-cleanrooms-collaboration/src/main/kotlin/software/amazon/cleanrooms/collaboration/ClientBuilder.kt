package software.amazon.cleanrooms.collaboration

import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient

object ClientBuilder {
    fun getCleanRoomsClient(): CleanRoomsClient = CleanRoomsClient.create()
}
