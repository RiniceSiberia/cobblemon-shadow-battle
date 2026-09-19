# 当前恢复状态

B6-mirror-battle-symbols（2026-09-19）代码与自动验证已完成：`MirrorBattle` 的日志器、顺序输出、本地 BattleRegistry 标识、已挂接战斗、主次席位、远端参与者、路由分段、临时实体登记和解释器交付声明完成语义改名。`CrossServerBattles`、`MirrorPropEntities` 和 `MirrorLifecycleCleanup` 的包内调用同步迁移。

快速 `classes` 与 JDK21 离线 `clean build` 均成功。259 项测试、0 失败、0 错误、0 跳过；javac 快照为 97 个文件、4277 个声明、0 个分析错误；40 个基线旧声明精确匹配为 0；`MirrorBattle` 公开 javap 与原 JAR 无差异。日志 `D:/workspace/gradle-mirror-battle-symbols.log`。

命名台账已删除 `MirrorBattle.java` 的 40 个剩余声明，并加入 40 项语义映射。当前 3487 个候选中仍有 389 个原名，3098 个原声明键已消失，覆盖上限 88.84%。源码状态保持待验证：真实实体、BattleRegistry、Showdown 解释和完整对战生命周期尚未验证。

恢复时先核对 Git 差异、CSV 可解析性和当前提交；若本批次尚未提交，运行 `git diff --check` 并提交 `refactor: clarify mirror battle coordination`。随后从 `CobblemonCompat.java` 的 26 个剩余安全声明继续，之后处理 `ChatScreen.java`、`TeamPreviews.java` 和 `BattleServerClient.java`。

完整验证命令：`$env:JAVA_HOME='D:\.jdks\ms-21.0.7'; .\gradlew.bat --offline clean build --console=plain`。整体仍未完成：90% 命名覆盖、真实实体与 Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均待完成。
