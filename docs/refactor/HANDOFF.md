# 恢复入口

当前批次 B4：排队、匹配与跨服服务状态。B1 已完成 Gradle Wrapper 8.12.1、Kotlin 2.2.20、ModDevGradle 2.0.147，固定 Minecraft 1.21.1 / NeoForge 21.1.66 / Architectury 13.0.8 / Cobblemon 1.7.0。2026-09-15 使用 JDK 21 执行 `--offline test` 成功，48 项测试全部通过；IDEA使用相同Gradle模型，实际界面导入尚未测试。

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

B4-mirror-entities（2026-09-15）：新增 Kotlin `MirrorEntityRegistry` 并接入 `MirrorBattle` 的 NPC/actor 组合和临时 Pokémon 实体引用。新增2项契约验证快照与一次性转移，全量48项测试通过，公开 `javap` 签名仍与原始 JAR 一致，日志位于 `D:/workspace/gradle-b4-mirror-entities.log`。当前代码和台账待提交；下一步处理镜像构造启动和失败回收。
