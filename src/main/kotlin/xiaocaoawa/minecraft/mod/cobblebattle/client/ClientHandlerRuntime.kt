package xiaocaoawa.minecraft.mod.cobblebattle.client

import com.cobblemon.mod.common.client.CobblemonClient
import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientGuiEvent
import dev.architectury.event.events.client.ClientRawInputEvent
import dev.architectury.event.events.client.ClientScreenInputEvent
import dev.architectury.event.events.client.ClientTickEvent
import dev.architectury.networking.NetworkManager
import dev.architectury.registry.client.keymappings.KeyMappingRegistry
import io.github.rinicesiberia.shadowbattle.client.ClientViewTransitions
import io.github.rinicesiberia.shadowbattle.client.ViewTransition
import net.minecraft.client.Minecraft
import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthMode
import xiaocaoawa.minecraft.mod.cobblebattle.network.AuthResultPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.ChatLinePayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.ChatStatePayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.LeaderboardPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.OpenAuthScreenPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.OpenMainMenuPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomListPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomStatePayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.SendChatPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.ServerDexPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.SubmitAuthPayload
import xiaocaoawa.minecraft.mod.cobblebattle.network.TeamPreviewPayload

/** 注册客户端消息接收器、按键和界面事件。 */
internal object ClientHandlerRuntime {
    @JvmStatic
    fun initializeAuthentication() {
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            OpenAuthScreenPayload.TYPE,
            OpenAuthScreenPayload.CODEC,
        ) { payload, context ->
            context.queue {
                Minecraft.getInstance().setScreen(
                    AuthScreen(payload.authMode(), payload.suggestedId(), payload.emailEnabled()),
                )
            }
        }
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            AuthResultPayload.TYPE,
            AuthResultPayload.CODEC,
        ) { payload, context ->
            context.queue {
                (Minecraft.getInstance().screen as? AuthScreen)?.handleAuthenticationResult(payload.ok(), payload.message())
            }
        }
    }

    @JvmStatic
    fun submitAuthentication(
        operation: AuthMode,
        accountId: String,
        email: String,
        password: String,
        code: String,
    ) {
        NetworkManager.sendToServer(SubmitAuthPayload.of(operation, accountId, email, password, code))
    }

    @JvmStatic
    fun initializeChat() {
        KeyMappingRegistry.register(ChatKeys.OPEN)
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            ChatLinePayload.TYPE,
            ChatLinePayload.CODEC,
        ) { payload, context ->
            context.queue {
                ChatLog.add(
                    ChatLog.Channel.of(payload.channel()),
                    ChatLog.Line(payload.uid(), payload.id(), payload.name(), payload.sender(), payload.text()),
                )
            }
        }
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            ChatStatePayload.TYPE,
            ChatStatePayload.CODEC,
        ) { payload, context ->
            context.queue {
                ChatState.setEnabled(payload.enabled())
                ChatState.accept(payload.signedIn(), payload.inBattle(), payload.uid(), payload.name())
            }
        }
        ClientGuiEvent.RENDER_HUD.register(
            ClientGuiEvent.RenderHud { graphics, _ -> ChatHud.render(graphics) },
        )
        ClientGuiEvent.RENDER_POST.register(
            ClientGuiEvent.ScreenRenderPost { screen, graphics, mouseX, mouseY, _ ->
                ChatHud.drawOver(screen, graphics, mouseX, mouseY)
            },
        )
        ClientRawInputEvent.KEY_PRESSED.register(
            ClientRawInputEvent.KeyPressed { minecraft, keyCode, scanCode, action, _ ->
                when {
                    action != 1 || minecraft.screen != null || !isChatReady(minecraft) -> EventResult.pass()
                    !ChatKeys.OPEN.isUnbound && ChatKeys.OPEN.matches(keyCode, scanCode) -> {
                        minecraft.setScreen(ChatScreen(null))
                        EventResult.interruptTrue()
                    }
                    else -> EventResult.pass()
                }
            },
        )
        ClientScreenInputEvent.MOUSE_CLICKED_PRE.register(
            ClientScreenInputEvent.MouseClicked { minecraft, screen, mouseX, mouseY, _ ->
                when {
                    !isChatReady(minecraft) || !ChatHud.interactiveOver(screen) -> EventResult.pass()
                    !ChatPanel.contains(screen.width, screen.height, mouseX, mouseY) -> {
                        ChatInput.cancel()
                        EventResult.pass()
                    }
                    else -> {
                        val relativeX = mouseX.toInt() - ChatPanel.originX(screen.width)
                        val relativeY = mouseY.toInt() - ChatPanel.originY(screen.height)
                        val selectedTab = ChatPanel.tabAt(relativeX, relativeY)
                        if (selectedTab != null) {
                            ChatState.select(selectedTab)
                        } else if (ChatPanel.inInputBox(relativeX, relativeY)) {
                            ChatInput.start()
                        }
                        EventResult.interruptTrue()
                    }
                }
            },
        )
        ClientScreenInputEvent.KEY_PRESSED_PRE.register(
            ClientScreenInputEvent.KeyPressed { minecraft, screen, keyCode, scanCode, _ ->
                when {
                    !isChatReady(minecraft) || !ChatHud.interactiveOver(screen) -> EventResult.pass()
                    ChatInput.typing() -> {
                        when (keyCode) {
                            256 -> ChatInput.cancel()
                            257, 335 -> ChatInput.send()
                            258 -> ChatInput.toggleChannel()
                            259 -> ChatInput.backspace()
                        }
                        EventResult.interruptTrue()
                    }
                    !ChatKeys.OPEN.isUnbound && ChatKeys.OPEN.matches(keyCode, scanCode) -> {
                        ChatInput.start()
                        EventResult.interruptTrue()
                    }
                    else -> EventResult.pass()
                }
            },
        )
        ClientScreenInputEvent.MOUSE_SCROLLED_PRE.register(
            ClientScreenInputEvent.MouseScrolled { minecraft, screen, mouseX, mouseY, _, amountY ->
                when {
                    !isChatReady(minecraft) || !ChatHud.interactiveOver(screen) -> EventResult.pass()
                    !ChatPanel.contains(screen.width, screen.height, mouseX, mouseY) -> EventResult.pass()
                    else -> {
                        ChatHud.scroll(amountY)
                        EventResult.interruptTrue()
                    }
                }
            },
        )
        ClientScreenInputEvent.CHAR_TYPED_PRE.register(
            ClientScreenInputEvent.KeyTyped { minecraft, screen, character, _ ->
                if (isChatReady(minecraft) && ChatHud.interactiveOver(screen) && ChatInput.typing()) {
                    if (ChatInput.type(character)) EventResult.interruptTrue() else EventResult.pass()
                } else {
                    EventResult.pass()
                }
            },
        )
    }

    @JvmStatic
    fun sendChat(channel: ChatLog.Channel, text: String) {
        NetworkManager.sendToServer(SendChatPayload(channel.id, text))
    }

    @JvmStatic
    fun clearChatSession() {
        ChatState.reset()
        ChatInput.reset()
    }

    @JvmStatic
    fun initializeLeaderboard() {
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            LeaderboardPayload.TYPE,
            LeaderboardPayload.CODEC,
        ) { payload, context ->
            context.queue {
                val minecraft = Minecraft.getInstance()
                val player = minecraft.player
                if (player != null) {
                    ServerDex.rememberRanked(payload.rankedId())
                    minecraft.setScreen(LeaderboardScreen(payload, player.uuid))
                }
            }
        }
    }

    @JvmStatic
    fun initializeServerDex() {
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            ServerDexPayload.TYPE,
            ServerDexPayload.CODEC,
        ) { payload, context -> context.queue { ServerDex.open(payload) } }
        ClientTickEvent.CLIENT_POST.register(ClientTickEvent.Client { ServerDex.tick() })
    }

    @JvmStatic
    fun initializeRooms() {
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            RoomListPayload.TYPE,
            RoomListPayload.CODEC,
        ) { payload, context -> context.queue { acceptRoomList(payload) } }
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            RoomStatePayload.TYPE,
            RoomStatePayload.CODEC,
        ) { payload, context -> context.queue { acceptRoomState(payload) } }
    }

    @JvmStatic
    fun initializeMainMenu() {
        KeyMappingRegistry.register(MenuKeys.OPEN)
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            OpenMainMenuPayload.TYPE,
            OpenMainMenuPayload.CODEC,
        ) { payload, context ->
            context.queue {
                val minecraft = Minecraft.getInstance()
                if (minecraft.player != null) minecraft.setScreen(MainMenuScreen(payload))
            }
        }
        ClientRawInputEvent.KEY_PRESSED.register(
            ClientRawInputEvent.KeyPressed { minecraft, keyCode, scanCode, action, _ ->
                when {
                    action != 1 || minecraft.screen != null || minecraft.player == null -> EventResult.pass()
                    !MenuKeys.OPEN.isUnbound && MenuKeys.OPEN.matches(keyCode, scanCode) -> {
                        ServerDex.requestMain()
                        EventResult.interruptTrue()
                    }
                    else -> EventResult.pass()
                }
            },
        )
    }

    @JvmStatic
    fun initializeTeamPreview() {
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            TeamPreviewPayload.TYPE,
            TeamPreviewPayload.CODEC,
        ) { payload, context -> context.queue { acceptTeamPreview(payload) } }
    }

    private fun isChatReady(minecraft: Minecraft): Boolean = minecraft.player != null && ChatPanel.visible()

    private fun acceptRoomList(payload: RoomListPayload) {
        val minecraft = Minecraft.getInstance()
        val lobby = minecraft.screen as? RoomLobbyScreen
        val mayOpenLobby = minecraft.player != null && lobby == null && !payload.refresh()
        when (
            ClientViewTransitions.roomList(
                hasPlayer = minecraft.player != null,
                lobbyVisible = lobby != null,
                refreshOnly = payload.refresh(),
                battleActive = mayOpenLobby && CobblemonClient.battle != null,
            )
        ) {
            ViewTransition.REFRESH -> lobby?.update(payload)
            ViewTransition.OPEN -> minecraft.setScreen(RoomLobbyScreen(payload))
            ViewTransition.IGNORE -> Unit
        }
    }

    private fun acceptRoomState(payload: RoomStatePayload) {
        val minecraft = Minecraft.getInstance()
        val room = minecraft.screen as? RoomScreen
        val sameRoomVisible = room?.roomId() == payload.roomId()
        val mayOpenRoom = minecraft.player != null && !sameRoomVisible
        when (
            ClientViewTransitions.roomState(
                hasPlayer = minecraft.player != null,
                visibleRoomId = room?.roomId(),
                incomingRoomId = payload.roomId(),
                battleActive = mayOpenRoom && CobblemonClient.battle != null,
            )
        ) {
            ViewTransition.REFRESH -> room?.update(payload)
            ViewTransition.OPEN -> minecraft.setScreen(RoomScreen(payload))
            ViewTransition.IGNORE -> Unit
        }
    }

    private fun acceptTeamPreview(payload: TeamPreviewPayload) {
        val minecraft = Minecraft.getInstance()
        val preview = minecraft.screen as? TeamPreviewScreen
        when (
            ClientViewTransitions.teamPreview(
                hasPlayer = minecraft.player != null,
                visibleBattleId = preview?.battleId(),
                incomingBattleId = payload.battleId(),
                closedReason = payload.closed(),
            )
        ) {
            ViewTransition.REFRESH -> preview?.update(payload)
            ViewTransition.OPEN -> minecraft.setScreen(TeamPreviewScreen(payload))
            ViewTransition.IGNORE -> Unit
        }
    }
}
