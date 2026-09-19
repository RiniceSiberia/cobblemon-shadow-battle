# 当前恢复状态

B6-cross-server-service-symbols（2026-09-19）已完成：CrossServerBattleService 的 225 个基线剩余声明已建立语义对应，222 项在本批次改名，3 个 lambda 参数此前已改为 participant。BattleQueue、TeamPreviews、MatchedBattleAssembly、MirrorLifecycleCleanup 和 SpectatorSessions 的 Java/Kotlin 调用点已同步。

JDK21 离线 clean build 成功，259 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-cross-server-symbols.log`。javac 快照为 97 个文件、4277 个声明、0 个分析错误；CrossServerBattleService 公开 javap 签名与原 JAR 无差异。整体原声明键消失比例上限为 75.80%，仍不能作为最终 90% 验收。

源码状态保持待验证或进行中：真实跨服连接、握手、匹配、房间、认证、聊天、排行榜、镜像实体、Mixin 和客户端副作用尚未联调。恢复时先核对 Git 差异、当前提交和台账，再从 `RemoteDex.java` 的 72 个剩余安全声明继续；随后按依赖关系处理 `ServerDex.java`、`BattleQueue.java`、`SettingsScreen.java`、`Ui.java` 和 `ChatHud.java`。

完整验证命令：`$env:JAVA_HOME='D:\.jdks\ms-21.0.7'; .\gradlew.bat --offline clean build --console=plain`。整体仍未完成：90%命名覆盖、真实实体/Mixin、远端联调、IDEA验证、远程提交和最终目录清理均未完成。