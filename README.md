# Cobblemon Shadow Battle

Cobblemon Shadow Battle 是一个面向 Minecraft 1.21.1、NeoForge 和 Cobblemon 的多人对战模组。它把 Minecraft 服务器接入独立的对战服务，让玩家可以在游戏内完成账号认证、图鉴同步、排位匹配、私人房间、队伍预览、对战操作、观战、排行榜和聊天。

模组本身负责 Minecraft 侧的状态管理、网络转发、界面和 Cobblemon 实体表现。排位规则、跨服匹配、账号服务和 Showdown 对战服务由外部对战服务提供，仓库不包含该服务端程序。

## 核心功能

项目的主要功能由以下几个模块协同完成：

### 对战服务连接与协议

`BattleServerClient` 和 `RemoteBattleConnection` 维护到对战服务的按需连接。连接支持握手、协议版本校验、长度受限的 JSON 帧、握手后的 Deflate 压缩、断线重连、指数退避和主动断开。

协议入口集中在 `Protocol`、`ServiceProtocolMessages`、`ServiceRequests`、`QueueRequests`、`RoomQueueRequests` 和 `TeamPreviewMessages`。客户端到服务端的消息覆盖认证、图鉴查询、房间、排位队列、队伍选择、战斗操作、聊天和排行榜；服务端到客户端的消息覆盖菜单、房间状态、队伍预览、对战输出、观战、战斗结束和错误反馈。

### 账号、排位与房间流程

服务端维护玩家会话、账号状态和比赛配置。玩家可以注册、发送验证码、绑定邮箱、登录和退出。登录后可以选择排位项目加入队列，也可以创建或加入私人房间。

`CrossServerBattleService` 负责将玩家事件、队列、房间、账号、图鉴和战斗状态连接起来。`MatchmakingQueueCoordinator`、`RoomDirectoryState`、`QueueReferenceBook` 和相关规则组件负责状态转换、请求引用回收、离线清理和错误反馈。

### 队伍预览与战斗镜像

匹配成功后，服务端发送战斗双方和队伍信息。`TeamPreviewSessions`、`TeamSelection` 以及客户端 `TeamPreviewSelectionState` 处理选择顺序、数量上限、重复选择、倒计时和确认锁定。

`MatchedBattleAssembly` 和 `MirrorBattle` 将远端对战映射到 Cobblemon 的本地战斗对象。远端玩家或 NPC 以镜像实体呈现，战斗输出按顺序转发，玩家选择通过 Showdown 兼容的 `sideupdate` 消息回传。观战者使用只读镜像接收同一场战斗的输出。

镜像生命周期由 `MirrorEntityRegistry`、`MirrorOwnershipIndex`、`MirrorPropTracker`、`MirrorSweepSequence` 和 `MirrorLifecycleCleanup` 管理，负责实体挂接、道具宝可梦、断线清理、战斗结束回收和遗留实体处理。

### 图鉴同步与队伍合法性

对战服务可以下发服务器允许使用的图鉴快照。服务端使用 `RemoteDex` 和 `DexLegalityRules` 校验物种、形态、能力、招式、IV/EV、等级和其他队伍字段。

客户端 `ServerDex` 临时替换 Cobblemon 图鉴内容，展示服务器允许的物种、形态、基础能力值、最近使用的排位项目和服务器图鉴进度。快照带有摘要值，客户端会复用本地摘要，只有内容变化时才请求完整数据。

### Minecraft 客户端界面

客户端包含认证、主菜单、排位榜、房间列表、房间详情、队伍预览、设置、聊天和图鉴入口。服务端通过 Architectury payload 将状态发送给客户端，`ClientHandlerRuntime` 在主线程切换界面并更新本地状态。

默认按键为 `I` 打开主菜单，`Y` 打开聊天。按键可以在 Minecraft 控制设置中调整。

### 服务端命令与配置热加载

根命令为 `/cbattle`，包含以下子命令：

| 命令 | 作用 |
| --- | --- |
| `/cbattle open [ranked]` | 打开认证界面、主菜单或指定排位榜 |
| `/cbattle join [ranked]` | 加入指定排位队列 |
| `/cbattle leave` | 离开当前排位队列 |
| `/cbattle check` | 检查当前队伍是否满足图鉴和队伍规则 |
| `/cbattle status` | 查看对战服务连接、账号和图鉴同步状态 |
| `/cbattle logout` | 退出当前账号 |
| `/cbattle reload` | 重新读取配置；需要权限等级 2 |

## 运行环境

构建配置当前使用：

- Minecraft 1.21.1
- NeoForge 21.1.66
- Cobblemon 1.7 或更高版本
- Architectury NeoForge 13.0.8
- Kotlin/JVM 2.2.20
- Java 21

客户端和服务端都需要安装该模组及其依赖。实际对战还需要可访问的外部对战服务，并且服务端需要为该 Minecraft 服务器分配接入凭证。

## 构建

在安装 JDK 21 的环境中运行：

```powershell
./gradlew.bat clean build
```

发布 JAR 位于 `build/libs/`。默认构建会生成包含 SnakeYAML 的发布包，并将其重定位到项目自己的内部包名，减少与其他模组的依赖冲突。

运行测试：

```powershell
./gradlew.bat test
```

测试覆盖配置读取、网络帧、协议消息、队列和房间状态、图鉴规则、队伍解析、队伍预览、战斗镜像、实体回收、命令决策和客户端界面状态转换等纯逻辑契约。

## 安装与配置

先将构建出的发布 JAR 和所需依赖放入 NeoForge 的 `mods` 目录，启动一次服务器后，模组会生成：

```text
config/cobblebattle.yml
config/cobblebattle-id.txt
```

`cobblebattle.yml` 的主要配置项如下：

| 配置项 | 作用 |
| --- | --- |
| `serverHost` | 对战服务地址 |
| `serverPort` | 对战服务端口 |
| `authToken` | 对战服务分配的接入凭证，部署时必须替换模板值 |
| `connectTimeoutMs` | 连接超时时间 |
| `reconnectBaseDelayMs` / `reconnectMaxDelayMs` | 重连退避范围 |
| `maxFrameBytes` | 单个协议帧的最大字节数 |
| `keepConnectedWhenEmpty` | 没有玩家在线时是否保持连接 |
| `idleDisconnectSeconds` | 空服后的断开等待时间 |
| `language` | 内置语言，支持 `zh_cn` 和 `en_us` |
| `defaultRanked` | `/cbattle join` 未指定项目时使用的排位 ID |
| `debug` | 是否输出详细的协议调试日志 |

服务器身份保存在 `cobblebattle-id.txt`。该文件用于让对战服务识别同一个 Minecraft 服务器，迁移服务器时应按部署策略保留或重新生成。

修改配置后执行：

```text
/cbattle reload
```

部分连接参数需要重新建立对战服务连接，命令会根据变化自动决定是否重连。

## 一次完整的对战流程

玩家进入服务器后，模组按需连接对战服务并发送握手信息。服务器身份、协议版本、模组版本和 Cobblemon 版本会随握手发送。

玩家通过 `/cbattle open` 完成登录，然后可以打开图鉴、排行榜、房间或排位入口。排位流程会先检查服务端图鉴是否就绪，再检查玩家队伍和队伍规则。

匹配成功后，对战服务发送双方信息和队伍数据。模组构造本地镜像实体，打开队伍预览界面，并在玩家确认后启动 Cobblemon 战斗。战斗输出从 Showdown 服务流入 Minecraft，玩家选择和投降操作沿相反方向发送；战斗结束或参与者离线后，镜像实体和引用会按生命周期规则清理。

## 目录结构

```text
src/main/java/.../cobblebattle/       Minecraft、NeoForge、Cobblemon 集成入口
src/main/kotlin/.../shadowbattle/     配置、协议、状态和规则组件
src/main/resources/                   模组元数据、语言、纹理和默认配置
src/test/kotlin/                      协议、状态和业务规则测试
```

Java 层保留 NeoForge、Cobblemon、Mixin 和公开兼容入口；Kotlin 层承载连接、协议、配置、队列、队伍解析、图鉴规则和镜像状态等可独立验证的业务组件。

## 边界与注意事项

仓库只提供 Minecraft 模组，不提供排位规则文件、账号邮件服务、跨服匹配服务或 Showdown 服务端。没有可用的外部对战服务时，模组可以启动和加载界面，但无法完成登录、图鉴同步和在线对战。

请勿将真实接入凭证提交到公开仓库，也不要把 `config/cobblebattle.yml` 复制到日志、问题报告或构建产物中。部署时应使用服务管理员提供的凭证，并限制配置文件的读取权限。

## 许可

项目许可见 [LICENSE](LICENSE)。第三方组件和 Cobblemon 生态依赖的许可信息见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
