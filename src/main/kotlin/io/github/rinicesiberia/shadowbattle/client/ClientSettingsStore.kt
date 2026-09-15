package io.github.rinicesiberia.shadowbattle.client

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer
import java.util.function.Supplier

/** 保存客户端本地偏好，并保持加载失败后的稳定回退状态。 */
class ClientSettingsStore(
    private val fileLocation: Supplier<Path>,
    private val warningSink: Consumer<String>,
) {
    private var loaded = false
    private var chatHudEnabled = true

    fun chatHudEnabled(): Boolean {
        loadOnce()
        return chatHudEnabled
    }

    fun updateChatHud(enabled: Boolean) {
        loadOnce()
        chatHudEnabled = enabled
        save()
    }

    private fun loadOnce() {
        if (loaded) return
        loaded = true
        try {
            val file = fileLocation.get()
            if (!Files.exists(file)) return
            val root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).asJsonObject
            if (root.has(CHAT_HUD_KEY)) chatHudEnabled = root[CHAT_HUD_KEY].asBoolean
        } catch (failure: Exception) {
            warningSink.accept("Could not read client settings: $failure")
        }
    }

    private fun save() {
        try {
            val file = fileLocation.get()
            Files.createDirectories(file.parent)
            val root = JsonObject().also { it.addProperty(CHAT_HUD_KEY, chatHudEnabled) }
            Files.writeString(file, root.toString(), StandardCharsets.UTF_8)
        } catch (failure: Exception) {
            warningSink.accept("Could not save client settings: $failure")
        }
    }

    private companion object {
        const val CHAT_HUD_KEY = "chatHud"
    }
}
