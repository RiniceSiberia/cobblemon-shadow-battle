package xiaocaoawa.minecraft.mod.cobblebattle.network

import io.netty.buffer.Unpooled
import net.minecraft.core.RegistryAccess
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.connection.ConnectionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class PayloadCodecContractTest {
    @Test
    fun `全部游戏包 ID 保持兼容`() {
        val actualIds = listOf(
            AuthResultPayload.TYPE,
            ChatLinePayload.TYPE,
            ChatStatePayload.TYPE,
            LeaderboardPayload.TYPE,
            MenuActionPayload.TYPE,
            OpenAuthScreenPayload.TYPE,
            OpenMainMenuPayload.TYPE,
            OpenPagePayload.TYPE,
            RoomActionPayload.TYPE,
            RoomListPayload.TYPE,
            RoomStatePayload.TYPE,
            SendChatPayload.TYPE,
            ServerDexPayload.TYPE,
            SubmitAuthPayload.TYPE,
            TeamPickPayload.TYPE,
            TeamPreviewPayload.TYPE,
        ).map { it.id().toString() }

        assertEquals(
            listOf(
                "cobblebattle:auth_result",
                "cobblebattle:chat_line",
                "cobblebattle:chat_state",
                "cobblebattle:leaderboard",
                "cobblebattle:menu_action",
                "cobblebattle:open_auth",
                "cobblebattle:open_main_menu",
                "cobblebattle:open_page",
                "cobblebattle:room_action",
                "cobblebattle:room_list",
                "cobblebattle:room_state",
                "cobblebattle:send_chat",
                "cobblebattle:server_dex",
                "cobblebattle:submit_auth",
                "cobblebattle:team_pick",
                "cobblebattle:team_preview",
            ),
            actualIds,
        )
    }

    @Test
    fun `认证聊天与菜单包按原字段往返`() {
        assertRoundTrip(AuthResultPayload.CODEC, AuthResultPayload(false, "验证码错误"))
        assertRoundTrip(
            ChatLinePayload.CODEC,
            ChatLinePayload("battle", 9_223_372_036L, "id-7", "玩家甲", FIXED_UUID, "你好，世界"),
        )
        assertRoundTrip(ChatStatePayload.CODEC, ChatStatePayload(true, true, 81L, "玩家乙", false))

        val boardEntry = LeaderboardPayload.Entry(2, 82L, "选手", -31L, 7, 4, -2, "皮卡丘")
        assertRoundTrip(
            LeaderboardPayload.CODEC,
            LeaderboardPayload("ranked-1", "赛季一", 18, listOf(boardEntry), LeaderboardPayload.Entry.NONE),
        )
        assertRoundTrip(MenuActionPayload.CODEC, MenuActionPayload(MenuActionPayload.QUEUE, "ranked-1"))
        assertRoundTrip(OpenAuthScreenPayload.CODEC, OpenAuthScreenPayload(3, "account-7", true))

        val competition = OpenMainMenuPayload.RankedInfo(
            "ranked-1",
            "标准单打",
            "singles",
            2,
            50,
            true,
            "+12",
            "-8",
            listOf("Species Clause", "Item Clause"),
        )
        assertRoundTrip(
            OpenMainMenuPayload.CODEC,
            OpenMainMenuPayload("玩家丙", "伊布", listOf(competition)),
        )
        assertRoundTrip(OpenPagePayload.CODEC, OpenPagePayload(OpenPagePayload.LEADERBOARD, "ranked-1", "etag-7"))
        assertRoundTrip(SendChatPayload.CODEC, SendChatPayload("global", "一条消息"))
        assertRoundTrip(
            SubmitAuthPayload.CODEC,
            SubmitAuthPayload(2, "account-7", "user@example.invalid", "pass word", "004201"),
        )
    }

    @Test
    fun `房间包按原字段及成员顺序往返`() {
        assertRoundTrip(
            RoomActionPayload.CODEC,
            RoomActionPayload("create", "room-7", "练习房", "secret", "doubles", -1, 4, false, true, false, "invite-7"),
        )

        val room = RoomListPayload.Room(
            "room-7",
            "练习房",
            "房主",
            91L,
            "doubles",
            -1,
            4,
            false,
            true,
            "烈咬陆鲨",
            true,
            3,
            false,
            true,
            true,
            false,
        )
        assertRoundTrip(RoomListPayload.CODEC, RoomListPayload(listOf(room), true))

        val host = RoomStatePayload.Member("房主", 91L, "烈咬陆鲨", 6)
        val guest = RoomStatePayload.Member("访客", 92L, "巨金怪", 4)
        val watcher = RoomStatePayload.Member("观战者", 93L, "", 0)
        assertRoundTrip(
            RoomStatePayload.CODEC,
            RoomStatePayload(
                "room-7",
                "invite-7",
                "练习房",
                true,
                "doubles",
                -1,
                4,
                false,
                true,
                false,
                true,
                host,
                true,
                guest,
                listOf(watcher),
                RoomStatePayload.WATCHER,
            ),
        )
    }

    @Test
    fun `图鉴与队伍包按原字段及列表顺序往返`() {
        val dexEntry = ServerDexPayload.Entry("cobblemon:pikachu", 35, 55, 40, 50, 50, 90)
        assertRoundTrip(ServerDexPayload.CODEC, ServerDexPayload("digest-7", false, listOf(dexEntry)))
        assertRoundTrip(TeamPickPayload.CODEC, TeamPickPayload("battle-7", listOf(5, 1, 3)))

        val mine = TeamPreviewPayload.Slot("cobblemon:pikachu", 50, "M", true, "light-ball")
        val theirs = TeamPreviewPayload.Slot("cobblemon:eevee", 42, "F", false, "")
        assertRoundTrip(
            TeamPreviewPayload.CODEC,
            TeamPreviewPayload(
                "battle-7",
                "玩家甲",
                "玩家乙",
                "server-b",
                4,
                1,
                1_725_000_000_123L,
                listOf(mine),
                listOf(theirs),
                true,
                false,
                "",
            ),
        )
    }

    private fun <T : CustomPacketPayload> assertRoundTrip(
        codec: StreamCodec<RegistryFriendlyByteBuf, T>,
        expected: T,
    ) {
        val buffer = RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE)
        try {
            codec.encode(buffer, expected)
            assertEquals(expected, codec.decode(buffer))
            assertEquals(0, buffer.readableBytes(), "解码必须完整消费当前包")
        } finally {
            buffer.release()
        }
    }

    private companion object {
        val FIXED_UUID: UUID = UUID.fromString("12345678-1234-5678-9abc-def012345678")
    }
}
