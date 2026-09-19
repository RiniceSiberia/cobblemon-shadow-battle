# 当前恢复状态

B6-team-preview-coordinator-symbols（2026-09-19）代码与自动验证已完成：包内 `TeamPreviews` 重命名为 `TeamPreviewCoordinator`，日志器、服务引用、会话清理、打开/状态/关闭事件处理、玩家选择处理、选择上报及局部声明完成语义改名；`CrossServerBattleService` 调用点同步迁移。

快速 `classes` 与 JDK21 离线 `clean build` 均成功。78 个测试套件、259 项测试、0 失败、0 错误、0 跳过；javac 快照为 97 个文件、4277 个声明、0 个分析错误；25 个基线旧声明精确匹配为 0；旧 `TeamPreviews` 类未进入产物，新协调器类存在；`CrossServerBattleService` 公开 javap 与原 JAR 无差异。日志 `D:/workspace/gradle-team-preview-coordinator-symbols.log`。

命名台账已删除原 `TeamPreviews.java` 的 25 个剩余声明，并加入 25 项语义映射。当前 3487 个候选中仍有 312 个原名，3175 个原声明键已消失，覆盖上限 91.06%。该比例仍需逐项职责对应审计，不能单独作为最终 90% 验收结论。源码状态保持待验证：真实客户端预览、在线玩家、远端事件、队列清理和消息副作用尚未联调。

恢复时先核对 Git 差异、CSV 可解析性和当前提交；若本批次尚未提交，运行 `git diff --check` 并提交 `refactor: clarify team preview coordination`。随后从 `BattleServerClient.java` 的 23 个剩余安全声明继续，之后处理 `RoomStatePayload.java`、`Backdrop.java`、`BackButton.java` 和 `ApiEvents.java`。

完整验证命令：`$env:JAVA_HOME='D:\.jdks\ms-21.0.7'; .\gradlew.bat --offline clean build --console=plain`。整体仍未完成：90% 命名对应审计、真实实体与 Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均待完成。
