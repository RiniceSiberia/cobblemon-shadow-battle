package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class QueueResponseDecodingTest {
    @Test
    fun `房间对战详情保持默认值和开战时空类型`() {
        assertEquals(
            QueueResponseDecoding.RoomBattleDetails("", "", "singles", false, true),
            QueueResponseDecoding.roomBattleDetails(JsonObject()),
        )

        val fighting = JsonObject().apply {
            addProperty("id", "room")
            addProperty("inviteCode", "invite")
            addProperty("battleType", "doubles")
            addProperty("fighting", true)
            addProperty("engine", "host")
            addProperty("legality", false)
        }
        assertEquals(
            QueueResponseDecoding.RoomBattleDetails("room", "invite", "", true, false),
            QueueResponseDecoding.roomBattleDetails(fighting),
        )
    }

    @Test
    fun `房间关闭和创建响应保留玩家及文本字段`() {
        val closed = QueueResponseDecoding.roomClosed(playerDocument().apply { addProperty("why", "timeout") })
        val created = QueueResponseDecoding.roomCreated(playerDocument().apply {
            addProperty("name", "room")
            addProperty("inviteCode", "code")
        })

        assertEquals(QueueResponseDecoding.RoomClosed(PARTICIPANT, "timeout"), closed)
        assertEquals(QueueResponseDecoding.RoomCreated(PARTICIPANT, "room", "code"), created)
    }

    @Test
    fun `排队响应分别采用原协议默认值和完整字段`() {
        assertEquals(
            QueueResponseDecoding.QueueAccepted(PARTICIPANT, 1, "", ""),
            QueueResponseDecoding.queueAccepted(playerDocument()),
        )
        assertEquals(
            QueueResponseDecoding.QueueDeparted(PARTICIPANT, false, ""),
            QueueResponseDecoding.queueDeparted(playerDocument()),
        )
        assertEquals(
            QueueResponseDecoding.QueuePosition(PARTICIPANT, 0, 0),
            QueueResponseDecoding.queuePosition(playerDocument()),
        )

        val full = playerDocument().apply {
            addProperty("waiting", 7)
            addProperty("rankedName", "ladder")
            addProperty("ranked", "ranked-id")
            addProperty("wasQueued", true)
            addProperty("why", "left")
            addProperty("position", 3)
        }
        assertEquals(QueueResponseDecoding.QueueAccepted(PARTICIPANT, 7, "ladder", "ranked-id"), QueueResponseDecoding.queueAccepted(full))
        assertEquals(QueueResponseDecoding.QueueDeparted(PARTICIPANT, true, "left"), QueueResponseDecoding.queueDeparted(full))
        assertEquals(QueueResponseDecoding.QueuePosition(PARTICIPANT, 3, 7), QueueResponseDecoding.queuePosition(full))
    }

    @Test
    fun `无效玩家和非数值等待位置继续抛出解析异常`() {
        assertThrows(IllegalArgumentException::class.java) {
            QueueResponseDecoding.roomCreated(JsonObject())
        }
        assertThrows(NumberFormatException::class.java) {
            QueueResponseDecoding.queuePosition(playerDocument().apply { addProperty("position", "bad") })
        }
    }

    private fun playerDocument(): JsonObject = JsonObject().apply { addProperty("player", PARTICIPANT.toString()) }

    private companion object {
        val PARTICIPANT: UUID = UUID.fromString("40000000-0000-0000-0000-000000000001")
    }
}
