# 重构计划

B4-packed-details-compatibility：恢复审计发现附加字段迁移压缩空招式槽位且省略非法数值的 setter 调用；优先恢复迁移前930c9db的行为，补充差分决策验证，再继续实体装配。此前构建通过不代表这些边界已保持。

## 目标与约束

使用 Kotlin/JVM、Gradle Kotlin DSL 和 NeoForge 1.21.1 构建 Cobblemon 对战模组，支持 IntelliJ IDEA 导入和命令行构建。按用户确认，重命名比例以项目自有且可安全重命名的符号为分母；公开 API、线协议、配置键、存档字段、Mixin 目标和第三方库不计入可安全改名范围。迁移须保持现有正常、异常、边界及副作用行为。业务语义变更需先澄清。

保留公开 API 的包名、JVM 签名和事件顺序；modId、资源位置、网络包 ID、序列化字段、枚举 ordinal 均为兼容约束。不把机械改名当成逻辑验证。代码与产品界面只描述业务；进度、范围与验收信息集中于本目录。

## 基线

远程 main：1a14f5caf1c92bf01fcd18051833a53dc4472586，仅含 MIT LICENSE。本地初始目录无 Git，226 个 Java 文件，25907 行，包含 123 个 SnakeYAML 文件，无测试和标准 Gradle 构建。基线原 JAR SHA-256：0FB54AE8E4C7E0D748B6DBE93F122A67C9E7C74B2BF2FA399AABC1EA43CCDC79。

2026-09-14 本地 build.ps1 编译通过，仍有两处弃用注解警告与泛型未经检查提示。原包与重建包 class 字节不同；无游戏启动或行为一致性证据。已有源码、IDE 配置、依赖、产物已完整备份至 D:/workspace/refactor-backups/cobblemon-shadow-battle-baseline-20260914-130213。

## 批次

| 批次 | 完整链路 | 验收 |
| --- | --- | --- |
| B0 | 盘点、快照、文件清单、恢复入口 | 记录真实状态和基线构建 |
| B1 | Gradle Wrapper、依赖解析、Kotlin 编译、IDEA 模型 | clean build；不依赖个人绝对路径 |
| B2 | 配置读取、校验、热加载、身份持久化 | 默认、错误回退、边界、文件副作用对照 |
| B3 | 网络传输、认证、包编码 | 帧边界、断线重连、异常、字段/ordinal 对照 |
| B4 | 排队、匹配、镜像对战、观战、图鉴同步 | 状态转换、事件、失败清理对照 |
| B5 | 客户端 UI、命令、Mixin、公开 API | 资源/签名核验及可执行的集成验证 |
| B6 | 全量审计、重命名统计、提交与清理 | 全范围状态明确；远程提交一致；无冗余工作文件 |

## 排除项与决策

第三方 SnakeYAML 不进行业务重命名；优先以锁定版本的 Maven 依赖代替复制源码，并保留其许可信息及打包隔离。未确认替换兼容前不删除实现。已有 LICENSE 保留；原模组的作者与许可元数据不擅自改为 MIT。

兼容入口及外部覆写方法可以保留 Java；内部业务优先 Kotlin。使用类型安全实现，不用空断言或压制诊断掩盖类型问题。不能仅为达成比例给符号添加无意义前后缀。

## 最终验收

完整适用构建与行为测试通过，公开 ABI 和资源/线协议核对通过。每个范围内文件均有最终状态、证据和对应版本。报告客户端/服务端和外部对战服务未实测的内容，存在未完成项不得称整个重构完成。推送不覆盖远程他人变更，清理前将本地独有内容保存到项目外；最终工作目录只保留受版本控制项目文件及 .git。

用户决定（2026-09-14）：先保留模板内现有部署地址和凭证以验证原逻辑，默认值差异作为已有情况保留；这些部署内容之后需删除，公开提交前另行处理。不得在报告或测试输出中复制凭证。

B5-symbol-batch（2026-09-14）：通过Javac符号绑定工具对内部业务声明及引用应用419项语义改名，跳过公开ABI、API包、Mixin、构造器、record成员和协议字段。构建与测试通过。该批次只覆盖可安全内部词典命名，尚未达到90%覆盖；最终比例须按声明级、去重且排除兼容边界重新计算，不得用重复引用充数。

B5-client-handlers（2026-09-15）：七组客户端包接收、按键和界面事件迁入 Kotlin 协调器，原 Java 类只保留公开兼容入口。房间列表、房间状态和队伍预览的界面切换规则提取为纯决策组件；实际客户端启动、收包与界面副作用继续列为集成验收项。

B5-command-runtime（2026-09-15）：七个子命令的树构建和执行逻辑迁入 Kotlin，公开 Java 类型保留原名称、构造、接口和权限方法。排行选择、重载连接动作及 Brigadier 命令树建立契约；实际服务器命令反馈、权限判定和远端服务副作用继续列为集成验收项。

B4-dex-snapshot（2026-09-15）：远端图鉴的快照解析、摘要、就绪、暂停、失效和磁盘文档生成迁入 Kotlin，公开 `RemoteDex` 继续承担兼容入口与 Pokémon 合法性校验。磁盘往返纳入自动验证；客户端全局图鉴替换和真实 Pokémon 规则检查继续分批处理。

B5-client-dex-state（2026-09-15）：客户端图鉴消息的摘要匹配、六项能力值索引、形态回退和最近排行迁入 Kotlin；公开 `ServerDex` 保留网络请求、Cobblemon 全局图鉴替换、反射填充和 GUI 生命周期。

B4-dex-legality（2026-09-15）：Showdown 标识规范化、能力和招式集合判断、非负限制及 EV/IV 上限边界迁入 Kotlin；`RemoteDex` 保持原拒绝类型、顺序和消息构造。

B5-client-startup（2026-09-15）：补齐开发及 IDEA 运行环境所需的 KotlinForForge 5.3.0，并按 ModDevGradle 1.21.1 的隔离类路径规则将 SnakeYAML 加入游戏运行类路径。保持发布 JAR 只打入并重定位 SnakeYAML；配置模板值不变，模板差异日志隐藏受保护值。以实际客户端进入稳定可交互状态作为入口、依赖装载、客户端初始化和 Mixin 配置的集成证据；界面操作、收包与游戏内对战仍分别验证。

B5-server-startup（2026-09-15）：用同一 Gradle 模型实际启动无界面 NeoForge 专用服务端，验收服务端分侧装载、公共入口、网络注册、Cobblemon 数据加载、Showdown 预热和世界就绪。上游模组的分侧 Mixin 与资源标签诊断单独记录，不把其无害日志归为本项目故障；真实玩家连接和远端对战服务交互继续保留为后续集成项。

B5-chat-composer（2026-09-15）：将聊天草稿、输入开关、字符过滤、长度限制、Unicode退格和提交清理迁入独立 Kotlin 状态对象；Java `ChatInput` 继续作为原公开入口并保留频道切换与网络发送边界。输入状态与兼容入口分别建立契约，具体屏幕按键和真实消息下发后续在客户端交互批次验证。

B5-client-settings（2026-09-15）：将客户端本地偏好的一次性加载、JSON解析、默认回退和原子内存更新迁入 Kotlin 存储对象；Java `ClientSettings` 保留原公开静态入口和 Minecraft 游戏目录定位。文件缺失、损坏、额外字段、目录创建及保存失败纳入契约，真实设置界面切换后续验证。

B5-auth-form（2026-09-15）：将认证界面的初始模式、模式循环、翻译键、字段可用性、验证码冷却和提交参数规范化迁入 Kotlin 规则组件；Java `AuthScreen` 保留控件、渲染、焦点、结果处理与原公开入口。密码确认失败、绑定邮箱标记和Java字符串裁剪边界纳入测试，实际界面操作与远端认证继续保留为集成项。

B5-team-preview（2026-09-15）：将队伍预览的选择顺序、选择上限、再次点击移除、截止锁定、准备锁定、确认条件和倒计时迁入 Kotlin 状态对象；Java `TeamPreviewScreen` 保留渲染、命中检测、模型缓存与网络发送。选择快照保持插入顺序，真实点击和服务器确认后续联调。

B5-room-lobby（2026-09-15）：将房间创建表单的循环选项、开关、提交参数规范化、邀请码裁剪及座位类型判断迁入 Kotlin；Java `RoomLobbyScreen` 保留表单显示、输入框焦点、房间卡片、滚动和网络发送。空邀请码继续阻止提交并保持表单打开，密码房间加入时继续保留密码原值。

B5-room-controls（2026-09-15）：将房间内的开战资格、重复提交冷却和邀请码复制提示迁入 Kotlin 状态对象；Java `RoomScreen` 保留渲染、点击命中、剪贴板、离开包和战斗开始后的关屏副作用。服务器房主身份、对手就位和未开战三项条件保持共同约束。

B4-team-preview-sessions（2026-09-15）：将服务端队伍预览会话目录、按对战筛选、无客户端默认选择和客户端选择校验迁入 Kotlin；Java `TeamPreviews` 保留远端 JSON 解析、玩家查找、通知与网络发送。选择校验继续先判断数量，再判断负数、越界和重复序号。

B4-packed-team-values（2026-09-15）：将远端压缩队伍的有界整数、UUID和性别协议字段解析迁入 Kotlin；Java `RemoteTeamCodec` 保留物种/形态索引、能力、招式、太晶属性、战斗 Pokémon 构造和错误日志。字符串裁剪继续采用 Java 范围，性别标记保持大小写敏感。

B4-spectator-format（2026-09-15）：观战镜像改用 Kotlin `BattleFormatResolver` 的独立入口，删除 `SpectatorFactory` 内重复规则解析。观战帧缺少内层对战类型时继续回退外层格式，普通镜像原有的 singles 回退及缺少规则描述警告保持不变。

B4-packed-team-details（2026-09-15）：将远端压缩队伍的性质、能力、招式名称、PP、亲密度和太晶字段解析迁入 Kotlin；Java RemoteTeamCodec 保留 Cobblemon 对象装配。协议仍最多读取四个招式，PP 仅接受带斜杠的当前值。

B4-species-index（2026-09-15）：将远端 Showdown 物种/形态标识索引的构建迁入 Kotlin；标准形态和显式形态均登记，重复标识保持首次登记值，Java RemoteTeamCodec 继续管理缓存失效、预热和对象创建。

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

B4-battle-control-messages (2026-09-16)：统一 `battle_ack` 与 `battle_abort` 帧生成入口，保留键顺序、协议文本、JSON 空值和发送时机。`MatchedBattleAssembly`、`SpectatorSessions`、`CrossServerBattleService` 改用 `BattleControlMessages`；新增3项消息契约。当前构建验证覆盖纯消息和调用方编译，真实跨服服务端确认/中止往返仍待联调。

B4-queue-rules (2026-09-16)：将排队/房间队伍人数的 battleType 别名和槽位数量规则迁入 Kotlin QueueRules，保留 singles 默认值及大小写无关匹配；新增1项规则测试，BattleQueue 继续负责消息副作用。

B4-queue-player-payload (2026-09-16)：将 queue/room 请求重复的玩家 JSON 对象构造迁入 Kotlin QueuePlayerPayload，保持 uuid/name 字段顺序和原始名称文本。

B4-queue-message-rules (2026-09-16)：将房间关闭和队列离队原因到翻译键的映射迁入 Kotlin QueueMessageRules，保留未知原因回退和空值行为。

B4-queue-reference-cleanup (2026-09-16)：为 QueueReferenceBook 增加按玩家回收等待队伍、房主引用和房间查询引用的统一入口；断线时先记录是否排队，再清理全部迟到响应引用，保留 queue_leave 发送条件。

B4-room-state-decoding（2026-09-16）：将 CrossServerBattleService 的 room_state 结构解码迁入 Kotlin RoomStateDecoding，保留默认值、对象过滤和无效玩家忽略行为；通过 207 项离线测试。

B4-room-list-decoding（2026-09-16）：将 room_list 房间数组字段解析迁入 Kotlin RoomListDecoding，保留缓存 unchanged 分支和交付顺序；通过 210 项离线测试。

B4-leaderboard-decoding（2026-09-16）：将排行榜响应条目解析迁入 Kotlin LeaderboardDecoding，保留菜单回退和客户端发送顺序；通过 212 项离线测试。

B4-player-identity-payload（2026-09-16）：将 `QueuePlayerPayload` 安全更名为 `PlayerIdentityPayload`，统一排队、房间、菜单、排行榜和聊天请求的玩家对象构造；通过 212 项离线测试。
B4-service-error-rules（2026-09-16）：认证与聊天错误码映射迁入两个独立 Kotlin 规则，保持未知码回退和静音特殊分支；通过 214 项离线测试。
B4-battle-queue-private-names（2026-09-16）：重写 `BattleQueue` 私有准备、槽位、发送和异常符号名称，不改变公开 ABI 与业务顺序；214 项离线测试通过。
B4-queue-rejection-message（2026-09-16）：队伍合法性拒绝消息投影迁入 Kotlin，保持顺序、槽位编号、翻译键和样式；216 项离线测试通过。
B4-room-queue-requests（2026-09-16）：房间创建与加入请求固定字段迁入 Kotlin 协议构造器，保持字段顺序和引擎/合法性规则；219 项离线测试通过。
B4-queue-action-requests（2026-09-16）：queue_join/queue_leave 与房间 lookup/leave/start 请求迁入 Kotlin 构造器，保持主动和断线离队的 ref 差异；222 项离线测试通过。
B4-queue-send-rollback（2026-09-16）：集中查询、房主与等待队伍请求的登记和发送失败回滚，保持异常传播及残留语义；224 项离线测试通过。
B4-service-requests（2026-09-16）：菜单、排行榜和聊天请求迁入 Kotlin 构造器，请求台账集中发送失败回滚并保留聊天失败残留语义；228 项离线测试通过。
B4-service-protocol-messages（2026-09-16）：握手、图鉴、房间目录、对战转发和观察者协议消息迁入 Kotlin，保留字段顺序与可选缓存字段；231 项离线测试通过。
B4-ranked-decoding（2026-09-16）：远端排位赛配置解析迁入 Kotlin，保持过滤、默认值、规则顺序和重复标识覆盖；234 项离线测试通过。
B4-handshake-response（2026-09-16）：握手设置解码和图鉴缓存决策迁入 Kotlin，保持默认值及失效/复用/请求优先级；237 项离线测试通过。
B4-room-list-personalization（2026-09-16）：房间目录自有标记和交付摘要迁入 Kotlin，保持账号零、顺序、对象复用和 :own 规则；239 项离线测试通过。
B4-connection-lifecycle-rules（2026-09-16）：连接启动、按需连接、最低空闲延迟和释放判定迁入 Kotlin，保持调度副作用位置；242 项离线测试通过。
B4-chat-line-decoding（2026-09-16）：远端聊天消息字段解析迁入 Kotlin，保持空文本忽略、默认频道和无效UUID回退；245 项离线测试通过。
B4-team-preview-roster（2026-09-16）：队伍预览阵容解码和选择消息编码迁入 Kotlin，保持槽位过滤、默认值、对手席位和选择顺序；248 项离线测试通过。
B4-team-preview-events（2026-09-16）：队伍预览打开、状态和关闭事件解码迁入 Kotlin，保持必填字段、默认值、UUID 错误分支、准备状态合并和截止时间边界；252 项离线测试通过。
B4-queue-response-decoding（2026-09-16）：排队和房间的六类响应字段迁入 Kotlin DTO，保持引用领取顺序、协议默认值、无效 UUID 与直接数值解析异常；256 项离线测试通过。
B5-battle-choice-restrictions（2026-09-16）：BattleActor Mixin 的五种机制限制映射迁入 Kotlin，保持注入签名、响应扫描和取消副作用顺序；259 项离线测试通过，真实回调仍待游戏内验证。
B6-symbol-coverage-baseline（2026-09-17）：以基线语义候选和当前零错误 javac 快照建立逐声明命名清单；3487 个候选中 2409 个原名仍存在，原声明键消失比例上限 30.91%，不得据此宣称达到 90%。
B6-room-lobby-symbols-1（2026-09-17）：重写 RoomLobbyScreen 的79个纹理、布局、颜色、状态和输入字段名称；259项测试通过，文件剩余原名267降至188，整体原声明键消失比例上限升至33.18%。
B6-room-lobby-symbols-2（2026-09-17）：完成 RoomLobbyScreen 的私有绘制、命中检测、表单流程、DialogMode及几何局部变量语义改名；259项测试通过，该文件基线安全原名清零，整体覆盖上限升至38.57%。

B6-leaderboard-symbols（2026-09-17）：完成 LeaderboardScreen 的纹理、布局、状态、条目选择、画像缓存、实体姿态保存恢复、输入和绘制声明语义改名；259项测试通过，该文件基线安全原名清零，整体原声明键消失比例上限升至43.73%。

B6-auth-screen-symbols（2026-09-17）：完成 AuthScreen 的认证模式、输入控件、验证码冷却、提交状态、内部按钮和绘制流程声明语义改名，并同步 Kotlin 结果回调；259项测试通过，该文件基线安全原名清零，整体原声明键消失比例上限升至48.75%。

B6-team-preview-symbols（2026-09-17）：完成 TeamPreviewScreen 的纹理布局、选择状态、阵容槽位、模型与姿态缓存、训练家面板、确认和提示流程声明语义改名；259项测试通过，该文件基线安全原名清零，整体原声明键消失比例上限升至53.57%。

B6-room-screen-symbols（2026-09-17）：完成 RoomScreen 的房间状态、席位渲染、实体缓存、观察者、邀请码和开战控制声明语义改名；259项测试通过，该文件基线安全原名清零，整体原声明键消失比例上限升至57.61%。

B6-chat-room-symbols（2026-09-17）：完成 ChatRoomScreen 的频道标签、消息布局、气泡绘制、输入提交和滚动声明语义改名；259项测试通过，该文件基线安全原名清零，整体原声明键消失比例上限升至61.14%。

B6-chat-panel-symbols（2026-09-17）：完成 ChatPanel 的面板坐标、频道标签、消息分行、发送者命中区域、头像绘制和滚动提示声明语义改名；259项测试通过，该文件基线安全原名清零，整体原声明键消失比例上限升至63.95%。
B6-ranked-screen-symbols（2026-09-17）：完成 RankedScreen 安全内部符号重命名，保留 Screen 覆写、构造入口、网络载荷和返回界面行为；离线构建与契约测试通过，真实客户端交互留待集成验证。

B6-main-menu-screen-symbols（2026-09-17）：主菜单仅完成内部声明职责命名和私有类型重命名，保留公开构造与 Screen 覆写、六个动作、网络载荷、纹理路径、布局数值和渲染顺序；真实客户端渲染与交互仍列入 B5/B6 集成验收。

B6-cross-server-service-symbols（2026-09-19）：完成 CrossServerBattleService 的连接生命周期、远端消息分发、镜像对战、房间、认证、聊天和排行榜安全内部声明语义改名，并同步 Java 与 Kotlin 调用点。公开方法签名、协议字段、配置、事件与副作用顺序保持；真实跨服服务和游戏内对象继续作为集成验收项。

B6-remote-dex-symbols（2026-09-19）：完成 RemoteDex 的缓存、快照、合法性检查和队伍元数据安全内部声明语义改名。公开方法名、JVM 签名、JSON 字段、缓存路径、拒绝顺序和文件副作用保持；真实 Pokémon 注册对象、拒绝文本和远端图鉴握手继续作为集成验收项。

B6-server-dex-symbols（2026-09-19）：完成 ServerDex 的服务端图鉴状态、原图鉴备份、候选条目、完整知识记录和反射字段访问安全内部声明语义改名。公开方法、网络载荷、Dex 标识、反射目标字符串和 GUI 副作用顺序保持；真实 Dexes 替换、反射字段、GUI 与 Mixin 继续作为集成验收项。
