package io.github.rinicesiberia.shadowbattle.battle

import com.cobblemon.mod.common.pokemon.Gender
import java.util.UUID

/** 解析远端队伍压缩字段中的基础值。 */
object PackedTeamValueParsing {
    @JvmStatic
    fun boundedInt(rawValue: String, fallback: Int, minimum: Int, maximum: Int): Int =
        try {
            Integer.parseInt(rawValue.trimJava()).coerceIn(minimum, maximum)
        } catch (_: NumberFormatException) {
            fallback
        }

    @JvmStatic
    fun uuidOrNull(rawValue: String): UUID? =
        try {
            UUID.fromString(rawValue)
        } catch (_: IllegalArgumentException) {
            null
        }

    @JvmStatic
    fun gender(rawValue: String): Gender =
        when (rawValue) {
            "M" -> Gender.MALE
            "F" -> Gender.FEMALE
            else -> Gender.GENDERLESS
        }
}

private fun String.trimJava(): String = trim { character -> character.code <= JAVA_TRIM_LIMIT }

private const val JAVA_TRIM_LIMIT = 0x20
