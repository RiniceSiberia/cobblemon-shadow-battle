package io.github.rinicesiberia.shadowbattle.configuration

import com.google.gson.*
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/** 将安全加载的 YAML 映射转换为配置对象所需的 JSON 树。 */
object ConfigurationDocument {
    @JvmStatic
    fun decode(source: String): JsonObject {
        val loaded: Any = Yaml(SafeConstructor(LoaderOptions())).load<Any?>(source) ?: return JsonObject()
        val document = encodeValue(loaded)
        require(document.isJsonObject) { "the top level of cobblebattle.yml must be a mapping" }
        return document.asJsonObject
    }

    private fun encodeValue(value: Any?): JsonElement = when (value) {
        null -> JsonNull.INSTANCE
        is Map<*, *> -> JsonObject().apply {
            value.forEach { (key, child) -> add(java.lang.String.valueOf(key), encodeValue(child)) }
        }
        is List<*> -> JsonArray().apply { value.forEach { add(encodeValue(it)) } }
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        else -> JsonPrimitive(java.lang.String.valueOf(value))
    }
}

