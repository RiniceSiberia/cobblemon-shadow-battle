# 恢复入口

当前推进 B4/B5 图鉴与客户端审查。B1 已完成 Gradle Wrapper 8.12.1、Kotlin 2.2.20、ModDevGradle 2.0.147，固定 Minecraft 1.21.1 / NeoForge 21.1.66 / Architectury 13.0.8 / Cobblemon 1.7.0。2026-09-15 使用 JDK 21 执行 `--offline clean build` 成功，9个任务全部执行，100项测试全部通过。IDEA使用相同Gradle模型，测试编译类路径已补齐，实际界面导入尚未测试。

业务源码基线为本地提交 2b4ea8d；本批次提交包含测试和构建。首次 Modrinth 下载 TLS 中断，通过缓存相同版本原件重试成功，项目不依赖 .deps 绝对路径。此前 tests 缺 Gson 依赖已修复。

恢复时先读 PLAN.md、FILES.csv、BEHAVIOR.md，再核对 git status/diff/log 和验证版本。构建命令：JDK21 下 ./gradlew.bat build。当前网络需要代理可在命令行临时传 -Dhttps.proxyHost=localhost -Dhttps.proxyPort=7897，不写入项目配置。

下一步：将配置解析、文件读写和身份缓存迁移至 Kotlin，保持原配置键/错误规则，重跑配置与完整测试后立刻更新记录；之后处理传输与认证。尚未推送，也未清理。用户要求暂保留部署地址和凭证；公开发布前仍需处理这部分，禁止把当前带凭证的本地历史直接推送。

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
