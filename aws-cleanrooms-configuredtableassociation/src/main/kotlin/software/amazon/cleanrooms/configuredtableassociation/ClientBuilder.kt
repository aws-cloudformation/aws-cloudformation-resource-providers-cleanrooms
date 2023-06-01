package software.amazon.cleanrooms.configuredtableassociation

import software.amazon.awssdk.services.cleanrooms.CleanRoomsClient

object ClientBuilder {
    fun getCleanRoomsClient(): CleanRoomsClient = CleanRoomsClient.create()
}
