package software.amazon.cleanrooms.collaboration.typemapper

import software.amazon.awssdk.services.cleanrooms.model.MemberSummary
import software.amazon.cleanrooms.collaboration.MemberSpecification

/**
 * Converting member summary list to a memberSpecification object from our resourceModel
 */
fun List<MemberSummary>.toMemberSpecification(): List<MemberSpecification> = mapNotNull {
    MemberSpecification(
        it.accountId(),
        it.abilitiesAsStrings().toSet(),
        it.displayName()
    )
}

fun MemberSummary.toMemberSpecification(): MemberSpecification = MemberSpecification(
    accountId(),
    abilitiesAsStrings().toSet(),
    displayName()
)
