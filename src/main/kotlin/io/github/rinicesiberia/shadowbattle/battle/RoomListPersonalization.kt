package io.github.rinicesiberia.shadowbattle.battle

import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomListPayload

/** 为接收者标记自有房间，并生成用于抑制重复发送的摘要。 */
object RoomListPersonalization {
    data class Result(val rooms: List<RoomListPayload.Room>, val deliveryStamp: String)

    @JvmStatic
    fun apply(rooms: List<RoomListPayload.Room>, hash: String, accountNumber: Long): Result {
        var hasOwnedRoom = false
        val personalized = ArrayList<RoomListPayload.Room>(rooms.size)
        for (room in rooms) {
            val owned = accountNumber != 0L && room.hostUid() == accountNumber
            hasOwnedRoom = hasOwnedRoom || owned
            personalized += if (owned) room.withOwnership() else room
        }
        return Result(personalized, hash + if (hasOwnedRoom) ":own" else "")
    }

    private fun RoomListPayload.Room.withOwnership(): RoomListPayload.Room = RoomListPayload.Room(
        id(), name(), host(), hostUid(), battleType(), level(), pick(), fullHeal(), locked(), lead(),
        hasGuest(), watchers(), true, hostEngine(), fighting(), legality()
    )
}
