package software.amazon.cleanrooms.collaboration.typemapper

import software.amazon.awssdk.services.cleanrooms.model.CollaborationSummary
import software.amazon.cleanrooms.collaboration.ResourceModel

fun List<CollaborationSummary>.toResourceModels() = map {
    with(it) {
        ResourceModel.builder()
            .collaborationIdentifier(id())
            .build()
    }
}
