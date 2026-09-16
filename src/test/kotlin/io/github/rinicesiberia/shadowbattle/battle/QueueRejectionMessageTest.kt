package io.github.rinicesiberia.shadowbattle.battle

import net.minecraft.network.chat.Component
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import xiaocaoawa.minecraft.mod.cobblebattle.dex.RemoteDex
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg

class QueueRejectionMessageTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun loadMessages() {
            Msg.init("zh_cn")
        }
    }

    @Test
    fun `拒绝项保持输入顺序和一基槽位编号`() {
        val result = QueueRejectionMessage.compose(
            listOf(
                rejection(1, "甲", RemoteDex.Rejection.Kind.ILLEGAL_MOVE, "招式A"),
                rejection(0, "乙", RemoteDex.Rejection.Kind.UNKNOWN_SPECIES, "忽略")
            )
        ).string

        assertEquals(
            "你无法加入跨服对战:\n  [2][甲]: 学不会 招式A\n  [1][乙]: 对战服务端没有这个物种\n对战服务端的图鉴是唯一权威。请移除上面列出的宝可梦，或联系管理员同步服务端数据。",
            result
        )
    }

    @Test
    fun `所有带详情类型使用各自翻译模板`() {
        val result = QueueRejectionMessage.compose(
            listOf(
                rejection(0, "一", RemoteDex.Rejection.Kind.BASE_STAT_MISMATCH, "S"),
                rejection(1, "二", RemoteDex.Rejection.Kind.ILLEGAL_ABILITY, "A"),
                rejection(2, "三", RemoteDex.Rejection.Kind.EV_OVER_CAP, "E"),
                rejection(3, "四", RemoteDex.Rejection.Kind.IV_OVER_CAP, "I")
            )
        ).string

        assertEquals(true, result.contains("种族值与对战服务端不一致 (S)"))
        assertEquals(true, result.contains("不能拥有特性 A"))
        assertEquals(true, result.contains("努力值超上限（E）"))
        assertEquals(true, result.contains("个体值超上限（I）"))
    }

    private fun rejection(slot: Int, pokemon: String, kind: RemoteDex.Rejection.Kind, detail: String) =
        RemoteDex.Rejection(slot, Component.literal(pokemon), "species", kind, Component.literal(detail))
}
