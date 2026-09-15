# 恢复入口

本批最终验证：JDK21离线clean build成功，9个任务全部执行，150项测试零失败；RemoteTeamCodec公开ABI与原JAR一致。三项受影响文件台账已更新内容哈希与验证批次。B4-packed-details-compatibility已完成兼容修复，下一步继续审查远端队伍对象装配和观战镜像失败清理；全项目仍未完成。

当前恢复批次B4-packed-details-compatibility，基线ba72372，工作区恢复时干净。发现并修复dffef6f的空招式槽位压缩和非法PP/亲密度setter跳过；5项定向测试已通过，包含48组调用决策差分。正在执行JDK21离线clean build，日志D:/workspace/gradle-packed-details-compatibility.log。下方142/145/148项及“当前下一步”均属历史快照，以本段最新状态为准。仍需远端队伍对象装配与镜像实体验证，90%命名覆盖、远端联调、推送和最终清理未完成。

当前完成 B5 双端启动与主要表单状态迁移，并继续完成 B4 服务端队伍预览、远端队伍基础字段和观战格式解析；下一步处理远端队伍的物种/招式装配和镜像实体装配，再继续剩余客户端界面。Gradle Wrapper 8.12.1、Kotlin 2.2.20、ModDevGradle 2.0.147，固定 Minecraft 1.21.1 / NeoForge 21.1.66 / Architectury 13.0.8 / Cobblemon 1.7.0 / KotlinForForge 5.3.0。2026-09-15 使用 JDK 21 执行 `--offline clean build` 成功，9个任务全部执行，142项测试全部通过。实际客户端完成模组构造、Showdown启动与资源加载；专用服务端完成世界生成并报告 `Done`。IDEA使用同一Gradle模型，界面导入仍未人工确认。

业务源码基线为本地提交 2b4ea8d；本批次提交包含测试和构建。首次 Modrinth 下载 TLS 中断，通过缓存相同版本原件重试成功，项目不依赖 .deps 绝对路径。此前 tests 缺 Gson 依赖已修复。

恢复时先读 PLAN.md、FILES.csv、BEHAVIOR.md，再核对 git status/diff/log 和验证版本。构建命令：JDK21 下 ./gradlew.bat build。当前网络需要代理可在命令行临时传 -Dhttps.proxyHost=localhost -Dhttps.proxyPort=7897，不写入项目配置。

当前具体下一步：审查 `RoomLobbyScreen` 的创建/密码/邀请码表单与循环选项，再处理 `RoomScreen` 的邀请、开始冷却和退出动作；随后进入世界验证镜像实体、Mixin回调和网络往返。尚未推送，也未最终清理。用户要求暂保留部署配置；公开发布前必须从可推送历史中移除，日志不得打印其值。

B2已完成：配置/身份迁移及16项测试通过，见 b2-tests.json。当前下一步B3：传输及认证业务。SnakeYAML源码仍在，计划替换为锁定2.6版本并隔离打包。

B3传输实现已通过16项原测试；继续认证基线与迁移。当前认证2项首次失败属于测试缺少Minecraft类，已补测试classpath。传输文件标记待验证，保留重连/真实服务未验证范围。

B3-account已完成Kotlin实现及21项测试，证据b3-tests.json。接下来清理第三方源码依赖、迁移聊天状态及对战状态模块，补足各自基线测试。当前没有重命名比例已达标的证据，未宣称完成。

B-vendor-chat完成：复制SnakeYAML源码已通过Git可恢复删除，Maven 2.6隔离打包；聊天状态Kotlin迁移完成；全量22项测试（含产物隔离加载）通过。下一步处理对战镜像索引/状态链路，并建立剩余符号审查。

B4-projection已提交状态组件及30项测试。建立了Java语义符号初始清单SYMBOLS.csv（5033条，含需排除的编译器生成record成员，尚未用于最终比例）。下一步先校正符号统计/审查安全改名，再继续大型服务与UI批次。

B5命名批次已完成并通过Gradle build，见build/gradle-b5-rename.log。当前工作区含419项语义改名；需先提交并重新生成符号清单，再推进BattleQueue/CrossServerBattleService剩余业务迁移。

B6-queue-state（2026-09-14）：新增 Kotlin `QueueReferenceBook`，并已接入 Java `BattleQueue`，集中管理等待队伍、房间查询引用和房主引用的绑定、领取、清理；加入、查询、启动发送失败时回收对应状态，退出、断线、房间关闭和队列响应继续使用兼容入口。新增2项状态契约测试，JDK21 下全量32项测试通过，提交 `0b68c60`，日志位于 `D:/workspace/gradle-b6-queue-integration.log`。

B4-request-ledger（2026-09-15）：新增 Kotlin `ServiceRequestLedger` 并接入 `CrossServerBattleService` 的菜单、排行榜和聊天正常/错误响应。保留排行榜/聊天64项上限、菜单无上限和发送失败撤回行为。新增3项状态契约测试，全量35项测试通过，提交 `f012cf3`，日志位于 `D:/workspace/gradle-b6-request-ledger.log`。

B4-room-directory（2026-09-15）：新增 Kotlin `RoomDirectoryState` 并接入房间列表缓存、请求合并、在途状态、错误清理和客户端摘要去重。新增4项边界契约测试，全量39项测试通过，提交 `1b52265`，日志位于 `D:/workspace/gradle-b4-room-directory.log`。

B4-team-selection（2026-09-15）：新增 Kotlin `TeamSelection` 并接入 `MirrorFactory` 的远端单玩家与本地双玩家创建路径。新增4项测试覆盖有效排序、空选择、非数字、负数、越界和重复序号，全量43项测试通过，提交 `b19ea10`，日志位于 `D:/workspace/gradle-b4-team-selection.log`。

B4-mirror-participants（2026-09-15）：新增 Kotlin `MirrorParticipantState` 并接入 `MirrorBattle` 的参与者、座位、观战者和双方业务描述。Java 兼容入口及嵌套 record 保留，原始 JAR 与当前 class 的 `javap -public` 输出一致。新增3项状态契约，原投影/路由8项继续通过，全量46项测试通过，提交 `366e259`，日志位于 `D:/workspace/gradle-b4-mirror-participants.log`。

B4-mirror-entities（2026-09-15）：新增 Kotlin `MirrorEntityRegistry` 并接入 `MirrorBattle` 的 NPC/actor 组合和临时 Pokémon 实体引用。新增2项契约验证快照与一次性转移，全量48项测试通过，公开 `javap` 签名仍与原始 JAR 一致，提交 `b454bff`，日志位于 `D:/workspace/gradle-b4-mirror-entities.log`。

B4-format-resolution（2026-09-15）：新增 Kotlin `BattleFormatResolver` 并接入 `MirrorFactory` 两条创建路径。新增3项契约验证默认格式、规则覆盖/去重、空规则回退和等级调整，全量51项测试通过，提交 `adaa96f`，日志位于 `D:/workspace/gradle-b4-format-resolver.log`。

B4-clean-build（2026-09-15）：JDK21 下执行 `./gradlew.bat --offline clean build --console=plain` 成功，9个任务全部执行，51项测试、0失败，发布 JAR 为 `build/libs/cobblemon-shadow-battle-neoforge-0.1.3.jar`。日志位于 `D:/workspace/gradle-b4-clean-build-20260915.log`。下一步处理镜像构造启动确认、失败回收和结束结果投影；游戏客户端、专用服务端和真实远端服务仍未验证。

B4-battle-result（2026-09-15）：新增 Kotlin `BattleResultProjection` 并接入结束事件与分数播报。新增3项契约覆盖胜负、非法玩家、顺序、默认数值和惰性目标查找，全量54项测试通过，提交 `06bf4a2`，日志位于 `D:/workspace/gradle-b4-battle-result.log`。

B4-pokemon-ownership（2026-09-15）：新增 Kotlin `MirrorOwnershipIndex` 并接入 `MirrorPokemon`。新增2项契约覆盖重复登记接管和幂等释放，全量56项测试通过，公开 `javap` 签名与原始 JAR 一致，提交 `a415554`，日志位于 `D:/workspace/gradle-b4-pokemon-ownership.log`。

B4-clean-build-56（2026-09-15）：JDK21 下离线空目录构建成功，9个任务全部执行，56项测试、0失败，发布 JAR 已生成；Kotlin 源码无 `!!` 和 `@Suppress`。日志位于 `D:/workspace/gradle-b4-clean-build-56-20260915.log`。下一步处理镜像构造启动确认与失败回收；游戏客户端、专用服务端和真实远端服务仍未验证。

B4-npc-state（2026-09-15）：新增 Kotlin `MirrorNpcState` 并接入活跃 NPC、皮肤缓存和显示名规范化。新增3项状态契约，全量59项测试通过，`MirrorNpc` 公开 `javap` 签名与原始 JAR 一致，提交 `bac1039`，日志位于 `D:/workspace/gradle-b4-npc-state.log`。

B5-public-abi（2026-09-15）：公开 ABI 审计发现并修复 `CobbleBattleConfig` 多出的公开 `validate()`，校验逻辑迁入 Kotlin `ConfigurationValidation`，包级 `GSON` 保留。原 JAR 的123个项目自有 public class 与当前发布 JAR 的 `javap -public` 对照为缺失0、差异0；全量59项测试通过。审计结果位于 `D:/workspace/cobblebattle-public-abi-filtered-20260915.json`，测试日志位于 `D:/workspace/gradle-abi-config-fix.log`。当前代码和台账待提交；下一步继续 B4 构造/清理，再执行 clean build。

B3-payload-contract（2026-09-15）：新增Kotlin `PayloadTypeCatalog`，16个公开payload record保留原ABI并委托统一命名空间创建。新增4项契约覆盖全部ID与CODEC代表值，原JAR与当前JAR的23个network公开class签名一致；修复测试编译期缺少ModDev主类路径的问题；JDK21离线全量63项测试通过，日志位于 `D:/workspace/gradle-b3-payload-contract.log`。下一步提取 `CobbleBattleNetwork` 内部注册/发送实现，保留公开静态入口，再重跑公开ABI与clean build。

B3-network-routing（2026-09-15）：`CobbleBattleNetwork`公开静态入口委托Kotlin `PayloadChannelCoordinator`；C2S接收注册、服务器主线程入队、S2C服务端注册和能力检测下发已迁移。新增3项策略契约，全量66项测试通过，network包23个公开class签名一致；日志位于 `D:/workspace/gradle-b3-network-routing.log`。实际客户端/专用服务端注册与收发仍待验证。下一步回到B4镜像构造失败回收，随后执行clean build。

B4-mirror-cleanup（2026-09-15）：删除包内Java `MirrorTeardown`，迁移并重命名为Kotlin `MirrorLifecycleCleanup`，服务内部字段同步改为 `lifecycleCleanup`；立即/延迟清理顺序保持。新增2项宽限时间边界契约，全量68项测试通过，`CrossServerBattleService`公开签名一致；日志位于 `D:/workspace/gradle-b4-mirror-cleanup.log`。实体和BattleRegistry实际回收仍待游戏内验证。下一步提取镜像构造作用域与失败结果，再执行clean build。

B4-construction-attempt（2026-09-15）：新增Kotlin `BattleConstructionAttempt`，`MirrorFactory`两条创建路径共用启动与构造上下文收尾。新增3项测试覆盖成功、运行时异常和Error，全量71项通过；日志位于 `D:/workspace/gradle-b4-construction-attempt.log`。下一步执行JDK21离线clean build与公开ABI复核，再继续实体装配和mixin未触发分支。

B3-B4-clean-71（2026-09-15）：JDK21离线clean build成功，9个任务全部执行，71项测试零失败，thin与发布JAR已生成；日志位于`D:/workspace/gradle-clean-b3-b4-71-20260915.log`。公开ABI以此前123类全量审计为基线，对其后受影响的23个network类和`CrossServerBattleService`增量核对一致。客户端、专用服务端、真实远端服务及实体生命周期仍未验证。下一步审查并移除构建模型不使用的反编译工程残留，再继续B4/B5。

B6-residue-cleanup（2026-09-15）：删除失效的`build.ps1`、旧`.iml`、反编译摘要/日志/导入/参数文件及无入站依赖的合成`PlatformMethods`。删除后JDK21离线clean build的9个任务全部执行，71项测试通过，发布JAR无`architectury_inject_*`条目；日志位于`D:/workspace/gradle-clean-b6-residue-71-20260915.log`。当前工作树待提交；下一步继续B4实体装配和mixin未触发分支，随后审查B5客户端、命令与Mixin。

B5-mixin-contract（2026-09-15）：Mixin配置与原JAR一致，6个配置类存在；新增3项元数据契约覆盖13个Inject目标和3个图鉴绘制调用目标。新增Kotlin `BattleIdentifierParsing`供服务与Showdown输出注入共用，1项UUID边界测试通过。JDK21离线全量75项测试通过，两个受影响公开类签名一致；日志位于`D:/workspace/gradle-b5-mixin-contract.log`。实际客户端/服务端Mixin应用仍待验证。下一步审查NeoForge引导和客户端接收器注册，再执行clean build。

B5-neoforge-entry（2026-09-15）：NeoForge入口迁移为同包同名Kotlin类，`@Mod`值、公开零参数构造与初始化顺序保持。新增1项入口契约，全量76项测试通过；归一化`javap -public`签名与原JAR一致，日志位于`D:/workspace/gradle-b5-neoforge-entry.log`。FML实际实例化及客户端/服务端分侧加载仍待验证。下一步逐组迁移客户端接收器注册，先固定7个handler的包类型和主线程入队行为。

B5-clean-76（2026-09-15）：JDK21离线clean build成功，9个任务全部执行，76项测试零失败，thin与发布JAR生成；日志位于`D:/workspace/gradle-clean-b5-76-20260915.log`。下一步从7个客户端handler的注册与主线程入队开始，保留公开静态入口，并继续记录未验证的UI副作用。

B5-client-handlers（2026-09-15）：新增Kotlin `ClientHandlerRuntime` 和 `ClientViewTransitions`，七个Java handler保留原公开静态入口并委托。房间列表、房间状态和队伍预览的刷新/打开/忽略规则新增5项测试；JDK21离线clean build成功，9个任务全部执行，81项测试零失败，七个handler公开签名与原JAR一致。日志位于`D:/workspace/gradle-clean-b5-client-81-20260915.log`。实际客户端启动、S2C收包、按键与界面副作用仍待验证。下一步审查`command/*`、其余`client/*`与图鉴状态链路，并重新统计安全内部符号覆盖率。

B5-command-runtime（2026-09-15）：新增 Kotlin `CommandExecutionRuntime` 和 `CommandDecisions`，八个Java命令实现类收缩为兼容入口；`AbstractSubCommand`与`SubCommand`审查保留。新增6项测试覆盖排行选择、重载后续动作、命令树、排行参数节点和权限等级；JDK21离线clean build成功，9个任务全部执行，87项测试零失败，命令目录十个公开类型签名与原JAR一致。日志位于`D:/workspace/gradle-clean-b5-command-87-20260915.log`。实际服务器命令、权限、配置重载和远端连接副作用仍待验证。下一步处理`RemoteDex`与`ServerDex`图鉴链路，再审查其余客户端界面和语言资源。

B4-dex-snapshot（2026-09-15）：新增Kotlin `RemoteDexSnapshotState`，`RemoteDex`公开入口保留并委托物种快照、摘要与就绪状态。新增5项测试覆盖解析、不可变能力值、未变化确认、暂停/失效和真实磁盘往返；JDK21离线clean build成功，9个任务全部执行，92项测试零失败，`RemoteDex`公开签名与原JAR一致。日志位于`D:/workspace/gradle-clean-b4-dex-snapshot-92-20260915.log`。下一步迁移`RemoteDex`的纯规则判断与`ServerDex`客户端快照状态；实际Pokémon和Pokedex全局替换继续列为集成验证。

B5-client-dex-state（2026-09-15）：新增Kotlin `ServerDexSnapshotState`，`ServerDex`委托摘要、能力值索引、形态回退和最近排行。新增4项测试；JDK21离线clean build成功，9个任务全部执行，96项测试零失败，`ServerDex`公开签名与原JAR一致。日志位于`D:/workspace/gradle-clean-b5-client-dex-96-20260915.log`。下一步处理`RemoteDex`纯规则判断及其余客户端界面；实际Dexes替换、反射字段和GUI生命周期继续列为集成验证。

B4-dex-legality（2026-09-15）：新增Kotlin `DexLegalityRules`，`RemoteDex`委托标识规范化、能力/招式许可和EV/IV上限判断。新增4项边界测试；JDK21离线clean build成功，9个任务全部执行，100项测试零失败，`RemoteDex`公开签名与原JAR一致。日志位于`D:/workspace/gradle-clean-b4-dex-rules-100-20260915.log`。下一步审查其余客户端界面状态与语言资源，并准备实际NeoForge客户端/服务端启动验证。

B5-client-startup（2026-09-15）：实际 `runClient` 先发现 KotlinForForge 缺失，再发现普通库未进入 1.21.1 游戏隔离类路径。`build.gradle.kts` 已加入 Modrinth 固定版本 KotlinForForge 5.3.0，并将 SnakeYAML 2.6 加入 `additionalRuntimeClasspath`；发布包仍仅重定位打入 SnakeYAML。修复后客户端完成 CobbleBattle 构造、Showdown 启动与全部资源加载，稳定停留在可交互状态；未发现项目自身 FATAL/ERROR。模板配置值未改变，差异日志已隐藏受保护值并新增1项契约。JDK21离线clean build的9个任务全部执行，101项测试零失败；日志位于`D:/workspace/gradle-run-client-b5-background-20260915.log`和`D:/workspace/gradle-clean-b5-client-runtime-101-20260915.log`。下一步运行专用服务端，并继续记录网络注册和分侧加载结果。

B5-server-startup（2026-09-15）：JDK21 下执行 `./gradlew.bat --offline runServer --console=plain`，NeoForge专用服务端完成模组构造、Showdown启动、数据加载、世界生成并报告 `Done`；CobbleBattle FATAL/ERROR为0。日志中的4处分侧类诊断及资源标签错误来自Cobblemon，未影响启动。测试完成后已终止进程，未残留项目Java进程；日志位于`D:/workspace/gradle-run-server-b5-initial-20260915.log`。下一步审查聊天输入、认证界面和房间交互状态，再进入真实网络与世界实体验证。

B5-chat-composer（2026-09-15）：新增 Kotlin `ChatComposerState` 并接入公开 Java `ChatInput`，覆盖输入开关、草稿保留、字符过滤、200单元上限、Unicode退格、Java裁剪边界和提交清空。新增5项状态测试及1项兼容入口测试，原聊天3项继续通过；`ChatInput`公开ABI与原JAR一致。JDK21离线clean build的9个任务全部执行，107项测试零失败，日志位于`D:/workspace/gradle-clean-b5-chat-composer-107-20260915.log`。下一步处理`ClientSettings`持久化，再审查认证和房间界面状态。

B5-client-settings（2026-09-15）：新增 Kotlin `ClientSettingsStore` 并接入公开 Java `ClientSettings`，覆盖缺失文件默认值、一次性读取、合法值、损坏回退、目录创建、JSON写入和保存失败内存状态。新增5项契约，`ClientSettings`公开ABI与原JAR一致。JDK21离线clean build的9个任务全部执行，112项测试零失败，日志位于`D:/workspace/gradle-clean-b5-client-settings-112-20260915.log`。下一步审查`AuthScreen`字段状态和提交决策。

B5-auth-form（2026-09-15）：新增 Kotlin `AuthenticationFormRules` 和 `AuthenticationSubmission`，`AuthScreen`委托模式、翻译键、字段门控、验证码冷却及提交参数。新增7项分支契约；`AuthScreen`公开ABI与原JAR一致。JDK21离线clean build的9个任务全部执行，119项测试零失败，日志位于`D:/workspace/gradle-clean-b5-auth-form-119-20260915.log`。下一步按房间链路审查`RoomScreen`、`RoomLobbyScreen`与`TeamPreviewScreen`。

B5-team-preview（2026-09-15）：新增 Kotlin `TeamPreviewSelectionState` 并接入 `TeamPreviewScreen`，覆盖插入顺序、重复移除、选择上限、关闭/截止/准备锁定、精确确认数量和倒计时取整。新增4项状态契约；`TeamPreviewScreen`公开ABI与原JAR一致。JDK21离线clean build的9个任务全部执行，123项测试零失败，日志位于`D:/workspace/gradle-clean-b5-team-preview-123-20260915.log`。下一步处理房间大厅表单和房间内动作。

B5-room-lobby（2026-09-15）：新增 Kotlin `RoomCreationOptions`、`RoomCreationRequest` 和 `RoomLobbyRules` 并接入 `RoomLobbyScreen`。创建默认值、循环选项、开关、Java裁剪边界、空名称回退、邀请码与座位类型共5项契约通过；空邀请码保持表单打开、密码加入不裁剪的Java控制流不变。`RoomLobbyScreen`公开ABI与原JAR一致。JDK21离线clean build的9个任务全部执行，128项测试零失败，日志位于`D:/workspace/gradle-clean-b5-room-lobby-20260915.log`。下一步审查并迁移`RoomScreen`的邀请、开始冷却和离开动作。

B5-room-controls（2026-09-15）：新增 Kotlin `RoomInteractionState` 并接入 `RoomScreen`。房主、对手、战斗状态和冷却组合门控，200 tick开始冷却及40 tick复制提示共4项契约通过；Java继续处理命中、剪贴板、网络和关屏副作用。`RoomScreen`公开ABI与原JAR一致。JDK21离线clean build的9个任务全部执行，132项测试零失败，日志位于`D:/workspace/gradle-clean-b5-room-controls-20260915.log`。下一步审查镜像实体装配和其余客户端界面。

B4-team-preview-sessions（2026-09-15）：新增 Kotlin `TeamPreviewSessionDirectory`、`TeamPreviewSession` 和 `TeamPreviewRules` 并接入 `TeamPreviews`。会话替换/筛选/清理、默认首选以及数量/范围/重复选择共4项契约通过；首次编译暴露 Java/Kotlin getter 调用错误，修正后定向测试和全量构建通过。JDK21离线clean build的9个任务全部执行，136项测试零失败，日志位于`D:/workspace/gradle-clean-b4-team-preview-sessions-20260915.log`。下一步审查`RemoteTeamCodec`字段解析与镜像实体装配。

B4-packed-team-values（2026-09-15）：新增 Kotlin `PackedTeamValueParsing` 并接入 `RemoteTeamCodec` 的等级、亲密度、HP、IV、EV、PP、UUID和性别解析。新增4项契约覆盖上下界、失败回退、Java裁剪、UUID及大小写敏感标记；Java保留原日志和对象装配。`RemoteTeamCodec`公开ABI与原JAR一致。JDK21离线clean build的9个任务全部执行，140项测试零失败，日志位于`D:/workspace/gradle-clean-b4-team-values-20260915.log`。下一步审查物种/形态、招式和太晶属性装配，再处理镜像实体。

B4-spectator-format（2026-09-15）：`SpectatorFactory` 删除重复格式解析，改用 Kotlin `BattleFormatResolver.resolveSpectator`。新增2项契约固定观战外层格式回退和内层类型优先，普通镜像3项格式契约继续通过。JDK21离线clean build的9个任务全部执行，142项测试零失败，日志位于`D:/workspace/gradle-clean-b4-spectator-format-20260915.log`。下一步继续观战实体装配、失败清理与远端队伍对象验证。

台账核对（2026-09-15）：逐项检查 `FILES.csv` 的路径存在性，137个缺失路径均为已记录删除或Java到Kotlin迁移关系。纠正 `MirrorTeardown.java` 和 `CobbleBattleNeoForge.java` 两个旧路径仍标待验证的过期状态；对应 Kotlin 实现继续分别承载实体集成风险和已完成的双端启动证据。当前台账348项、无重复路径、状态值全部合法。

B4-packed-team-details（2026-09-15）：`RemoteTeamCodec` 使用 Kotlin `PackedTeamDetailsParser` 读取固定字段。性质和能力为空时保持空值；招式名称最多四项并过滤空项；PP 保留位置，仅带 `/` 且可解析的当前值进入对象装配，异常值沿用原对象默认；亲密度限制在0至255，太晶字段保留原文本交给 Java 名称/标识解析。新增3项契约。JDK21离线clean build的9个任务全部执行，145项测试、0失败，日志位于`D:/workspace/gradle-clean-b4-packed-details-20260915.log`。物种/形态索引、Cobblemon能力与招式注册、太晶名称和实体标记待真实数据验证。
下一步：审查物种/形态索引、注册对象创建和太晶名称分支。

B4-species-index（2026-09-15）：新增 Kotlin SpeciesLookupIndex、SpeciesLookupEntry 并接入 RemoteTeamCodec。标准形态、显式形态、重复标识和空集合共3项契约通过；修复 Java 缓存声明残留 FormLookup 的编译错误后，全量构建通过。JDK21离线clean build的9个任务全部执行，148项测试零失败，日志位于D:/workspace/gradle-clean-b4-species-index-20260915.log。下一步处理 Cobblemon 真实物种/形态注册验证和镜像实体装配。
