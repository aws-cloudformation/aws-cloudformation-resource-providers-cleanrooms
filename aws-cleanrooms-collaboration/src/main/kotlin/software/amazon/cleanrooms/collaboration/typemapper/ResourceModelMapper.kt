package software.amazon.cleanrooms.collaboration.typemapper

import software.amazon.awssdk.services.cleanrooms.model.MemberAbility
import software.amazon.cleanrooms.collaboration.ResourceModel
import software.amazon.awssdk.services.cleanrooms.model.MemberSpecification as SdkMemberSpecification

/**
 * Converts the Members in ResourceModel to the type supported by CleanRooms SDK.
 */
fun ResourceModel.toMembers(): List<SdkMemberSpecification> =
    members.map {
            memberSpecification ->
        SdkMemberSpecification.builder()
            .memberAbilities(
                memberSpecification.memberAbilities.map {
                    MemberAbility.fromValue(it)
                }
            )
            .displayName(memberSpecification.displayName)
            .accountId(memberSpecification.accountId)
            .build()
    }
