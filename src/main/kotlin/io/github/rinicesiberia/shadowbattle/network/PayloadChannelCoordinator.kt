package io.github.rinicesiberia.shadowbattle.network

import dev.architectury.networking.NetworkManager
import dev.architectury.platform.Platform
import dev.architectury.utils.Env
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import xiaocaoawa.minecraft.mod.cobblebattle.CobbleBattle
import xiaocaoawa.minecraft.mod.cobblebattle.network.AuthResultPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.ChatLinePayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.ChatStatePayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.LeaderboardPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.MenuActionPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.OpenAuthScreenPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.OpenMainMenuPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.OpenPagePayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomActionPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomListPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomStatePayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.SendChatPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.ServerDexPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.SubmitAuthPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.TeamPickPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.TeamPreviewPayload
import java.util.function.Supplier

/** 注册游戏包通道，并统一服务端下发前的接收能力检查。 */
object PayloadChannelCoordinator {
    @JvmStatic
    fun initialize() {
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            SubmitAuthPayload.TYPE,
            SubmitAuthPayload.CODEC,
        ) { payload, context ->
            dispatch(context.player, context::queue) { participant ->
                CobbleBattle.service().onCredentialsSubmitted(
                    participant,
                    payload.authMode(),
                    payload.accountId(),
                    payload.email(),
                    payload.password(),
                    payload.verificationCode(),
                )
            }
        }
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            SendChatPayload.TYPE,
            SendChatPayload.CODEC,
        ) { payload, context ->
            dispatch(context.player, context::queue) { participant ->
                CobbleBattle.service().onChatSubmitted(participant, payload.channel(), payload.text())
            }
        }
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            OpenPagePayload.TYPE,
            OpenPagePayload.CODEC,
        ) { payload, context ->
            dispatch(context.player, context::queue) { participant ->
                CobbleBattle.service().openPage(participant, payload.page(), payload.ranked(), payload.have())
            }
        }
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            RoomActionPayload.TYPE,
            RoomActionPayload.CODEC,
        ) { payload, context ->
            dispatch(context.player, context::queue) { participant ->
                CobbleBattle.service().onRoomAction(participant, payload)
            }
        }
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            TeamPickPayload.TYPE,
            TeamPickPayload.CODEC,
        ) { payload, context ->
            dispatch(context.player, context::queue) { participant ->
                CobbleBattle.service().onTeamPicked(participant, payload.battleId(), payload.picks())
            }
        }
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            MenuActionPayload.TYPE,
            MenuActionPayload.CODEC,
        ) { payload, context ->
            dispatch(context.player, context::queue) { participant ->
                CobbleBattle.service().onMenuAction(participant, payload)
            }
        }

        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(OpenAuthScreenPayload.TYPE, OpenAuthScreenPayload.CODEC)
            NetworkManager.registerS2CPayloadType(AuthResultPayload.TYPE, AuthResultPayload.CODEC)
            NetworkManager.registerS2CPayloadType(ChatLinePayload.TYPE, ChatLinePayload.CODEC)
            NetworkManager.registerS2CPayloadType(ChatStatePayload.TYPE, ChatStatePayload.CODEC)
            NetworkManager.registerS2CPayloadType(LeaderboardPayload.TYPE, LeaderboardPayload.CODEC)
            NetworkManager.registerS2CPayloadType(ServerDexPayload.TYPE, ServerDexPayload.CODEC)
            NetworkManager.registerS2CPayloadType(RoomListPayload.TYPE, RoomListPayload.CODEC)
            NetworkManager.registerS2CPayloadType(RoomStatePayload.TYPE, RoomStatePayload.CODEC)
            NetworkManager.registerS2CPayloadType(OpenMainMenuPayload.TYPE, OpenMainMenuPayload.CODEC)
            NetworkManager.registerS2CPayloadType(TeamPreviewPayload.TYPE, TeamPreviewPayload.CODEC)
        }
    }

    @JvmStatic
    fun <T : CustomPacketPayload> sendWhenSupported(
        participant: ServerPlayer,
        type: CustomPacketPayload.Type<T>,
        payloadFactory: Supplier<T>,
    ): Boolean = ConditionalPayloadSender.deliver(
        recipient = participant,
        payloadFactory = payloadFactory::get,
        supports = { NetworkManager.canPlayerReceive(it, type) },
        send = { recipient, payload -> NetworkManager.sendToPlayer(recipient, payload) },
    )

    private fun dispatch(
        subject: Any?,
        queue: (Runnable) -> Unit,
        action: (ServerPlayer) -> Unit,
    ) {
        QueuedTypeDispatch.enqueue(subject, ServerPlayer::class.java, queue, action)
    }
}
