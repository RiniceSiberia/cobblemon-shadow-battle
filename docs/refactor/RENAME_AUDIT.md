# 安全符号重命名审计

本审计以基线 javac 语义绑定结果为准。成员候选沿用早期批次规则：排除 public/protected 成员、构造器、枚举常量、record 自动成员、API 包、Mixin 和无方法体声明；另计非 Mixin 的具名内部类型。声明按路径、种类、owner 和原名与当前 Java 零错误快照精确匹配。

2026-09-19 当前快照共有 3487 个候选，其中 289 个原名仍存在，3198 个原声明键已消失，原声明键消失比例上限为 91.72%。删除、迁入 Kotlin 和真正改名仍需逐项对应，该数值不能单独作为最终 90% 验收结果。当前 Java 快照含 97 个文件、4277 个声明，javac 分析错误为 0。

剩余原名逐项记录在 `RENAME_REMAINING.csv`。当前数量较多的文件为：

- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/network/RoomStatePayload.java`：20 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/client/Backdrop.java`：20 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/client/BackButton.java`：19 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/battle/ApiEvents.java`：18 项

已完成命名批次：`RoomLobbyScreen`、`LeaderboardScreen`、`AuthScreen`、`TeamPreviewScreen`、`RoomScreen`、`ChatRoomScreen`、`ChatPanel`、`RankedScreen`、`MainMenuScreen`、`SettingsScreen`、`Ui`、`ChatHud`、`Msg`、`MirrorBattle`、`CobblemonCompat`、`ChatScreen`、`TeamPreviewCoordinator`（原 `TeamPreviews`）、`BattleServerClient`、`CrossServerBattleService`、`RemoteDex`、`ServerDex` 和原 `BattleQueue` 的安全内部声明已按业务职责完成语义改名。当前精确匹配不到这些文件的基线安全原名。

每个命名批次完成后重新生成零错误快照和剩余清单；只在旧声明与新职责建立对应且适用验证通过后计入最终覆盖率。

B6-ranked-screen-symbols（2026-09-17）：完成 RankedScreen 的安全内部声明语义改名；259 项测试通过，真实客户端渲染和远端请求仍待联调。

B6-main-menu-screen-symbols（2026-09-17）：完成 MainMenuScreen 的 97 个安全内部声明语义改名；259 项测试通过，真实客户端渲染、点击和远端动作仍待联调。

B6-cross-server-service-symbols（2026-09-19）：CrossServerBattleService 的 225 个基线剩余声明已建立对应，其中 222 项在本批次改名，3 个 lambda 参数此前已改为 participant。Java/Kotlin 调用点同步更新；JDK21 离线 clean build 成功，259 项测试零失败，javac 97 个文件、4277 个声明、0 个分析错误，公开 javap 签名与原 JAR 一致。真实跨服和游戏内副作用仍待联调。

B6-remote-dex-symbols（2026-09-19）：RemoteDex 的 72 个基线剩余声明已全部建立语义对应并改名。JDK21 离线 clean build 成功，259 项测试零失败，javac 97 个文件、4277 个声明、0 个分析错误，公开 javap 签名与原 JAR 一致。真实 Pokémon 对象、缓存文件和远端图鉴握手仍待联调。

B6-server-dex-symbols（2026-09-19）：ServerDex 的 65 个基线剩余声明已全部建立语义对应并改名，其中私有内部类型 `Records` 改为 `RecordFields`。JDK21 离线 clean build 成功，259 项测试零失败，javac 97 个文件、4277 个声明、0 个分析错误，公开 javap 签名与原 JAR 一致。真实 Dexes 替换、反射字段、GUI、Mixin 和客户端生命周期仍待联调。

B6-matchmaking-queue-symbols（2026-09-19）：原 BattleQueue 的 65 个基线剩余声明已全部建立语义对应，其中顶层协调器改为 `MatchmakingQueueCoordinator`，内部记录 `QueuedTeam` 改为 `ClaimedQueueTeam`，其余 63 项字段、方法、参数和局部变量完成语义改名。首次快速编译发现 Kotlin 方法引用和调用仍使用旧 `claim`，同步改为 `claimWaitingTeam` 后，JDK21 离线 clean build 成功，259 项测试零失败；javac 快照为 97 个文件、4277 个声明、0 个分析错误，旧 BattleQueue 类不再进入产物。真实队列、房间、远端服务、玩家消息和镜像建场仍待联调。

B6-settings-screen-symbols（2026-09-19）：SettingsScreen 的 64 个基线剩余声明已全部建立语义对应，其中私有记录 `Row` 改为 `PreferenceToggle`，其余 63 项字段、方法、参数和局部变量完成语义改名。快速 `classes` 与 JDK21 离线 clean build 均成功，259 项测试零失败；javac 快照为 97 个文件、4277 个声明、0 个分析错误，公开 javap 与原 JAR 无差异。真实客户端渲染、返回按钮和偏好开关点击仍待验证。

B6-ui-symbols（2026-09-19）：Ui 的 59 个基线剩余参数、循环索引和模式绑定变量已全部完成语义改名；既有 `outputStream` 不在本轮基线剩余项中并保持。快速 `classes` 与 JDK21 离线 clean build 均成功，259 项测试零失败；javac 快照为 97 个文件、4277 个声明、0 个分析错误，公开 javap 与原 JAR 无差异。真实字体资源及 13 个调用文件的客户端显示仍待验证。

B6-chat-hud-symbols（2026-09-19）：ChatHud 的 45 个基线剩余字段、方法、参数和局部变量已全部建立语义对应，另将早期误名 `connectionRequested` 修正为 `targetScrollOffset`。包内 `draw` 改为 `drawChatPanel` 并同步 ChatScreen 调用，一行滚动常量恢复为实际计算输入。快速 `classes` 与 JDK21 离线 clean build 均成功，259 项测试零失败；javac 快照为 97 个文件、4277 个声明、0 个分析错误，公开 javap 与原 JAR 无差异。真实客户端 HUD、聊天界面、滚轮和身份提示仍待验证。
B6-message-catalogue-symbols（2026-09-19）：Msg 的 45 个基线剩余字段、方法、参数、资源变量和局部变量已全部建立语义对应；另修正早期误名 `connectionRequested` 及两个 `outputStream`。默认语言与内置目录常量恢复为实际引用。快速 `classes` 与 JDK21 离线 clean build 均成功，259 项测试零失败；javac 快照为 97 个文件、4277 个声明、0 个分析错误，公开 javap 与原 JAR 无差异。真实外部语言文件、读取失败日志和全部消息目录仍待验证。

B6-mirror-battle-symbols（2026-09-19）：MirrorBattle 的 40 个基线剩余字段、方法、参数和局部变量已全部建立语义对应并改名；CrossServerBattles 的方法引用及 MirrorPropEntities、MirrorLifecycleCleanup 的 Kotlin 调用同步迁移。快速 `classes` 与 JDK21 离线 clean build 均成功，259 项测试零失败；javac 快照为 97 个文件、4277 个声明、0 个分析错误，40 个基线旧声明精确匹配为 0，公开 javap 与原 JAR 无差异。真实实体、BattleRegistry、Showdown 解释和对战生命周期仍待联调。

B6-cobblemon-compat-symbols（2026-09-19）：CobblemonCompat 的 26 个基线剩余字段、方法、参数、局部变量和异常参数已全部建立语义对应并改名。快速 `classes` 与 JDK21 离线 clean build 均成功，259 项测试零失败；javac 快照为 97 个文件、4277 个声明、0 个分析错误，26 个基线旧声明精确匹配为 0，公开 javap 与原 JAR 无差异。真实客户端画像绘制、跨 Cobblemon 版本反射和图鉴枚举仍待验证。

B6-chat-screen-symbols（2026-09-19）：ChatScreen 的 26 个基线剩余字段、方法、参数和局部变量已全部建立语义对应并改名。快速 `classes` 与 JDK21 离线 `clean build` 均成功，259 项测试零失败；javac 快照为 97 个文件、4277 个声明、0 个分析错误，26 个基线旧声明精确匹配为 0，公开 javap 与原 JAR 无差异。真实客户端键鼠输入、频道切换、滚动、提交和返回界面仍待验证。

B6-team-preview-coordinator-symbols（2026-09-19）：原 TeamPreviews 的 25 个基线剩余类、字段、方法、参数和局部变量已全部建立语义对应并改名，文件迁移为 TeamPreviewCoordinator.java；CrossServerBattleService 调用点同步更新。快速 `classes` 与 JDK21 离线 `clean build` 均成功，259 项测试零失败；javac 快照为 97 个文件、4277 个声明、0 个分析错误，25 个基线旧声明精确匹配为 0，旧内部类未进入产物，CrossServerBattleService 公开 javap 与原 JAR 无差异。真实客户端、在线玩家、远端事件和队列副作用仍待联调。
B6-battle-server-client-symbols（2026-09-19）：`BattleServerClient` 的 23 个基线剩余字段和参数已全部建立语义对应并改名，公开方法签名与传输边界保持。JDK21 离线 `clean build` 成功，259 项测试零失败；javac 快照为 97 个文件、4277 个声明、0 个分析错误；23 个基线旧声明精确匹配为 0，公开 javap 与原 JAR 无差异。真实远端连接、帧往返、断线重连和认证握手仍待联调。

B6-room-state-payload-symbols（2026-09-19）：移除 RoomStatePayload 的 20 个安全内部命名候选并记录新语义名；公开 record 组件、TYPE、CODEC 和协议顺序未改。构建与 259 项测试通过，源码 SHA-256：C90D57AE9DEA6BF40E2D6FB47410BF500BBF2719763E3320A2AEC2ABD71D07AD。

B6-backdrop-symbols（2026-09-19）：移除 Backdrop 的 20 个安全内部命名候选并记录新语义名；公开 draw、贴图路径、动画与裁剪参数保持。构建与 259 项测试通过，源码 SHA-256：19269970B988FCE4EF20A8900BA15C690239229CF9037AD0DBB2C65E4E35A438。
