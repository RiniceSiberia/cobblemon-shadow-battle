package io.github.rinicesiberia.shadowbattle.battle

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import xiaocaoawa.minecraft.mod.cobblebattle.dex.RemoteDex
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg

/** 将队伍合法性拒绝项按原顺序组合为玩家提示。 */
object QueueRejectionMessage {
    @JvmStatic
    fun compose(rejections: List<RemoteDex.Rejection>): Component {
        val message = Msg.of(ChatFormatting.RED, "reject.header").copy()
        for (rejection in rejections) {
            val detail = when (rejection.kind()) {
                RemoteDex.Rejection.Kind.UNKNOWN_SPECIES -> Msg.compose("reject.unknown_species")
                RemoteDex.Rejection.Kind.BASE_STAT_MISMATCH -> Msg.compose("reject.stat_mismatch", rejection.detail())
                RemoteDex.Rejection.Kind.ILLEGAL_ABILITY -> Msg.compose("reject.illegal_ability", rejection.detail())
                RemoteDex.Rejection.Kind.ILLEGAL_MOVE -> Msg.compose("reject.illegal_move", rejection.detail())
                RemoteDex.Rejection.Kind.EV_OVER_CAP -> Msg.compose("reject.ev_over_cap", rejection.detail())
                RemoteDex.Rejection.Kind.IV_OVER_CAP -> Msg.compose("reject.iv_over_cap", rejection.detail())
            }
            message.append(
                Msg.compose(ChatFormatting.YELLOW, "reject.slot", rejection.slot() + 1, rejection.pokemon())
                    .append(" ")
                    .append(detail)
            )
        }
        message.append(Msg.of(ChatFormatting.GRAY, "reject.footer"))
        return message
    }
}
