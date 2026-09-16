package io.github.rinicesiberia.shadowbattle.transport

import com.google.gson.JsonObject
import java.util.UUID

/** 创建排队协议使用的玩家对象，保持 uuid/name 字段顺序。 */
object QueuePlayerPayload {
    @JvmStatic
    fun create(uuid: UUID, name: String): JsonObject = JsonObject().apply {
        addProperty("uuid", uuid.toString())
        addProperty("name", name)
    }
}
