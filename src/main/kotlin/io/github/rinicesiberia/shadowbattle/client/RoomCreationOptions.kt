package io.github.rinicesiberia.shadowbattle.client

/** 创建房间时提交给网络层的规范化参数。 */
data class RoomCreationRequest(
    val name: String,
    val password: String,
    val battleType: String,
    val level: Int,
    val pick: Int,
    val fullHeal: Boolean,
    val hostEngine: Boolean,
    val legality: Boolean,
)

/** 管理创建房间表单的循环选项与开关状态。 */
class RoomCreationOptions {
    private var battleTypeIndex = 0
    private var levelIndex = 0
    private var pickIndex = 0
    private var fullHeal = true
    private var hostEngine = false
    private var legality = true

    fun battleType(): String = BATTLE_TYPES[battleTypeIndex]

    fun level(): Int = LEVEL_CAPS[levelIndex]

    fun pick(): Int = PICK_COUNTS[pickIndex]

    fun fullHeal(): Boolean = fullHeal

    fun hostEngine(): Boolean = hostEngine

    fun legality(): Boolean = legality

    fun passwordRow(): Int = if (hostEngine) HOST_PASSWORD_ROW else SERVER_PASSWORD_ROW

    fun cycleBattleType(step: Int) {
        battleTypeIndex = Math.floorMod(battleTypeIndex + step, BATTLE_TYPES.size)
    }

    fun cycleLevel(step: Int) {
        levelIndex = Math.floorMod(levelIndex + step, LEVEL_CAPS.size)
    }

    fun cyclePick(step: Int) {
        pickIndex = Math.floorMod(pickIndex + step, PICK_COUNTS.size)
    }

    fun toggleFullHeal() {
        fullHeal = !fullHeal
    }

    fun toggleHostEngine() {
        hostEngine = !hostEngine
    }

    fun toggleLegality() {
        legality = !legality
    }

    fun createRequest(rawName: String, rawPassword: String, defaultName: String): RoomCreationRequest {
        val normalizedName = rawName.trimJava().ifEmpty { defaultName }
        return RoomCreationRequest(
            name = normalizedName,
            password = rawPassword.trimJava(),
            battleType = battleType(),
            level = level(),
            pick = pick(),
            fullHeal = fullHeal,
            hostEngine = hostEngine,
            legality = legality,
        )
    }

    private companion object {
        val BATTLE_TYPES = listOf("singles", "doubles", "triples")
        val LEVEL_CAPS = listOf(-1, 50, 100)
        val PICK_COUNTS = listOf(6, 3, 4)
        const val SERVER_PASSWORD_ROW = 6
        const val HOST_PASSWORD_ROW = 7
    }
}

/** 房间大厅中与界面组件无关的输入规则。 */
object RoomLobbyRules {
    @JvmStatic
    fun invitationCode(rawCode: String): String? = rawCode.trimJava().ifEmpty { null }

    @JvmStatic
    fun seatType(fighting: Boolean, battleType: String): String = if (fighting) "" else battleType
}

private fun String.trimJava(): String = trim { character -> character.code <= JAVA_TRIM_LIMIT }

private const val JAVA_TRIM_LIMIT = 0x20
