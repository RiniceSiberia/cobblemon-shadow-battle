package io.github.rinicesiberia.shadowbattle.client

/** 客户端收到状态消息后对当前界面采取的动作。 */
internal enum class ViewTransition {
    REFRESH,
    OPEN,
    IGNORE,
}

/** 集中描述房间和队伍预览消息的界面切换规则。 */
internal object ClientViewTransitions {
    fun roomList(
        hasPlayer: Boolean,
        lobbyVisible: Boolean,
        refreshOnly: Boolean,
        battleActive: Boolean,
    ): ViewTransition = when {
        !hasPlayer -> ViewTransition.IGNORE
        lobbyVisible -> ViewTransition.REFRESH
        !refreshOnly && !battleActive -> ViewTransition.OPEN
        else -> ViewTransition.IGNORE
    }

    fun roomState(
        hasPlayer: Boolean,
        visibleRoomId: String?,
        incomingRoomId: String,
        battleActive: Boolean,
    ): ViewTransition = when {
        !hasPlayer -> ViewTransition.IGNORE
        visibleRoomId == incomingRoomId -> ViewTransition.REFRESH
        !battleActive -> ViewTransition.OPEN
        else -> ViewTransition.IGNORE
    }

    fun teamPreview(
        hasPlayer: Boolean,
        visibleBattleId: String?,
        incomingBattleId: String,
        closedReason: String,
    ): ViewTransition = when {
        !hasPlayer -> ViewTransition.IGNORE
        visibleBattleId == incomingBattleId -> ViewTransition.REFRESH
        closedReason.isEmpty() -> ViewTransition.OPEN
        else -> ViewTransition.IGNORE
    }
}
