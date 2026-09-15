package io.github.rinicesiberia.shadowbattle.battle

/** 远端队伍压缩字段中可独立验证的附加信息。 */
data class PackedTeamDetails(
    val natureName: String?,
    val abilityName: String?,
    val moveNames: List<String>,
    val movePp: List<String?>,
    val friendship: String?,
    val teraName: String?,
)

/** 从压缩队伍的固定字段读取附加信息，保持原协议索引和数量限制。 */
object PackedTeamDetailsParser {
    @JvmStatic
    fun parse(fields: Array<String>): PackedTeamDetails {
        val misc = fields.getOrNull(16)?.split(",", limit = Int.MAX_VALUE).orEmpty()
        return PackedTeamDetails(
            natureName = fields.getOrNull(10)?.takeIf(String::isNotEmpty),
            abilityName = fields.getOrNull(7)?.takeIf(String::isNotEmpty),
            moveNames = fields.getOrNull(8)
                ?.takeIf(String::isNotEmpty)
                ?.split(",", limit = Int.MAX_VALUE)
                ?.take(4)
                .orEmpty(),
            movePp = parseMovePp(fields.getOrNull(9)),
            friendship = misc.getOrNull(0)?.takeIf(String::isNotEmpty),
            teraName = misc.getOrNull(5)?.takeIf(String::isNotEmpty),
        )
    }

    private fun parseMovePp(raw: String?): List<String?> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split(",", limit = Int.MAX_VALUE).take(4).map { entry ->
            if (entry.contains("/")) entry.substringBefore("/") else null
        }
    }

}
