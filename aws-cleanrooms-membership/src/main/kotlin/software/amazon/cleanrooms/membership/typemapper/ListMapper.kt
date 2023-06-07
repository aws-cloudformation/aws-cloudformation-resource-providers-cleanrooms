package software.amazon.cleanrooms.membership.typemapper

import software.amazon.awssdk.services.cleanrooms.model.MembershipSummary
import software.amazon.cleanrooms.membership.ResourceModel

fun List<MembershipSummary>.toResourceModels() = map {
    with(it) {
        ResourceModel.builder()
            .membershipIdentifier(id())
            .build()
    }
}
