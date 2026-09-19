# 当前恢复状态

B6-chat-screen-symbols（2026-09-19）代码与自动验证已完成：`ChatScreen` 的来源界面、绘制上下文、指针位置、面板相对坐标、频道命中、滚动、字符输入、键盘输入和草稿提交声明完成语义改名。公开构造与 Screen 覆写签名保持。

快速 `classes` 与 JDK21 离线 `clean build` 均成功。259 项测试、0 失败、0 错误、0 跳过；javac 快照为 97 个文件、4277 个声明、0 个分析错误；26 个基线旧声明精确匹配为 0；`ChatScreen` 公开 javap 与原 JAR 无差异。日志 `D:/workspace/gradle-chat-screen-symbols.log`。

命名台账已删除 `ChatScreen.java` 的 26 个剩余声明，并加入 26 项语义映射。当前 3487 个候选中仍有 337 个原名，3150 个原声明键已消失，覆盖上限 90.34%。该比例仍需逐项职责对应审计，不能单独作为最终 90% 验收结论。源码状态保持待验证：真实客户端键鼠输入、频道切换、滚动、提交和返回界面尚未验证。

恢复时先核对 Git 差异、CSV 可解析性和当前提交；若本批次尚未提交，运行 `git diff --check` 并提交 `refactor: clarify chat screen interaction`。随后从 `TeamPreviews.java` 的 25 个剩余安全声明继续，之后处理 `BattleServerClient.java`、`RoomStatePayload.java`、`Backdrop.java` 和 `BackButton.java`。

完整验证命令：`$env:JAVA_HOME='D:\.jdks\ms-21.0.7'; .\gradlew.bat --offline clean build --console=plain`。整体仍未完成：90% 命名对应审计、真实实体与 Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均待完成。
