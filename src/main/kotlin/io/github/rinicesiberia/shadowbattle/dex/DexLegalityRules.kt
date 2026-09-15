package io.github.rinicesiberia.shadowbattle.dex

import java.util.Locale

/** 处理图鉴合法性检查中与游戏对象无关的名称和数值规则。 */
object DexLegalityRules {
    @JvmStatic
    fun normalizedId(raw: String?): String = raw
        ?.lowercase(Locale.ROOT)
        ?.replace(NON_ALPHANUMERIC, "")
        ?: ""

    @JvmStatic
    fun abilityAllowed(selectedName: String?, availableNames: Collection<String?>): Boolean {
        val selectedId = normalizedId(selectedName)
        if (selectedId.isEmpty() || selectedId == "noability") return true
        return availableNames.any { normalizedId(it) == selectedId }
    }

    @JvmStatic
    fun legalMoveIds(moveNames: Collection<String?>): Set<String> = moveNames
        .mapTo(LinkedHashSet(), ::normalizedId)

    @JvmStatic
    fun moveAllowed(moveName: String?, legalMoveIds: Set<String>): Boolean {
        val moveId = normalizedId(moveName)
        return moveId.isEmpty() || legalMoveIds.isEmpty() || moveId in legalMoveIds
    }

    @JvmStatic
    fun nonNegativeLimit(configured: Int): Int = configured.coerceAtLeast(0)

    @JvmStatic
    fun exceedsLimit(value: Int, maximum: Int): Boolean = maximum > 0 && value > maximum

    private val NON_ALPHANUMERIC = Regex("[^a-z0-9]")
}
