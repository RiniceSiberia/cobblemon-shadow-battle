package io.github.rinicesiberia.shadowbattle.transport

import com.google.gson.JsonParser
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayerIdentityPayloadTest {
    @Test
    fun `player object keeps protocol field order and text`() {
        val uuid = UUID.fromString("12345678-1234-5678-9abc-def012345678")
        assertEquals("{\"uuid\":\"12345678-1234-5678-9abc-def012345678\",\"name\":\"玩家\"}", PlayerIdentityPayload.create(uuid, "玩家").toString())
    }

    @Test
    fun `special name is encoded as JSON text`() {
        val uuid = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val name = " 名\n\"x\" "
        assertEquals(name, JsonParser.parseString(PlayerIdentityPayload.create(uuid, name).toString()).asJsonObject.get("name").asString)
    }
}
