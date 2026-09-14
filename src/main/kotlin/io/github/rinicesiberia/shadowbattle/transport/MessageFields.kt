package io.github.rinicesiberia.shadowbattle.transport

import com.google.gson.JsonObject

/** 对战服务消息的类型字段和可选标量读取。 */
object MessageFields {
    @JvmStatic fun envelope(kind: String): JsonObject = JsonObject().apply { addProperty("t", kind) }
    @JvmStatic fun text(document: JsonObject, key: String, defaultValue: String?): String? =
        document[key]?.takeUnless { it.isJsonNull }?.asString ?: defaultValue
    @JvmStatic fun number(document: JsonObject, key: String, defaultValue: Int): Int =
        document[key]?.takeUnless { it.isJsonNull }?.asInt ?: defaultValue
    @JvmStatic fun flag(document: JsonObject, key: String, defaultValue: Boolean): Boolean =
        document[key]?.takeUnless { it.isJsonNull }?.asBoolean ?: defaultValue
    @JvmStatic fun longNumber(document: JsonObject, key: String, defaultValue: Long): Long =
        document[key]?.takeUnless { it.isJsonNull }?.asLong ?: defaultValue
}
