# 行为契约与证据

B4-packed-details-compatibility最终证据：clean build全量150项测试通过，9个任务全部执行；RemoteTeamCodec公开ABI一致。5项附加字段契约包含48组输入的原实现调用决策对照，恢复空槽位置及非法数值回退后的setter调用；真实世界对象副作用仍待集成验证。

B4-packed-details-compatibility（当前纠正）：dffef6f引入的空招式过滤会错配槽位和PP；将非法亲密度/PP表示为空也省略了原setter调用。已恢复原始槽位和原始数值文本，在Java装配时读取对象当前值作为回退。PackedTeamDetailsTest新增空槽测试及48组对照930c9db分割/调用决策的差分输入；定向5项通过，全量验证进行中。此前“过滤空项兼容”的记录撤回。历史失败实际原因是Kotlin split禁止负limit，以及非法PP错误回退为0；与Array(17)构造无关。差分测试覆盖调用决策，不替代真实Cobblemon setter和注册对象的运行验证。

基线版本 baseline-local-20260914；仅全量编译通过，以下行为尚未建立运行证据。

| 链路 | 原实现 | 正常、异常及副作用约束 | 新实现/证据 | 状态 |
| --- | --- | --- | --- | --- |
| 配置 | CobbleBattleConfig / ConfigFile / YamlTree | 首次写模板；空映射默认值；非法端口回退18470；帧上限至少1MiB；读取失败回退；reload 失败抛异常并保留原值；区分连接参数 | Kotlin 实现；配置5项契约通过 | 已验证 |
| 服务器身份 | ServerIdentity | 复用非空文件；空/不可读时生成；保存失败仍返回；并发缓存；mc-前缀 | Kotlin 实现；身份3项契约通过 | 已验证 |
| TCP 传输 | BattleServerClient | 压缩流、四字节长度、引用序号、握手、按需连接、拒绝、重连及回调 | Kotlin 状态机；Socket契约2项通过，重连交错待验证 | 待验证 |
| 认证 | AuthService / AuthMode | 枚举顺序及越界LOGIN；登录注册绑定邮箱验证码；事件及清理 | Kotlin 会话与消息；5项契约通过，远端联调待验证 | 待验证 |
| 游戏包 | network/* | 17个文件；包ID、字段顺序、线程调度、客户端/服务器路由 | 16个包ID、全部CODEC及收发调度策略契约通过；客户端和专用服务端环境注册成功 | 待验证 |
| 排队及对战 | BattleQueue / CrossServerBattleService | 加入退出、远端匹配、选择与回合中继、断线、结束清理、事件顺序 | 队列与服务请求状态已提取；状态契约5项通过，游戏链路继续实施 | 进行中 |
| 镜像及观战 | MirrorBattle / MirrorFactory / SpectatorFactory | 实体、队伍、座位、回合、退出及错误回收 | 索引、序号缓存、启动作用域及回收调度契约通过；实体生命周期待验证 | 待验证 |
| 图鉴 | RemoteDex / ServerDex | 远端数据、同步、缓存及错误处理 | 快照、客户端索引及合法性纯规则共13项契约通过；真实Pokémon和客户端全局图鉴替换待集成验证 | 进行中 |
| UI及命令 | client/* / command/* / lang/* | 权限、显示、输入、翻译、数据请求、副作用 | 客户端接收器、聊天编辑状态和七个命令执行链已迁入Kotlin；界面切换、输入、参数选择和命令树17项契约通过，其余界面待审查 | 进行中 |
| 公开API | api/* | 类名、record访问器、事件类型、静态字段及JVM签名 | 原JAR 123个公开class与当前发布包逐项 `javap -public` 一致；事件运行待验证 | 待验证 |
| Mixin及引导 | mixin/* / neoforge/* / CobbleBattle | 注入目标/描述符、客户端隔离、注册顺序 | 配置、类清单、13个Inject目标及3个绘制调用描述符契约通过；客户端和专用服务端均完成模组构造与资源加载 | 待验证 |

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

B4-dex-legality：`RemoteDex` 的 Showdown ID、能力许可、招式集合和数值上限判断委托给 Kotlin `DexLegalityRules`。名称继续按ROOT小写并剔除非字母数字；空能力、noability、空合法招式集合及零上限保持原放行行为；边界等于上限合法。新增4项纯规则测试。JDK21离线clean build的9个任务全部执行，100项测试、0失败；`RemoteDex`公开签名与原JAR一致。真实Pokémon对象、拒绝文本和队伍顺序待游戏内验证。

B5-client-startup：首次执行 `runClient` 依次发现开发运行时缺少 KotlinForForge 和 SnakeYAML。KotlinForForge 5.3.0 作为运行模组加入，SnakeYAML 同时保留发布包重定位配置并加入 ModDevGradle `additionalRuntimeClasspath`。修复后实际启动完成 NeoForge、KotlinForForge、Cobblemon、CobbleBattle 构造，Showdown 启动，资源全部加载且进程保持稳定；Cobblemon 自身的可选 Adorn Mixin、资源路径和缺失音效警告未影响启动。模板差异日志新增1项契约，确认受保护字段值不出现在消息中。JDK21离线clean build的9个任务全部执行，101项测试、0失败。客户端收包、具体界面、世界内Mixin回调和专用服务端仍待验证。

B5-server-startup：`runServer --nogui` 实际完成 NeoForge、KotlinForForge、Cobblemon 和 CobbleBattle 构造，Showdown 服务启动并预热，数据注册完成，世界生成后服务端报告 `Done`。启动日志没有 CobbleBattle FATAL/ERROR；Cobblemon 对客户端声音类的分侧探测、可选 Adorn 目标和资源标签诊断未阻止服务端运行。网络环境注册随公共初始化完成且未抛异常。真实玩家登录、C2S/S2C往返、远端对战服务连接及世界实体生命周期仍待验证。

B5-chat-composer：`ChatInput` 的可变草稿和编辑状态委托给 Kotlin `ChatComposerState`。未输入时字符返回未消费；输入时空格可写，控制字符、DEL和格式控制符拒绝；达到200个UTF-16单元后继续返回已消费但不增长；退格按完整Unicode码点删除；暂停/取消隐藏并保留草稿；提交沿用Java `trim` 范围后清空。新增状态5项及Java兼容入口1项测试，原聊天3项继续通过。JDK21离线clean build的9个任务全部执行，107项测试、0失败；`ChatInput`公开签名与原JAR一致。真实键盘事件和消息下发待客户端交互验证。

B5-client-settings：`ClientSettings` 的加载标记和聊天HUD开关委托给 Kotlin `ClientSettingsStore`。首次访问只请求一次路径；缺少文件保留开启默认值；合法 `chatHud` 覆盖默认值；损坏文件记录一次读取诊断且同实例不重试；设置开关先更新内存，再创建父目录并写出单字段JSON；保存失败保留内存值并记录诊断。新增5项文件契约。JDK21离线clean build的9个任务全部执行，112项测试、0失败；`ClientSettings`公开签名与原JAR一致。设置界面点击和重新启动客户端后的实际读取待交互验证。

B5-auth-form：`AuthScreen` 的初始模式、标题/提交/切换翻译键、邮箱字段要求、模式循环、按钮可用性、验证码倒计时和请求参数委托给 Kotlin `AuthenticationFormRules`。CODE_REQUEST初始转REGISTER；禁用邮箱时BIND_EMAIL回LOGIN；邮箱启用时LOGIN→REGISTER→BIND_EMAIL循环；密码只要求非空且保持原值，标识、邮箱、验证码沿用Java `trim`；注册密码不匹配不生成请求；绑定验证码请求保留固定账户标记。新增7项规则契约。JDK21离线clean build的9个任务全部执行，119项测试、0失败；`AuthScreen`公开签名与原JAR一致。控件焦点、响应后的关闭/清空和远端认证待客户端交互验证。

B5-team-preview：`TeamPreviewScreen` 的已选槽位列表委托给 Kotlin `TeamPreviewSelectionState`。首次点击按顺序添加，重复点击移除；达到上限后新槽位忽略但点击仍由界面消费；截止时刻采用 `now >= deadline`，关闭原因或本方已准备均锁定；确认要求未锁定且选择数精确等于要求；倒计时毫秒向上取整。新增4项状态契约。JDK21离线clean build的9个任务全部执行，123项测试、0失败；`TeamPreviewScreen`公开签名与原JAR一致。真实鼠标命中、TeamPickPayload下发和双方准备后启动待联调。

B5-room-lobby：`RoomLobbyScreen` 的创建选项委托给 Kotlin `RoomCreationOptions`，创建提交值由 `RoomCreationRequest` 固定，邀请码和座位类型由 `RoomLobbyRules` 判断。默认值保持 singles、自由等级、六选六、完整治疗、服务端引擎和合法性检查；类型、等级和选择数双向循环；主机引擎使密码行后移并显示合法性开关；房间名和创建密码沿用 Java 裁剪，空名称回退默认值；加入密码保持原输入；空邀请码不提交也不关闭表单；战斗中房间使用空座位类型。新增5项规则契约。JDK21离线clean build的9个任务全部执行，128项测试、0失败；`RoomLobbyScreen`公开签名与原JAR一致。真实鼠标命中、网络下发和远端房间交互待联调。

B5-room-controls：`RoomScreen` 的开战冷却和复制提示委托给 Kotlin `RoomInteractionState`。只有角色为 host、已有 guest、尚未 fighting 且冷却为零时可开始；成功开始立即设置200 tick冷却并阻止重复请求；复制邀请码设置40 tick提示；两个计数每tick递减且不低于零。新增4项状态契约。JDK21离线clean build的9个任务全部执行，132项测试、0失败；`RoomScreen`公开签名与原JAR一致。点击命中、剪贴板、leave/start发包和战斗出现后的关屏仍待客户端联调。

B4-team-preview-sessions：`TeamPreviews` 的玩家会话表委托给 Kotlin `TeamPreviewSessionDirectory`，默认选择和客户端选择检查委托给 `TeamPreviewRules`。同一玩家的新会话覆盖旧会话；状态和关闭帧只处理相同 battleId 的快照；无客户端时依次选取请求数与队伍长度的较小值；客户端选择先检查精确数量，再拒绝负数、越界和重复序号，合法顺序原样发送。新增4项状态契约。首次定向编译因 Java 沿用 record 风格访问器失败，改用 Kotlin getter 后通过。JDK21离线clean build的9个任务全部执行，136项测试、0失败。真实 preview_open/state/closed/pick 往返和玩家断线待联调。

B4-packed-team-values：`RemoteTeamCodec` 的基础标量解析委托给 Kotlin `PackedTeamValueParsing`。整数先按 Java `trim` 范围裁剪，解析失败沿用调用处回退值，解析成功限制在指定上下界；UUID合法时保留，非法时返回空并由Java入口记录原警告；性别只接受协议大写 M/F，其余均映射 GENDERLESS。等级、亲密度、当前HP、IV、EV和招式PP共用有界整数入口。新增4项边界契约。JDK21离线clean build的9个任务全部执行，140项测试、0失败；`RemoteTeamCodec`公开签名与原JAR一致。物种/形态、能力、招式、太晶属性和实体标记待真实数据验证。

B4-spectator-format：`SpectatorFactory` 删除重复的 Java 规则解析并调用 Kotlin `BattleFormatResolver.resolveSpectator`。观战 `formatJson` 缺少 `battleType` 时沿用外层 `format`，存在内层类型时以内层为准；规则去重、空规则回退和 `adjustLevel` 继续复用已验证实现。新增2项观战分支契约，原3项普通镜像格式契约继续通过。JDK21离线clean build的9个任务全部执行，142项测试、0失败。观战实体装配、数据包下发和最后观战者离开后的回收待游戏内验证。

B4-packed-team-details（2026-09-15）：`RemoteTeamCodec` 使用 Kotlin `PackedTeamDetailsParser` 读取固定字段。性质和能力为空时保持空值；招式名称最多四项并过滤空项；PP 保留位置，仅带 `/` 且可解析的当前值进入对象装配，异常值沿用原对象默认；亲密度限制在0至255，太晶字段保留原文本交给 Java 名称/标识解析。新增3项契约。JDK21离线clean build的9个任务全部执行，145项测试、0失败，日志位于`D:/workspace/gradle-clean-b4-packed-details-20260915.log`。物种/形态索引、Cobblemon能力与招式注册、太晶名称和实体标记待真实数据验证。

B4-species-index：RemoteTeamCodec 使用 Kotlin SpeciesLookupIndex 构建 Showdown 标识索引。每个物种先登记标准形态，再按原顺序登记显式形态；putIfAbsent 语义保留重复标识的首次值；空集合返回空索引。新增3项纯规则契约。修复 Java 缓存声明残留旧私有类型的编译错误后，JDK21离线clean build的9个任务全部执行，148项测试、0失败；RemoteTeamCodec公开签名与原JAR一致。真实 Cobblemon 物种注册顺序和形态数据仍待游戏内验证。

B4-spectator-recovery：离线build成功，154项测试零失败。观战构造接入Kotlin回收控制，保持start、回收、finish顺序；4项新测试覆盖成功、RuntimeException、回收异常和Error。真实NPC/队伍回收仍待游戏内验证。日志D:/workspace/gradle-spectator-recovery.log。

B4-packed-moves (base eb73675)：远端招式装配迁入Kotlin PackedMoveAssembly，RemoteTeamCodec保留入口。空槽、未知招式、四项限制、PP读写顺序、非法值回退及异常后部分写入由4项新契约约束；离线build全量158项通过，日志D:/workspace/gradle-packed-moves.log。Cobblemon注册招式及真实对象setter仍待游戏内验证。

B4-packed-stats (base 12b5411)：六维IV/EV装配迁入Kotlin PackedStatAssembly，保留HP/攻击/防御/特攻/特防/速度顺序。4项新测试覆盖空字段、维度错误、尾空值、上下界、非法整数和写入异常的部分副作用。JDK21离线clean build成功，9个任务全部执行，162项测试零失败；RemoteTeamCodec公开javap签名与原JAR一致。日志D:/workspace/gradle-packed-stats.log。真实Cobblemon对象及世界行为尚待验证。

B4-remote-roster (base 30a0358)：RemoteTeamCodec剩余队伍解码、物种缓存、性格/能力/亲密度/太晶/HP设置及BattlePokemon包装迁入Kotlin RemoteRosterAssembly，Java只保留三个公开委托入口。5项新测试包含7组Java过滤/槽位编号差分、包装异常中止、可选属性Exception隔离/Error传播及公开入口无效输入。JDK21离线clean build成功，167项测试零失败，9个任务全部执行；公开ABI与原JAR一致。日志D:/workspace/gradle-remote-roster.log。实际注册对象、prop标记、召回动画和远端联调仍未验证。

B4-npc-runtime (base 17520c5，2026-09-16)：MirrorNpc的实体创建/销毁、皮肤查询迁入Kotlin MirrorNpcRuntime；公开Java入口保留。MirrorStageLayout保持原Vec3几何和朝向；NpcSkinDelivery保持缓存命中同步应用、异步先缓存后主线程检查移除状态。新增5项测试，11组站位对照；全量172项通过，公开ABI一致，日志D:/workspace/gradle-npc-runtime-final.log。首次编译的可空模型错误已改为非空默认模型，空URL仍禁止加载；字节码审查发现头部旋转属性会直写字段，已恢复显式setYHeadRot。世界实体、Mojang查询和实际纹理仍待验证。原addFreshEntity/皮肤调度抛异常时的登记残留语义保留，未擅自增加回收。

B4-prop-entities (base f2b00cc)：MirrorPokemon实现迁入Kotlin MirrorPropEntities；MirrorPropTracker集中处理归属优先挂接、遗留标签销毁和返回值。4项新契约覆盖重复接管/释放、空Pokemon、普通实体及异常传播。全量176项测试通过，MirrorPokemon公开ABI一致，日志D:/workspace/gradle-mirror-props-final.log。实体标签写入、挂接及世界销毁仍待集成验证。

B4-spectator-sessions (base d3e93c0)：观战创建/挂接/结束迁入Kotlin SpectatorSessions，Java包内入口保留；SpectatorTeamLayout返回明确的缺描述、空队伍、缺席位与成功结果。4项新增测试约束输入重建顺序、非p1归第二席、重复席位末值和空队伍先于UUID验证。clean build全量180项通过，9任务全部执行，日志D:/workspace/gradle-spectator-sessions.log。首次编译发包引用失败，已使用CobblemonNetwork.sendPacket成员扩展，发送顺序不变。回收在构造上下文内、无localBattleId单独回收、ack后release及最后观战者离开回收均保留；实际网络/实体/Mixin仍待验证。

B4-mirror-sweep (base eaa604d)：MirrorLifecycleCleanup使用MirrorSweepSequence约束归属释放、服务端查找、延迟和主线程销毁顺序。4项新测试覆盖空快照、服务端缺失、零/负/正宽限时间、prop先于body及异常中止。JDK21离线clean build成功，184项测试（52套）零失败/错误/跳过，9任务全部执行；日志D:/workspace/gradle-mirror-sweep.log。MirrorNpc/MirrorPokemon/SpectatorFactory包内与公开可调用签名一致；javap差异仅静态初始化块迁入Kotlin。真实世界回收仍待验证。

B4-match-opponent (base 2881468)：匹配对手解析迁入Kotlin MatchOpponentParsing。保留最后一个不同席位、仅解析最终对手、主机标志缺省/空值为false和非法最终UUID向外传播。4项新测试，全量188项通过，日志D:/workspace/gradle-match-opponent.log；实体建场继续待验证。

B4-matched-assembly (base 5c5127c)：MirrorFactory远端和双本地建场迁入Kotlin MatchedBattleAssembly，Java保留包内构造/build签名。LocalMatchClaims保留双次领取及第一次缺失仍领取第二次；构造异常回收仍在endConstruction后执行，缺失Mixin不额外forget，成功通知/API事件先于ack且不新增release。新增3项领取契约与2项构造边界测试；clean build后局部复核build均成功，全量193项通过。日志D:/workspace/gradle-matched-assembly.log及gradle-matched-assembly-final.log。包内可调用ABI与原JAR一致；真实NPC/BattleRegistry/远端匹配与事件副作用尚待集成验证。

## B4-battle-control-messages（2026-09-16）

对战建场确认帧仍为 `{"t":"battle_ack","battleId":...}`，中止帧仍为 `{"t":"battle_abort","battleId":...,"reason":...}`。字段按既有顺序写入，null 继续编码为 JSON null，文本不裁剪；每次调用创建独立对象。匹配建场和观战建场在成功通知及 API 事件之后发送确认，断线或玩家离开时沿用原中止原因。证据为 `BattleControlMessagesTest` 三项契约及 2026-09-16 JDK21 离线 clean build；真实远端确认/中止响应尚未验证。

## B4-queue-rules（2026-09-16）

QueueRules.requiredSlots 对 null、空值和未知类型返回1，对 doubles/double/double_battle 返回2，对 triples/triple/triple_battle 返回3，使用 ROOT 小写规则。BattleQueue 的提示键、队伍数量比较和消息参数顺序不变。证据为 QueueRulesTest 及 D:/workspace/gradle-queue-rules.log 的测试通过；真实队伍构建和远端房间交互仍待验证。

## B4-queue-player-payload（2026-09-16）

排队、房间查询、离开、开战和断线请求继续发送 {uuid,name} 玩家对象，字段顺序为 uuid 后 name；UUID 使用标准文本，名称由 Gson 原样 JSON 编码，不做裁剪。两项 payload 契约及离线 test 构建通过，真实远端请求往返待验证。

## B4-queue-message-rules（2026-09-16）

房间关闭原因 host_left、started、banned、finished、gone 继续映射原翻译键，未知值不提示；队列离队 busy、banned 映射专用键，其他值回退 queue.left。两项规则测试及 JDK21 离线 test 通过，真实服务端响应待验证。

## B4-queue-reference-cleanup（2026-09-16）

玩家断线时，等待队伍仍被移除；若存在等待队伍，继续发送 queue_leave。新增清理会同时删除该玩家尚未领取的 owner/lookup 引用，避免断线后的迟到响应再次命中玩家；其他玩家的引用保持不变。QueueReferenceBookTest 新增断线引用回收契约，JDK21 离线 test 通过；真实连接断开和服务端响应竞态待验证。

## B4-queue-error-rules（2026-09-16）

房间拒绝码与邀请码查询失败码的翻译键映射迁入 Kotlin `QueueErrorRules`。房间拒绝继续覆盖 `NO_SUCH_ROOM`、`ROOM_LOCKED`、`BAD_INVITE_CODE`、`OWN_ROOM`、`NOT_ROOM_HOST`、`ROOM_NOT_READY`；查询失败继续覆盖 `BAD_INVITE_CODE`、`NOT_LOGGED_IN`、`ALREADY_QUEUED`、`ALREADY_IN_BATTLE`、`DEX_NOT_READY`，未知值仍由调用方使用原回退文本。认证和聊天错误保持原有独立映射，避免跨业务复用导致翻译键变化。证据为 `QueueErrorRulesTest` 两项契约及 `D:/workspace/gradle-queue-error-rules.log` 的 JDK21 离线 `clean build` 成功；真实服务端错误帧和客户端提示仍待联调。

## B4-room-state-decoding（2026-09-16）

跨服 room_state 消息的玩家标识、房主/客人、观战者列表和默认字段解码迁入 Kotlin RoomStateDecoding。无效玩家标识继续忽略消息；缺少房主或客人时使用 NOBODY，非对象观战者继续过滤，engine=host、fighting 和 youAre 的字段语义保持不变。证据为 RoomStateDecodingTest 三项契约及 D:/workspace/gradle-room-state-decoding-clean.log 的 JDK21 离线 clean build，207 项测试、0 失败、0 错误；真实服务端 room_state 往返和客户端界面展示仍待联调。

## B4-room-list-decoding（2026-09-16）

跨服 room_list 中房间对象的字段读取、默认值和非对象过滤迁入 Kotlin RoomListDecoding。缺少 rooms 或数组类型不符时继续得到空列表；房主引擎、战斗状态、观战人数、锁定和合法性字段保持原默认值及类型。CrossServerBattleService 的 unchanged 缓存分支、哈希和交付副作用保持原位置。证据为 RoomListDecodingTest 三项契约及 D:/workspace/gradle-room-list-decoding.log 的 JDK21 离线测试，210 项测试、0 失败、0 错误；真实房间列表响应和客户端刷新展示仍待联调。

## B4-leaderboard-decoding（2026-09-16）

跨服排行榜响应的条目字段、	op 数组过滤、you 缺省对象和默认值迁入 Kotlin LeaderboardDecoding。CrossServerBattleService 保留 ref 领取、菜单回退和网络发送顺序。证据为 LeaderboardDecodingTest 两项契约及 D:/workspace/gradle-leaderboard-decoding-clean.log 的 JDK21 离线 clean build，212 项测试、0 失败、0 错误；真实排行榜响应和客户端展示仍待联调。

## B4-player-identity-payload（2026-09-16）

原排队专用命名的玩家协议对象更名为 `PlayerIdentityPayload`，并复用于菜单、排行榜和聊天请求。所有消息继续按 `uuid` 后 `name` 的顺序写入，名称文本不裁剪；排队、房间、菜单、排行榜和聊天请求的字段结构保持一致。证据为原两项 JSON 契约及 `D:/workspace/gradle-player-identity-payload.log` 的 JDK21 离线测试，212 项测试、0 失败、0 错误；真实跨服请求待联调。
## B4-service-error-rules（2026-09-16）

认证与聊天错误码分别迁入 Kotlin `AuthenticationErrorRules` 和 `ChatErrorRules`。认证保持十二个账户错误键；聊天保持限速、未在战斗、禁用和未登录四个错误键；未知码继续使用服务端回退文本，`MUTED` 的永久/限时分支仍保持原位置和时长格式。两套规则不接受对方的业务错误码。证据为 `ServiceErrorRulesTest` 两项契约及 `D:/workspace/gradle-service-error-rules.log` 的 JDK21 离线 `clean build`，214 项测试、0 失败、0 错误；真实认证和聊天错误帧仍待联调。
## B4-battle-queue-private-names（2026-09-16）

`BattleQueue` 的私有队伍准备、槽位不足说明、已准备请求发送、准备结果和拒绝异常改为职责明确的名称。改动仅涉及私有符号，排队检查顺序、合法性判断、消息字段、引用登记和发送失败回滚均未改变。JDK21 离线全量测试 214 项通过，日志 `D:/workspace/gradle-battle-queue-private-names.log`；真实队伍数据和远端交互仍待联调。
## B4-queue-rejection-message（2026-09-16）

队伍合法性拒绝列表到玩家消息的投影迁入 Kotlin `QueueRejectionMessage`。拒绝项保持输入顺序，槽位继续从零基转为一基显示，六种拒绝类型沿用原翻译键与详情参数，消息继续由红色标题、黄色槽位和灰色页脚组成。队伍加入与兼容性检查共用同一入口。两项契约覆盖顺序、槽位和全部拒绝类型；JDK21 离线 `clean build` 通过，216 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-queue-rejection-message.log`。真实 Pokémon 名称组件和客户端渲染仍待游戏内验证。
## B4-room-queue-requests（2026-09-16）

房间创建和加入请求的固定字段构造迁入 Kotlin `RoomQueueRequests`。创建请求保持字段顺序，主机引擎写入 `host` 并尊重合法性选项，服务端引擎写入 `server` 且强制合法性检查；加入请求只在邀请码非空时写入 `inviteCode`。ref、玩家、队伍和队伍元数据仍由发送链路随后追加，顺序不变。三项协议契约及 JDK21 离线 `clean build` 通过，219 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-room-queue-requests.log`。真实房间服务往返仍待联调。
## B4-queue-action-requests（2026-09-16）

匹配加入/离开与房间查询/离开/开战请求迁入 Kotlin 协议构造器。主动离队继续包含 `ref`，断线离队继续省略 `ref`；玩家对象仍保持 `uuid`、`name` 顺序。房间动作保持 `t`、`ref`、`player` 顺序，查询请求随后追加 `inviteCode`。动态队伍字段仍由发送链路追加。三项新增契约连同既有房间请求契约通过；JDK21 离线 `clean build` 成功，222 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-queue-action-requests.log`。真实远端请求和断线竞态仍待联调。
## B4-queue-send-rollback（2026-09-16）

`QueueReferenceBook` 集中执行查询、房主操作和等待队伍三类“先登记再发送”流程。发送返回 false 时，查询只撤销 lookup，房主操作只撤销 owner，队伍请求同时撤销 waiting 与 owner；发送抛出异常时保持原实现语义，异常继续传播且已登记状态保留。`BattleQueue` 三处发送路径接入该事务边界。新增两项契约覆盖成功、false 和异常；JDK21 离线 `clean build` 成功，224 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-queue-send-rollback.log`。真实套接字失败和并发响应仍待联调。
## B4-service-requests（2026-09-16）

菜单、排行榜与聊天请求构造迁入 Kotlin `ServiceRequests`，保持 `t/ref/ranked/player` 和 `t/ref/channel/text/player` 字段顺序；聊天频道仅精确 `battle` 保留，其余继续回退 `global`。`ServiceRequestLedger` 集中登记与发送：菜单和排行榜发送 false 时撤销引用，聊天发送 false 时按原行为保留引用，发送异常均传播并保留已登记状态。四项新增契约及 JDK21 离线 `clean build` 通过，228 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-service-requests.log`。真实服务响应和并发竞态仍待联调。
## B4-service-protocol-messages（2026-09-16）

握手、图鉴查询、房间目录查询、对战输出、选择转发和聊天观察者消息迁入 Kotlin `ServiceProtocolMessages`。握手保持协议版本 10、模组版本 1.0 和原字段顺序；图鉴摘要与房间哈希仅在存在时写入；对战与观察者消息保持原字段名和文本。配置中的接入值只作为调用参数传递，模板未改。三项协议契约及 JDK21 离线 `clean build` 通过，231 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-service-protocol-messages.log`。真实握手、图鉴和对战转发仍待远端联调。
## B4-ranked-decoding（2026-09-16）

远端排位赛配置解析迁入 Kotlin `RankedCompetitionDecoding`。非对象、空标识条目继续忽略；字段保持原默认值；规则数组只接受 JSON 原始值并按顺序转成文本；重复标识继续由后项覆盖，同时保留首次插入位置。`CrossServerBattleService` 保留清空、日志和后续菜单使用。三项契约及 JDK21 离线 `clean build` 通过，234 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-ranked-decoding.log`。真实服务端 ranked 配置仍待联调。
## B4-handshake-response（2026-09-16）

握手响应的图鉴、合法性、聊天、邮件、实例和物种计数字段迁入 Kotlin `HandshakeResponseDecoding`。缺失字段保持原默认值。图鉴决策保持原优先级：未就绪先使缓存失效；就绪且本地摘要非空并与远端相同则接受缓存；其余请求完整快照。排位读取、观察者上报、等待认证界面打开和日志顺序未改。三项契约及 JDK21 离线 `clean build` 通过，237 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-handshake-response.log`。真实远端握手和缓存文件仍待联调。
## B4-room-list-personalization（2026-09-16）

房间目录按接收者标记自有房间及生成交付摘要的逻辑迁入 Kotlin `RoomListPersonalization`。账号为零时不认领 hostUid 为零的房间；匹配账号的全部房间均复制为 `mine=true`，非自有房间继续复用原对象，顺序不变；存在任一自有房间时摘要追加 `:own`。缓存去重、网络发送成功后记录摘要的副作用保持原位置。两项契约及 JDK21 离线 `clean build` 通过，239 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-room-list-personalization.log`。真实账号状态和客户端房间列表仍待联调。
## B4-connection-lifecycle-rules（2026-09-16）

连接启动、按需连接和空闲释放的纯判定迁入 Kotlin `ConnectionLifecycleRules`。服务端启动仍在保活开启或存在在线玩家时连接；按需连接仍要求客户端存在、连接未被请求且没有拒绝原因；空闲延迟最低五秒；定时任务仍只在无人在线且连接仍被需要时主动断开。调度器创建、取消、线程切换和实际连接副作用保持原位置。三项契约及 JDK21 离线 `clean build` 通过，242 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-connection-lifecycle-rules.log`。真实计时、玩家进出和网络断开竞态仍待集成验证。
## B4-chat-line-decoding（2026-09-16）

远端聊天行的字段解析迁入 Kotlin `ChatLineDecoding`。空文本继续直接忽略；缺少频道时回退 global；缺少或无效玩家 UUID 时使用零 UUID；uid、账号、名称、battleId 和文本保持原默认值与原文。`CrossServerBattleService` 继续负责战斗频道按镜像席位分发、其他频道向已登录玩家广播，服务器缺失时不发送。三项契约及 JDK21 离线 `clean build` 通过，245 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-chat-line-decoding.log`。真实聊天广播和战斗频道成员仍待联调。
## B4-team-preview-roster（2026-09-16）

队伍预览席位阵容解码迁入 Kotlin `TeamPreviewRosterDecoding`，选择消息构造迁入 `TeamPreviewMessages`。阵容继续过滤非对象槽位并保持物种、等级、性别、闪光、道具默认值；对手席位继续取对象顺序中第一个不同键；选择序号和字段顺序原样发送。玩家查找、无客户端默认选择、会话保存和状态分发仍在原链路。三项契约及 JDK21 离线 `clean build` 通过，248 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-team-preview-roster.log`。真实预览消息与客户端界面仍待联调。

## B4-team-preview-events（2026-09-16）

队伍预览打开、状态和关闭事件的字段解析迁入 Kotlin `TeamPreviewEventDecoding`。打开事件继续区分缺少必填字段与无效玩家 UUID，阵容、选择数量、首发数量、对手信息沿用原默认值；远端截止时间仅在未来十分钟内采用，否则以第二次取时加一分钟。状态事件继续按自身席位赋值并对其他席位执行逻辑或，非原始值视为 false；关闭原因缺失时仍为 `closed`。会话登记、无客户端默认选择、网络发送、关闭清理和提示顺序保持在 Java 协调层。四项契约及 JDK21 离线 `clean build` 通过，252 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-team-preview-events.log`。真实远端预览往返和客户端界面仍待联调。

## B4-queue-response-decoding（2026-09-16）

房间对战详情、房间关闭、房间创建、排队确认、离队和等待位置六类响应迁入 Kotlin `QueueResponseDecoding`。详情继续在 fighting 为 true 时传空战斗类型，其他字段沿用原默认值；玩家字段继续使用 `UUID.fromString` 并传播无效值异常；等待位置继续直接读取 JSON 数值并传播错误类型异常。ref 引用仍在解析前领取，等待队伍清理、玩家消息、邀请码复制样式和 API 事件顺序保留在 Java 协调层。四项契约及 JDK21 离线 `clean build` 通过，256 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-queue-response-decoding.log`。真实跨服响应和并发竞态仍待联调。

## B5-battle-choice-restrictions（2026-09-16）

`BattleActorMixin` 中 Mega、Z-Move、Dynamax、Terastal 和 Ultra Burst 的条款匹配与提示键投影迁入 Kotlin `BattleChoiceRestrictions`。机制标识继续按根区域忽略大小写，规则条款仍精确匹配；空规则、未知机制和非玩家 actor 不取消选择。命中后仍依次清空已有响应、要求重新选择、发送队列请求与选择包、发送红色提示并取消回调，异常继续由原入口捕获和记录。三项规则契约、既有 Mixin 元数据契约及 JDK21 离线 `clean build` 通过，259 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-battle-choice-restrictions.log`。真实游戏内回调和发包顺序仍待验证，因此 Mixin 文件保持待验证。

## B6-symbol-coverage-baseline（2026-09-17）

本批次未改变业务行为。基线成员候选沿用既有公开接口、协议、API、Mixin 和生成成员排除规则，并补入具名内部类型；当前 Java 以完整 Gradle 编译类路径执行 javac 语义分析，97 个文件、4277 个声明、0 个分析错误。3487 个安全候选中仍有 2409 个原名精确存在，1078 个原声明键已消失，30.91% 仅是待人工对应的覆盖上限。剩余项写入 `RENAME_REMAINING.csv`，删除或迁移不会在建立职责对应前计作最终改名。

## B6-room-lobby-symbols-1（2026-09-17）

本批次只改写 `RoomLobbyScreen` 的私有纹理、布局、颜色、刷新状态、房间模型缓存和输入框名称，构造参数与更新参数由 javac 符号绑定同步改名。房间刷新周期、卡片几何、颜色数值、表单切换、网络载荷和公开覆写签名均未改变。JDK21 离线 `clean build` 通过，259 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-room-lobby-symbols-1.log`；重新生成的 javac 快照为 0 个分析错误，确认该文件 79 个原声明键消失。真实界面点击与渲染仍待客户端验证。

## B6-room-lobby-symbols-2（2026-09-17）

`RoomLobbyScreen` 的私有绘制、滚动条、卡片几何、命中检测、表单切换、发送入口和 `DialogMode` 类型完成语义改名。Screen 的公开构造、更新及输入覆写签名保持；纹理路径、坐标和颜色数值、点击分支、请求字段及发送时机未改变。JDK21 离线 `clean build` 通过，259 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-room-lobby-symbols-2-final.log`；当前 javac 快照为 0 个分析错误，精确匹配不到该文件的基线安全原名。真实客户端渲染、点击和网络发送仍待集成验证。

## B6-leaderboard-symbols（2026-09-17）

`LeaderboardScreen` 的私有纹理与布局常量、排行榜快照、当前玩家与选中条目、画像缓存、滚动位置、条目选择、命中处理和绘制流程完成语义改名。公开构造、更新入口、Screen 覆写及 `drawEntity` 方法名保持；榜单排序和选择规则、画像获取、颜色与坐标数值、滚轮边界、实体渲染参数及姿态恢复顺序未改变。JDK21 离线 `clean build` 通过，259 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-leaderboard-symbols-final.log`；当前 javac 快照为 97 个文件、4277 个声明、0 个分析错误，精确匹配不到该文件的基线安全原名。真实客户端渲染、鼠标交互和实体姿态副作用仍待游戏内验证。

## B6-auth-screen-symbols（2026-09-17）

`AuthScreen` 的认证模式、账号与邮箱输入、验证码请求、密码确认、提交状态、焦点顺序、内部按钮和绘制流程完成语义改名。公开构造、Screen 覆写、认证规则调用、字段长度、冷却时间、翻译键、颜色和坐标数值保持；包内结果回调改名后同步更新 Kotlin 客户端处理器。首次构建检出 Kotlin 仍调用旧回调，修正后 JDK21 离线 `clean build` 通过，259 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-auth-screen-symbols-final.log`；当前 javac 快照为 97 个文件、4277 个声明、0 个分析错误，精确匹配不到该文件的基线安全原名。真实注册、登录、邮箱绑定、验证码和焦点交互仍待客户端与远端联调。

## B6-team-preview-symbols（2026-09-17）

`TeamPreviewScreen` 的纹理与布局常量、预览状态、选择与锁定判断、阵容槽位、模型和姿态缓存、训练家面板、确认按钮、提示框及提交入口完成语义改名。公开构造、`battleId`、`update` 和 Screen 覆写保持；选择顺序、确认条件、倒计时、坐标与颜色数值、模型姿态、物品名称回退及 `TeamPickPayload` 字段顺序未改变。JDK21 离线 `clean build` 通过，259 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-team-preview-symbols.log`；当前 javac 快照为 97 个文件、4277 个声明、0 个分析错误，精确匹配不到该文件的基线安全原名。真实客户端模型渲染、点击选择、倒计时和服务端确认仍待游戏内联调。

## B6-room-screen-symbols（2026-09-17）

`RoomScreen` 的布局常量、房间状态、玩家画像与首发模型缓存、席位和观察者绘制、邀请码复制及开战控制声明完成语义改名。公开构造、`update`、`roomId` 和 Screen 覆写保持；离开与开战动作值、按钮条件、邀请码剪贴板、实体姿态、坐标和颜色数值未改变。JDK21 离线 `clean build` 通过，259 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-room-screen-symbols.log`；当前 javac 快照为 97 个文件、4277 个声明、0 个分析错误，精确匹配不到该文件的基线安全原名。真实房间按钮、实体渲染和服务端响应仍待游戏内联调。

## B6-chat-room-symbols（2026-09-17）

`ChatRoomScreen` 的布局与颜色常量、消息输入、频道标签、消息列表、气泡布局、头像与文本绘制、滚动和提交入口完成语义改名，内部 `Bubble` 类型改为 `MessageBubble`。Screen 公开覆写保持；频道选择、200字符限制、文本裁剪、换行宽度、滚动步长、发送后清空和返回主界面的行为未改变。JDK21 离线 `clean build` 通过，259 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-chat-room-symbols.log`；当前 javac 快照为 97 个文件、4277 个声明、0 个分析错误，精确匹配不到该文件的基线安全原名。真实聊天输入、滚动、频道切换和远端发送仍待客户端联调。

## B6-chat-panel-symbols（2026-09-17）

`ChatPanel` 的面板边界、频道标签缩放、可见消息行、发送者名称命中区域、滚动提示、头像纹理和 Unicode 换行声明完成语义改名，内部 `NameBox` 与 `Row` 分别改为 `SenderHitArea` 与 `VisualMessageRow`。公开静态入口和常量保持；面板坐标、频道命中区、四行窗口、颜色与纹理数值、代码点换行和本地玩家皮肤优先级未改变。JDK21 离线 `clean build` 通过，259 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-chat-panel-symbols.log`；当前 javac 快照为 97 个文件、4277 个声明、0 个分析错误，精确匹配不到该文件的 98 个基线安全原名。真实 HUD 点击、滚动、头像和战斗日志避让仍待客户端验证。
B6-ranked-screen-symbols（2026-09-17）：排位界面固定纹理尺寸居中，列表滚动边界、选中项、详情规则换行、返回命中、排队请求和排行榜请求顺序保持不变。验证：JDK21 离线 clean build 成功，259 项测试通过，javac 符号分析 0 错误；实际 GUI 与远端响应尚未验证。

## B6-main-menu-screen-symbols（2026-09-17）

主菜单保持 ranked、rooms、dex、settings、chat、logout 六个入口及原顺序。logout 继续先发送原有 MenuActionPayload 再关闭界面；头像、收藏宝可梦、未知标记、裁切范围、坐标、颜色和姿态参数保持。JDK21 离线 clean build 成功，259 项测试、0 失败、0 错误，javac 符号分析为97个文件、4277个声明、0错误。真实客户端渲染、鼠标交互及远端动作仍待验证。

## B6-cross-server-service-symbols（2026-09-19）

CrossServerBattleService 的字段、包内方法、私有处理器、参数和局部变量按连接、握手、图鉴、排位、匹配、战斗、房间、认证、聊天和排行榜职责完成语义改名。BattleQueue、TeamPreviews、MatchedBattleAssembly、MirrorLifecycleCleanup 与 SpectatorSessions 的包内调用点同步更新。消息类型、JSON 字段、翻译键、网络发送时机、事件顺序、错误传播和清理分支未改。JDK21 离线 clean build 成功，259 项测试、0 失败、0 错误；javac 快照为 97 个文件、4277 个声明、0 个分析错误；CrossServerBattleService 的公开 javap 签名与原 JAR 无差异。真实跨服连接、镜像实体、Mixin、玩家消息和客户端界面仍待联调。日志 `D:/workspace/gradle-cross-server-symbols.log`。

## B6-remote-dex-symbols（2026-09-19）

RemoteDex 的 72 个基线剩余声明已按缓存文件、图鉴快照、种族值、能力、招式、EV/IV 和队伍元数据职责完成语义改名。公开方法名、record 字段、JSON 字段、缓存路径、日志文本、判断顺序、拒绝顺序、异常处理和磁盘替换副作用未改。JDK21 离线 clean build 成功，259 项测试、0 失败、0 错误、0 跳过；javac 快照为 97 个文件、4277 个声明、0 个分析错误；RemoteDex 的公开 javap 签名与原 JAR 无差异。真实 Pokémon 注册对象、拒绝文本、缓存文件和远端图鉴握手仍待联调。日志 `D:/workspace/gradle-remote-dex-symbols.log`。

## B6-server-dex-symbols（2026-09-19）

ServerDex 的 65 个基线剩余声明已按服务端图鉴激活状态、原图鉴备份、许可条目收集、形态知识填充和反射字段访问职责完成语义改名。公开方法名与 JVM 签名、`DEX_ID`、网络请求、Cobblemon 反射字段名、图鉴替换与恢复顺序、空图鉴提示和 GUI 打开关闭时机未改。JDK21 离线 clean build 成功，259 项测试、0 失败、0 错误、0 跳过；javac 快照为 97 个文件、4277 个声明、0 个分析错误；ServerDex 的公开 javap 签名与原 JAR 无差异。真实 Dexes 全局替换、反射字段、GUI、Mixin 和客户端生命周期仍待联调。日志 `D:/workspace/gradle-server-dex-symbols.log`。

## B6-matchmaking-queue-symbols（2026-09-19）

原 `BattleQueue` 改为 `MatchmakingQueueCoordinator`，等待队伍记录改为 `ClaimedQueueTeam`；等待队伍的登记、领取和丢弃，请求引用领取，排位加入，房间创建、加入、查询、离开和开战，关闭与创建响应，排队确认、离队和位置响应，以及队伍兼容性检查均只调整内部声明和调用点。协议类型、JSON 字段、翻译键、队伍检查顺序、引用登记与领取、发送失败回滚、API 事件和玩家消息顺序保持。

首次快速编译检出 `MatchedBattleAssembly.kt` 仍有两个旧 `claim` 调用，其中一个为方法引用；同步改为 `claimWaitingTeam` 后快速 `classes` 和 JDK21 离线 `clean build` 均成功。全量 259 项测试、0 失败、0 错误、0 跳过；javac 快照为 97 个文件、4277 个声明、0 个分析错误；`CrossServerBattleService` 公开 javap 与原 JAR 无差异，构建产物中旧 `BattleQueue` 类数量为 0，新协调器及内部类共 4 个。真实队列、房间、远端服务、在线玩家消息和镜像建场仍待联调，因此相关源码保持待验证。日志 `D:/workspace/gradle-matchmaking-queue-symbols.log`。

## B6-settings-screen-symbols（2026-09-19）

`SettingsScreen` 的纹理与布局常量、颜色、父界面、选项列表、面板原点、渲染参数、选项行绘制、行位置、命中检测和鼠标点击声明完成语义改名，私有 `Row` 记录改为 `PreferenceToggle`。公开构造和 Screen 覆写签名、标题及说明翻译键、纹理路径、345×207 面板、裁剪区域、行坐标、颜色值、左键分支、布尔值翻转和返回父界面的副作用保持。

快速 `classes` 与 JDK21 离线 `clean build` 均成功，259 项测试、0 失败、0 错误、0 跳过；javac 快照为 97 个文件、4277 个声明、0 个分析错误；64 个基线旧声明精确匹配为 0，公开 javap 与原 JAR 无差异。客户端设置存储已有契约继续覆盖配置读写；真实界面渲染、返回按钮和偏好开关点击仍待客户端验证，因此源码保持待验证。日志 `D:/workspace/gradle-settings-screen-symbols.log`。

## B6-ui-symbols（2026-09-19）

`Ui` 的文本、样式、组件数组、循环索引、画布、字体、坐标、颜色、阴影和模式绑定变量完成语义改名。公开静态方法和全部重载签名、`cobblebattle:ui` 字体资源、默认字体回退、组件间 ` · ` 分隔符、文本包装、绘制返回值、居中绘制和宽度测量分支保持。快速 `classes` 与 JDK21 离线 `clean build` 均成功，259 项测试零失败；javac 快照为 97 个文件、4277 个声明、0 个分析错误；59 个基线旧声明精确匹配为 0，公开 javap 与原 JAR 无差异。真实字体加载及 13 个调用文件的客户端显示仍待验证。日志 `D:/workspace/gradle-ui-symbols.log`。

## B6-chat-hud-symbols（2026-09-19）

`ChatHud` 的悬浮层画布、活动屏幕、指针坐标、悬停消息、身份提示行、面板原点、空态消息、滚动输入与范围、输入提示框、可见草稿和光标位置完成语义改名；包内 `draw` 改为 `drawChatPanel`，`ChatScreen` 调用同步迁移。早期误名 `connectionRequested` 改为 `targetScrollOffset`，一行滚动常量恢复为实际计算输入。

公开 `render`、`drawOver`、`drawNameCard`、`interactiveOver` 和 `scroll` 签名保持。聊天显示条件、原版聊天屏幕与 BattleGUI 覆盖判断、身份提示文本顺序、面板绘制顺序、战斗频道空态、输入框 153×10 裁剪、147 像素可用宽度、按 Unicode code point 从头裁剪、500 毫秒光标节奏、滚轮符号方向和 0..max 钳制保持。快速 `classes` 与 JDK21 离线 `clean build` 均成功，259 项测试零失败；javac 快照为 97 个文件、4277 个声明、0 个分析错误；45 个基线旧声明精确匹配为 0，公开 javap 与原 JAR 无差异。真实 HUD、聊天屏幕、滚轮和玩家身份提示仍待客户端验证。日志 `D:/workspace/gradle-chat-hud-symbols.log`。

## B6-message-catalogue-symbols（2026-09-19）

`Msg` 的日志器、默认语言、内置与外部目录、活动消息表、当前语言、语言解析、目录读取、模板替换和组件组合声明完成语义改名。早期误名 `connectionRequested` 改为 `resolvedLanguageCode`，两个 `outputStream` 分别改为纯文本结果和组合组件含义；默认语言与内置目录常量恢复为实际引用。

公开 `language`、`init`、`raw`、两个 `of` 和两个 `compose` 签名保持。空语言回退 `zh_cn`、请求语言 trim/lowercase、缺失目录回退、基础目录先加载、区域目录覆盖、外部文件最后覆盖、UTF-8、缺键回显键名、顺序替换 `{n}`、非法或越界组件占位符保留为普通文本、组件参数保留样式及最终整体样式应用保持。快速 `classes` 与 JDK21 离线 `clean build` 均成功，259 项测试零失败；javac 快照为 97 个文件、4277 个声明、0 个分析错误；45 个基线旧声明精确匹配为 0，公开 javap 与原 JAR 无差异。真实外部语言文件、读取失败日志和全部消息目录仍待验证。日志 `D:/workspace/gradle-msg-symbols.log`。

## B6-mirror-battle-symbols（2026-09-19）

`MirrorBattle` 的日志器、顺序输出缓存、协议追踪开关、本地 BattleRegistry 标识、已挂接战斗、上游对战标识、主次席位、远端参与者、路由分段、临时 Pokemon 登记和输出解释声明完成语义改名。包内 `attachProp`、`takeProps`、`bindLocalId` 分别改为 `registerPropEntity`、`drainPropEntities`、`bindLocalBattleId`，Java 方法引用与 Kotlin 调用同步迁移。

公开构造、`spectator`、`route`、`attachBody`、`attach`、访问器及输出接收方法签名保持。双本地对战仍向发送端和解释器同时路由；非 `sideupdate` 帧保持双向路由；`sideupdate` 仍按次席位决定发送端与本地解释端。输出继续先等待 `release`，绑定前到达时只记录错误，调试开关仍控制帧日志，最终调用 `ShowdownInterpreter.interpretMessage`。实体登记、快照和领取顺序未改变。快速 `classes` 与 JDK21 离线 `clean build` 均成功，259 项测试零失败；javac 快照为 97 个文件、4277 个声明、0 个分析错误；40 个基线旧声明精确匹配为 0，公开 javap 与原 JAR 无差异。真实实体、BattleRegistry、Showdown 解释和完整对战生命周期仍待验证。日志 `D:/workspace/gradle-mirror-battle-symbols.log`。

## B6-cobblemon-compat-symbols（2026-09-19）

`CobblemonCompat` 的日志器、画像绘制 MethodHandle、无变换枚举、PoseStack、模型旋转、姿态、帧插值、缩放、颜色、头部偏移、光照、图鉴进度候选和反射初始化声明完成语义改名。公开 `drawProfile`、`modernProfile` 和 `OWNED` 签名保持。

静态初始化仍先查找带 `ProfileTransformType` 与方块光照的现代 `drawProfilePokemon`，在索引 7 固定 `NONE`；反射失败后仍查找两个 boolean 且无光照的旧签名，在索引 7 固定 false，并在索引 14补入未使用的 int 参数。两种签名均缺失时继续记录错误且不绘制。调用时 Error 和 RuntimeException 原样传播，其他 Throwable 包装为 IllegalStateException。图鉴进度仍依次尝试 `OWNED`、`CAUGHT`，最后回退枚举末项。快速 `classes` 与 JDK21 离线 `clean build` 均成功，259 项测试零失败；javac 快照为 97 个文件、4277 个声明、0 个分析错误；26 个基线旧声明精确匹配为 0，公开 javap 与原 JAR 无差异。真实客户端画像绘制、跨版本反射和图鉴枚举仍待验证。日志 `D:/workspace/gradle-cobblemon-compat-symbols.log`。

## B6-chat-screen-symbols（2026-09-19）

`ChatScreen` 打开时继续调用 `ChatInput.start()` 建立草稿，`onClose()` 继续先取消草稿，再切回构造时传入的来源界面。该屏幕仍不暂停游戏，背景覆写继续保持空实现。

每帧仍先绘制聊天面板，再调用 Screen 父类渲染，最后绘制玩家身份卡。鼠标点击继续以聊天面板原点换算相对坐标；命中频道时只切换频道并消费事件，未命中时交给父类。滚轮仅在聊天面板范围内调整垂直滚动，面板外继续交给父类，水平滚动参数保持透传。

字符输入继续交给 `ChatInput.type`。Enter 与小键盘 Enter 继续发送当前草稿并关闭界面，Tab 切换频道，Backspace 删除一个 Unicode code point，其余按键交给父类。公开构造和 Screen 覆写的 JVM 签名与原 JAR 一致；259 项自动测试与公开 ABI 对照通过。真实客户端键鼠事件、焦点、频道切换、滚动、提交和返回界面仍待验证，源码状态保持待验证。证据日志：`D:/workspace/gradle-chat-screen-symbols.log`。

## B6-team-preview-coordinator-symbols（2026-09-19）

包内协调器由 `TeamPreviews` 更名为 `TeamPreviewCoordinator`。服务启动和断开时仍清空全部会话，玩家退出时仍只移除该玩家的预览会话。`preview_open`、`preview_state` 和 `preview_closed` 仍由服务端线程交付，公开 `onTeamPicked` 的签名与转发时机保持。

打开事件缺少玩家在线实体时继续记录警告并上报从槽位 0 开始的默认选择。客户端发包失败时继续提示无客户端并使用同一默认选择；发包成功后才保存包含战斗标识、席位、选择数、队伍大小和显示载荷的会话。状态事件继续跳过离线玩家，在线玩家的己方和对方准备状态由席位投影后保存并发送。

关闭事件继续逐个移除会话；在线玩家收到带关闭原因的载荷，所有参与者仍收到关闭提示并从等待队伍中移除。玩家选择继续忽略缺失会话和战斗标识不匹配，数量错误与非法槽位分别记录原日志，合法选择保留传入顺序，保存己方准备状态后发送 `preview_pick`。现有纯规则、解码、会话目录和消息契约继续覆盖这些边界；真实网络发送、在线玩家消息、客户端界面及远端往返仍待验证。证据日志：`D:/workspace/gradle-team-preview-coordinator-symbols.log`。
## B6-battle-server-client-symbols（2026-09-19）

`BattleServerClient` 继续作为 `RemoteBattleConnection` 的兼容门面。构造时仍按配置、入站文档回调、连接成功回调和断开回调的原顺序创建传输对象；启动、停止、连接、断开、暂停、失败观察和重连均只转发原调用。

会话就绪标志、引用递增、socket/会话/需求/拒绝状态读取和普通消息/握手发送结果保持。静态消息对象仍写入相同类型字段；文本、整数、布尔和长整数读取在字段缺失、类型不匹配及默认值场景继续交给 `MessageFields` 原规则处理。自动传输契约已覆盖本地回环连接、握手、发送、主动断开和连接失败回调；真实远端服务与长期重连仍待验证。证据日志：`D:/workspace/gradle-battle-server-client-symbols.log`。

## B6-room-state-payload-symbols（2026-09-19）
RoomStatePayload 的编码顺序仍为房间标识、邀请码、名称、锁定、战斗类型、等级、选人数量、治疗、引擎、合法性、战斗状态、房主、客位、客人、观战者列表和自身角色。解码仍将观战者数量限制为 256，并完整消费列表及末尾角色字段；本批次仅改变内部声明名称。JDK21 离线 clean build 通过，259 项测试零失败。

## B6-backdrop-symbols（2026-09-19）`nBackdrop.draw 继续使用 Cobblemon Pokédex 球体贴图，按当前时间计算 16 帧动画，采用 150ms 帧周期，在固定裁剪区域内按原坐标绘制；本批次仅改内部声明名称。JDK21 离线 clean build 通过，259 项测试零失败。

## B6-back-button-symbols（2026-09-19）

`BackButton` 保留 `X/Y/W/H`、`draw`、`contains` 的公开入口和原有几何值。绘制仍先填充按钮，再绘制四条边框，最后居中绘制翻译标签；悬停颜色、普通颜色、边框颜色和标签颜色未改变。命中检测继续使用左闭右开边界，并与绘制使用同一按钮原点和尺寸。JDK21 离线 `clean build` 完成，259 项测试零失败；客户端真实渲染与点击尚未验证。

## B6-api-events-symbols（2026-09-19）
ApiEvents 的公开事件入口、事件字符串、载荷字段、监听器调用顺序和异常隔离保持。

## B6-player-portrait-symbols（2026-09-19）
PlayerPortrait.lookup 继续裁剪显示名后缀、构造占位 UUID、创建隐藏远程实体并异步更新皮肤。

## B6-open-main-menu-symbols（2026-09-20）
OpenMainMenuPayload 的排行榜规则写入/读取顺序、UTF 长度、规则数量截断至 256 及 record 组件保持。验证日志：D:/workspace/gradle-open-main-menu-symbols.log。

## B6-cross-server-battles-symbols（2026-09-20）
CrossServerBattles 继续委托同一 BattleProjectionIndex，保留注入、选择转发、输出转发、构造登记、按本地/远端/玩家查找、清理和数量查询行为。验证日志：D:/workspace/gradle-cross-server-battles-symbols.log。

## B6-remote-battle-actor-symbols（2026-09-20）
RemoteBattleActor 继续保留来源服务器、座位、名称、镜像队伍和 BattleActor 空操作入口；只调整内部字段和参数名称。验证日志：D:/workspace/gradle-remote-battle-actor-symbols.log。

## B6-payload-codec-batch（2026-09-20）
队伍预览与队伍选择载荷继续保持 UTF 长度、槽位数量上限、字段顺序和列表快照行为。验证日志：D:/workspace/gradle-payload-symbols-batch.log。

## B6-network-request-batch（2026-09-20）
网络发送入口继续按原 payload 类型分发；房间动作载荷继续保持字段顺序、默认 singles、队伍上限和邀请码入口。验证日志：D:/workspace/gradle-network-room-action-batch.log。

## B6-account-client-batch（2026-09-20）
认证会话入口继续使用同一 AccountSessions；UnknownMark 继续使用原纹理和宽高比例绘制。验证日志：D:/workspace/gradle-account-unknown-batch.log。

## B6-runtime-entry-batch（2026-09-20）
模组初始化、服务器事件、镜像实体入口、实体位置缓存和认证提交继续保持原调用顺序与副作用。验证日志：D:/workspace/gradle-runtime-entry-batch.log。
