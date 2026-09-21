package io.github.rinicesiberia.shadowbattle.showdown

/** 官方 Pokémon Showdown 文本协议的最小安全命令集。 */
object ShowdownProtocol {
    fun login(username: String, assertion: String) = "|/trn ${safe(username)},0,$assertion"
    fun uploadTeam(packedTeam: String) = "|/utm $packedTeam"
    fun validate(format: String) = "|/vtm ${ShowdownIdentifiers.formatId(format)}"
    fun search(format: String) = "|/search ${ShowdownIdentifiers.formatId(format)}"
    fun cancelSearch() = "|/cancelsearch"
    fun challenge(username: String, format: String) = "|/challenge ${safe(username)}, ${ShowdownIdentifiers.formatId(format)}"
    fun accept(username: String) = "|/accept ${safe(username)}"
    fun reject(username: String) = "|/reject ${safe(username)}"
    fun choose(roomId: String, choice: String, requestId: Int? = null) =
        "$roomId|/choose $choice" + (requestId?.let { "|$it" } ?: "")
    fun forfeit(roomId: String) = "$roomId|/forfeit"

    private fun safe(value: String): String = value.replace("|", "").replace("\n", "").replace("\r", "").trim()
}

data class ShowdownFrame(val roomId: String?, val lines: List<String>)

object ShowdownFrameParser {
    fun parse(payload: String): ShowdownFrame {
        val lines = payload.lineSequence().filter(String::isNotEmpty).toList()
        val roomId = lines.firstOrNull()?.takeIf { it.startsWith(">") }?.removePrefix(">").orEmpty().ifBlank { null }
        return ShowdownFrame(roomId, if (roomId == null) lines else lines.drop(1))
    }
}
