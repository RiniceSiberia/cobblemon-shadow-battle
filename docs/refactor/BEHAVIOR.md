# 行为契约与证据

基线版本 baseline-local-20260914；仅全量编译通过，以下行为尚未建立运行证据。

| 链路 | 原实现 | 正常、异常及副作用约束 | 新实现/证据 | 状态 |
| --- | --- | --- | --- | --- |
| 配置 | CobbleBattleConfig / ConfigFile / YamlTree | 首次写模板；空映射默认值；非法端口回退18470；帧上限至少1MiB；读取失败回退；reload 失败抛异常并保留原值；区分连接参数 | Kotlin 实现；配置5项契约通过 | 已验证 |
| 服务器身份 | ServerIdentity | 复用非空文件；空/不可读时生成；保存失败仍返回；并发缓存；mc-前缀 | Kotlin 实现；身份3项契约通过 | 已验证 |
| TCP 传输 | BattleServerClient | 压缩流、四字节长度、引用序号、握手、按需连接、拒绝、重连及回调 | Kotlin 状态机；Socket契约2项通过，重连交错待验证 | 待验证 |
| 认证 | AuthService / AuthMode | 枚举顺序及越界LOGIN；登录注册绑定邮箱验证码；事件及清理 | Kotlin 会话与消息；5项契约通过，远端联调待验证 | 待验证 |
| 游戏包 | network/* | 17个文件；包ID、字段顺序、线程调度、客户端/服务器路由 | 16个包ID、全部CODEC及收发调度策略契约通过；环境注册待集成验证 | 待验证 |
| 排队及对战 | BattleQueue / CrossServerBattleService | 加入退出、远端匹配、选择与回合中继、断线、结束清理、事件顺序 | 队列与服务请求状态已提取；状态契约5项通过，游戏链路继续实施 | 进行中 |
| 镜像及观战 | MirrorBattle / MirrorFactory / SpectatorFactory | 实体、队伍、座位、回合、退出及错误回收 | 索引、序号缓存、启动作用域及回收调度契约通过；实体生命周期待验证 | 待验证 |
| 图鉴 | RemoteDex / ServerDex | 远端数据、同步、缓存及错误处理 | 远端快照与客户端摘要/能力索引共9项契约通过；Pokémon规则和客户端全局图鉴替换待集成验证 | 进行中 |
| UI及命令 | client/* / command/* / lang/* | 权限、显示、输入、翻译、数据请求、副作用 | 客户端接收器和七个命令执行链已迁入Kotlin；界面切换、参数选择和命令树11项契约通过，其余界面待审查 | 进行中 |
| 公开API | api/* | 类名、record访问器、事件类型、静态字段及JVM签名 | 原JAR 123个公开class与当前发布包逐项 `javap -public` 一致；事件运行待验证 | 待验证 |
| Mixin及引导 | mixin/* / neoforge/* / CobbleBattle | 注入目标/描述符、客户端隔离、注册顺序 | 配置、类清单、13个Inject目标及3个绘制调用描述符契约通过；实际应用待验证 | 待验证 |

测试只能在实际执行后填写通过，后续改动影响调用链时需重新执行。生产服务器身份的随机生成不能因测试可复现而改成固定 seed；测试输入使用固定值。

B1-gradle-baseline：Gradle build 成功，13个测试全部通过。ConfigurationContractTest 5项，ValueContractTest 3项，TransportContractTest 2项（本地真实Socket双向压缩/异常长度），ChatContractTest 3项。此证据建立于业务源码迁移前；未覆盖完整认证请求、游戏实体和UI。配置模板部署字段按用户决定保持。

B2-configuration：ConfigFile→ConfigurationRepository，YamlTree→ConfigurationDocument，reload→ConfigurationReloading，ServerIdentity内部缓存/IO→PersistentServerIdentity；配置字段和公开入口保留。原配置5项契约继续通过；新增身份3项验证Java trim、缓存、16次并发只生成一次、磁盘写入失败仍返回。全量16 tests / 0 failures，详细计数见 b2-tests.json。

B3-transport：BattleServerClient保留公开Java入口，状态机和帧收发迁至RemoteBattleConnection，标量读取迁至MessageFields。16项既有测试通过；网络2项验证握手/压缩/普通发送门控/Unicode/非对象忽略/手动断线原因/非法长度。尚未覆盖真实远端服务、超时重连压力和所有并发交错。新增AccountContractTest初次2项因测试运行类路径缺Minecraft Component失败，已补测试类路径后重跑，不能归为业务回归。

B3-account：AuthService→AccountSessions+AuthenticationMessages，保留公开入口和Outcome/Signed记录。21项测试通过，含原认证会话2项以及新增三项消息字段/空白边界。代码保持Java isBlank与trim规则、绑定输入不裁剪、验证码请求不携带player、先写pending后发送及失败撤回。真实玩家提交和远端服务联调仍待验证。

B-vendor差分验证：删除前用同一组16个有效YAML和2个非法/不安全标签文档，对比原内嵌解析器与Maven org.yaml:snakeyaml:2.6，全部结果/异常接受性一致（YamlDependencyContractTest，build/gradle-yaml-comparison.log，22项测试全通过）。随后移除123个复制库文件及Java9 Logger源码，使用锁定依赖并隔离打包。最终保留配置契约测试，原差分测试只用于这次替换验证。

B-chat：ChatLog→ChatHistory，ChatState→ConversationState，保留公开入口/Channel/Line。原聊天3项测试通过，保留40条上限、不可变快照、战斗退出恢复频道、注销清空、负滚动归零、reset不改enabled。B-artifact：最终shadowJar在仅含Gson和Kotlin标准库的隔离加载器中解析配置成功，未误打包Minecraft/Cobblemon。

B4-projection：新增迁移前对战契约5项通过，再提取BattleProjectionIndex和SequencedOutputBuffer，新增序号精确交付/先限容量/两分钟边界3项通过。全量30项通过。注入ThreadLocal清理、权威仅中继强制结束、非权威吞掉选择、双本地座位路由保留。未覆盖真实Showdown解释和NPC实体生命周期。

B5-symbol-batch：419项内部参数/局部/字段/方法名改写，Gradle build通过，现有行为契约全部通过。改名工具先做Javac语义分析，未产生分析错误；尚未进行游戏内启动。

B6-queue-state：`BattleQueue` 的等待队伍、房间查询引用、房主引用已委托给 Kotlin `QueueReferenceBook`。加入请求发送失败时同时回收等待队伍和请求引用；房间查询/房间启动发送失败时回收对应引用；退出、断线、房间关闭及队列响应仍按原顺序清理。新增2项状态契约测试，并以全量32项测试验证编译、打包和既有行为。真实远端匹配、游戏内玩家断线及服务重连仍未实测。

B4-request-ledger：`CrossServerBattleService` 的菜单、排行榜和聊天请求引用已委托给 Kotlin `ServiceRequestLedger`。三类引用独立领取；排行榜和聊天仍按插入顺序最多保留64项；菜单保留原有无上限行为；菜单及排行榜发送失败仍立即撤回引用。新增3项状态契约测试，全量35项测试通过。正常/错误远端响应已接入，真实服务交互尚未验证。

B4-room-directory：房间列表缓存、等待请求、在途标记和客户端刷新摘要已委托给 Kotlin `RoomDirectoryState`。普通打开在缓存不足1秒时复用，主动刷新在缓存不足4.5秒时复用，边界时刻重新查询；同一玩家的合并请求保留较弱的刷新要求；服务错误清空全部等待请求，发送失败只撤回当前玩家；只有成功发送客户端包后才记录摘要。新增4项状态契约测试，全量39项测试通过。房间协议解析和玩家发送仍在 Java 入口，真实客户端/服务端联调待验证。

B4-team-selection：`MirrorFactory` 的远端队伍预选已迁移到 Kotlin `TeamSelection`，远端单玩家镜像和本地双玩家对战共用同一入口。有效序号按远端顺序选取；缺少或空选择沿用原队伍；非数字、负数、越界和重复序号回退完整队伍。新增4项分支契约测试，全量43项测试通过。实体生成、BattleRegistry 启动及 Mixin 绑定仍未集成验证。

B4-mirror-participants：`MirrorBattle` 的参与者、座位、观战者和双方 `BattleInfo` 已委托给 Kotlin `MirrorParticipantState`。Java 公开构造器、方法和嵌套 record 保持；双本地玩家顺序、座位映射、未知玩家空结果、观战者最后离开判断及双方描述分别验证。新增3项状态契约，原投影/路由8项继续通过，全量46项测试通过；原始 JAR 与当前 class 的 `javap -public` 输出一致。实体引用、Showdown 输出解释和游戏内结束流程待验证。

B4-mirror-entities：`MirrorBattle` 创建的 NPC/远端 actor 组合及临时 Pokémon 实体引用已委托给 Kotlin `MirrorEntityRegistry`。快照不转移清理责任，`take` 返回当前不可变副本后清空，空引用不登记。新增2项容器契约，全量48项测试通过；`MirrorBattle` 公开 `javap` 签名继续与原始 JAR 一致。真实实体生成、延迟清理和异常回收仍待游戏内验证。

B4-format-resolution：`MirrorFactory` 的远端规则描述解析已迁移到 Kotlin `BattleFormatResolver`，远端与本地双方创建路径共用。缺少 `formatJson` 时按 `format` 标识使用 Cobblemon 默认格式；显式规则去重并保留顺序；空规则沿用基础格式；`adjustLevel` 按远端值覆盖。新增3项契约测试，全量51项测试通过。真实 BattleRegistry 启动仍待验证。

B4-clean-build：在 JDK 21 下执行 `./gradlew.bat --offline clean build --console=plain`，9个任务全部实际执行，51项测试、0失败，thin JAR 与包含隔离依赖的发布 JAR 均生成。日志位于 `D:/workspace/gradle-b4-clean-build-20260915.log`。此证据覆盖编译、测试和打包，不替代客户端、服务端及远端对战服务集成验证。

B4-battle-result：结束帧的胜负和分数解析已迁移到 Kotlin `BattleResultProjection`。`win` 只在获胜座位非空时映射胜负，`tie` 映射平局，其余原因映射中止；分数按远端顺序投影，非法 UUID 忽略，缺失数值沿用0。指定玩家查找保持遇到首个匹配立即返回，不解析后续坏数据。新增3项契约测试，全量54项测试通过。事件触发和玩家消息副作用仍由 Java 服务入口执行。

B4-pokemon-ownership：`MirrorPokemon` 的 Pokémon UUID 到镜像对战归属表已委托给 Kotlin `MirrorOwnershipIndex`。重复登记由最新镜像接管，释放未知 UUID 无副作用，释放后查询为空。新增2项状态契约，全量56项测试通过；`MirrorPokemon` 公开 `javap` 签名与原始 JAR 一致。实体加入世界、标签写入和残留实体销毁待游戏内验证。

B4-clean-build-56：在提交 `a415554` 后使用 JDK 21 执行 `./gradlew.bat --offline clean build --console=plain`，9个任务全部实际执行，56项测试、0失败，thin JAR 与发布 JAR 均生成。扫描 `src/main/kotlin` 未发现空断言或 `@Suppress`。日志位于 `D:/workspace/gradle-b4-clean-build-56-20260915.log`。

B4-npc-state：`MirrorNpc` 的活跃实体 UUID、皮肤缓存和显示名规范化已委托给 Kotlin `MirrorNpcState`。显示名裁剪空白并去除首个 `#` 后的编号，空结果不查询；活跃登记/移除幂等；缓存由最后一次结果覆盖。新增3项状态契约，全量59项测试通过；`MirrorNpc` 公开 `javap` 签名与原始 JAR 一致。Mojang 皮肤查询、主线程应用和 NPC 世界生命周期待验证。

B5-public-abi：首次全量扫描发现 `CobbleBattleConfig` 比原 JAR 多出公开 `validate()`；校验逻辑迁入 Kotlin `ConfigurationValidation`，Java 类恢复原公开表面，包级 `GSON` 保留。随后从原 JAR 的152个项目自有候选 class 中识别123个 public class，与当前发布 JAR 逐项比较 `javap -public`，缺失0、差异0。配置契约及全量59项测试通过。审计结果位于 `D:/workspace/cobblebattle-public-abi-filtered-20260915.json`；运行时事件顺序另行验证。

B3-payload-contract：16个公开payload record保留原JVM形态，TYPE创建委托给Kotlin `PayloadTypeCatalog`。新增4项契约逐项固定16个资源ID，并覆盖全部CODEC的字符串、UUID、整数、布尔值、嵌套record和列表顺序；解码后缓冲区无剩余字节；原JAR与当前JAR的23个network公开class逐项`javap -public`一致。为测试源码补齐ModDev主编译类路径，JDK21离线全量63项测试、0失败。`CobbleBattleNetwork`注册侧、主线程调度与真实客户端收发仍待验证。

B3-network-routing：`CobbleBattleNetwork`保留原公开静态入口，6类C2S接收注册、10类S2C服务端注册及下发能力门控迁入Kotlin `PayloadChannelCoordinator`。消息不受支持时保持惰性且不发送，合法服务端玩家任务先入主线程队列，其他对象忽略。新增3项策略契约，全量66项测试、0失败；network包23个公开class的`javap -public`继续与原JAR一致。实际环境注册及客户端/服务端收发仍待验证。

B4-mirror-cleanup：包内Java `MirrorTeardown`迁移并重命名为Kotlin `MirrorLifecycleCleanup`，保留先领取实体引用、立即释放镜像Pokemon归属、按宽限时间提交主线程销毁、异常终止关闭BattleRegistry、最后通知本地参与者的顺序。新增2项调度边界契约覆盖零/负延迟立即执行及正延迟只提交任务；全量68项测试、0失败，`CrossServerBattleService`公开签名一致。世界实体、BattleRegistry关闭、断线和延迟2000ms回收仍待游戏内验证。

B4-construction-attempt：`MirrorFactory`的远端单玩家与双本地玩家路径统一通过Kotlin `BattleConstructionAttempt`执行`BattleRegistry.startBattle`并结束构造上下文。成功返回空失败；`RuntimeException`返回原Java通知/回收分支；其他错误继续抛出；三种退出均执行`CrossServerBattles.endConstruction`。新增3项顺序契约，全量71项测试、0失败。实体装配、mixin未触发和游戏内启动确认仍待验证。

B3-B4-clean-71：JDK21下执行`./gradlew.bat --offline clean build --console=plain`成功，9个任务全部实际执行，71项测试、0失败，thin JAR与发布JAR均生成；日志位于`D:/workspace/gradle-clean-b3-b4-71-20260915.log`。公开ABI沿用`8e0c7a5`的123类全量一致结果，并对后续受影响的23个network类和`CrossServerBattleService`增量复核一致；`MirrorFactory`及已删除的`MirrorTeardown`均非公开类。客户端、专用服务端和远端服务运行仍未验证。

B6-residue-cleanup：删除失效的`build.ps1`、旧IDEA模块、反编译摘要/空错误日志、导入清单、javac参数文件及无入站依赖的合成`PlatformMethods`。源码与jdeps引用扫描未发现合成类调用；删除后JDK21离线clean build的9个任务全部执行，71项测试、0失败，发布JAR不再包含`architectury_inject_*`条目。Gradle构建与IDEA模型继续作为唯一项目构建入口。

B5-mixin-contract：`cobblebattle.mixins.json`与原JAR文本一致，4个客户端类和2个通用类均存在。新增3项元数据契约固定13个`@Inject`目标名、图鉴`@ModifyArg`调用目标及两套`@ModifyArgs`完整描述符和可选要求。服务与Showdown注入共用Kotlin `BattleIdentifierParsing`，新增1项UUID正常/异常边界契约。全量75项测试、0失败，`CrossServerBattleService`与`GraalShowdownServiceMixin`公开签名一致；实际Mixin应用和游戏内副作用待验证。

B5-neoforge-entry：Java `CobbleBattleNeoForge`迁移为同包同名Kotlin入口，保留`@Mod("cobblebattle")`、唯一公开零参数构造、先执行公共初始化及仅`Dist.CLIENT`执行客户端初始化的顺序。新增1项入口元数据契约，全量76项测试、0失败；忽略`Compiled from`来源文件名后，`javap -public`签名与原JAR一致。FML实际实例化和分侧类加载仍待客户端/专用服务端启动验证。

B5-clean-76：最新Kotlin入口提交后使用JDK21执行`./gradlew.bat --offline clean build --console=plain`成功，9个任务全部实际执行，76项测试、0失败，thin JAR与发布JAR均生成；日志位于`D:/workspace/gradle-clean-b5-76-20260915.log`。该证据覆盖完整编译、资源、测试和打包，仍不替代IDEA界面导入、NeoForge进程启动与真实远端服务联调。

B5-client-handlers：认证、聊天、排行榜、图鉴、房间、主菜单和队伍预览的 S2C 注册及客户端输入事件迁入 Kotlin `ClientHandlerRuntime`，原七个 Java handler 的公开静态入口和包内发送入口保持。所有消息处理继续先调用 `context.queue`；聊天按键、鼠标和字符事件的拦截条件及初始化顺序保持。房间列表、房间状态和队伍预览通过 `ClientViewTransitions` 固定刷新、打开、忽略规则，5项测试覆盖无玩家、当前界面匹配、刷新包、战斗中和关闭原因分支。JDK21离线clean build的9个任务全部执行，81项测试、0失败；七个公开handler逐项与原JAR的`javap -public`一致。实际客户端注册、收包、按键和界面副作用待游戏内验证。

B5-command-runtime：`cbattle` 根命令及 open、logout、join、leave、check、status、reload 七个执行链迁入 Kotlin `CommandExecutionRuntime`，原十个公开命令类型保留。排行参数缺省值、空清单放行、未知排行拒绝、连接字段优先重连及拒绝后重试由 `CommandDecisions` 固定；命令树测试固定子命令、两个 `ranked` 参数节点和 reload 二级权限。JDK21离线clean build的9个任务全部执行，87项测试、0失败；命令目录十个公开类型逐项与原JAR的`javap -public`一致。实际服务器权限、聊天反馈、配置重载与远端连接副作用待游戏内验证。

B4-dex-snapshot：`RemoteDex` 的物种快照表、摘要和就绪状态委托给 Kotlin `RemoteDexSnapshotState`。采用快照保持六项能力值和不可变映射；服务端确认未变化时仅在本地缓存非空时就绪；暂停保留快照和摘要，失效清空；读取坏文件继续清空缓存且等待重新拉取。新增5项测试，包括真实 `config/cobblebattle-dex.json` 写入、恢复和 unchanged 确认。JDK21离线clean build的9个任务全部执行，92项测试、0失败；`RemoteDex`公开签名与原JAR一致。真实 Pokémon 能力、招式、EV/IV 规则及客户端图鉴替换待集成验证。

B5-client-dex-state：`ServerDex` 的服务端消息摘要、能力值索引和最近排行委托给 Kotlin `ServerDexSnapshotState`。完整快照按六项 Stat 建索引；unchanged 在缓存为空或摘要不符时清空摘要并请求完整快照；形态无数据时回退物种。新增4项状态测试。JDK21离线clean build的9个任务全部执行，96项测试、0失败；`ServerDex`公开签名与原JAR一致。Dexes全局替换、反射记录填充和GUI关闭恢复待客户端验证。
