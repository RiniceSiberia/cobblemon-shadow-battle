# 当前恢复状态

B6-matchmaking-queue-symbols（2026-09-19）代码与自动验证已完成：原 `BattleQueue` 顶层协调器改为 `MatchmakingQueueCoordinator`，内部 `QueuedTeam` 改为 `ClaimedQueueTeam`，其余 63 个基线剩余字段、方法、参数和局部变量按排队、房间、引用和响应职责完成语义改名。`CrossServerBattleService`、`TeamPreviews` 和 `MatchedBattleAssembly` 调用点已同步。

首次快速编译发现 `MatchedBattleAssembly.kt` 的两个 Kotlin 调用仍使用旧 `claim`，其中一个是方法引用；改为 `claimWaitingTeam` 后，快速 `classes` 和 JDK21 离线 `clean build` 均成功。259 项测试、0 失败、0 错误、0 跳过；javac 快照为 97 个文件、4277 个声明、0 个分析错误；`CrossServerBattleService` 公开 javap 与原 JAR 无差异；构建产物不含旧 `BattleQueue` 类，新协调器及内部类共 4 个。日志 `D:/workspace/gradle-matchmaking-queue-symbols.log`。

命名台账已删除原 `BattleQueue.java` 的 65 个剩余声明，并加入 63 项工具映射和两项类型映射。当前 3487 个候选中仍有 642 个原名，2845 个原声明键已消失，覆盖上限 81.59%。源码状态保持待验证：真实队列、房间、远端服务、在线玩家消息、镜像建场、实体与 Mixin 尚未完成联调。

恢复时先核对 Git 差异、CSV 可解析性和当前提交；若本批次尚未提交，运行 `git diff --check` 并提交 `refactor: clarify matchmaking queue coordination`。随后从 `SettingsScreen.java` 的 64 个剩余安全声明继续，之后依次处理 `Ui.java`、`ChatHud.java`、`Msg.java` 和 `MirrorBattle.java`。

完整验证命令：`$env:JAVA_HOME='D:\.jdks\ms-21.0.7'; .\gradlew.bat --offline clean build --console=plain`。整体仍未完成：90% 命名覆盖、真实实体与 Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均待完成。
