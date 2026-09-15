package io.github.rinicesiberia.shadowbattle.command

/** 排位参数解析后的执行信息。 */
internal data class RankedSelection(
    val chosenId: String,
    val accepted: Boolean,
    val offeredIds: String,
)

/** 统一命令参数默认值和服务端排位清单校验。 */
internal object CommandDecisions {
    fun rankedSelection(
        requestedId: String?,
        defaultId: String,
        availableIds: List<String>,
    ): RankedSelection {
        val chosenId = requestedId ?: defaultId
        return RankedSelection(
            chosenId = chosenId,
            accepted = availableIds.isEmpty() || chosenId in availableIds,
            offeredIds = availableIds.joinToString(", "),
        )
    }

    fun reloadFollowUp(changedConnectionFields: Collection<String>, refusal: String?): ReloadFollowUp = when {
        changedConnectionFields.isNotEmpty() -> ReloadFollowUp.RECONNECT
        refusal != null -> ReloadFollowUp.RETRY
        else -> ReloadFollowUp.NONE
    }
}

/** 配置重载完成后对远端连接采取的动作。 */
internal enum class ReloadFollowUp {
    NONE,
    RECONNECT,
    RETRY,
}
