# 当前恢复状态

B6-chat-hud-symbols（2026-09-19）代码与自动验证已完成：`ChatHud` 的悬浮层、屏幕、指针、身份提示、面板布局、空态、滚动、输入提示、草稿裁剪和光标声明完成语义改名；包内 `draw` 改为 `drawChatPanel` 并同步 `ChatScreen` 调用。早期误名 `connectionRequested` 已修正，一行滚动常量恢复为实际计算输入。

快速 `classes` 与 JDK21 离线 `clean build` 均成功。259 项测试、0 失败、0 错误、0 跳过；javac 快照为 97 个文件、4277 个声明、0 个分析错误；45 个基线旧声明精确匹配为 0；`ChatHud` 公开 javap 与原 JAR 无差异。日志 `D:/workspace/gradle-chat-hud-symbols.log`。

命名台账已删除 `ChatHud.java` 的 45 个剩余声明，并加入 46 项工具映射，其中一项用于修正早期误名。当前 3487 个候选中仍有 474 个原名，3013 个原声明键已消失，覆盖上限 86.41%。源码状态保持待验证：真实客户端 HUD、聊天界面、滚轮和玩家身份提示尚未验证。

恢复时先核对 Git 差异、CSV 可解析性和当前提交；若本批次尚未提交，运行 `git diff --check` 并提交 `refactor: clarify chat overlay presentation`。随后从 `Msg.java` 的 45 个剩余安全声明继续，之后处理 `MirrorBattle.java`、`CobblemonCompat.java` 和 `ChatScreen.java`。

完整验证命令：`$env:JAVA_HOME='D:\.jdks\ms-21.0.7'; .\gradlew.bat --offline clean build --console=plain`。整体仍未完成：90% 命名覆盖、真实实体与 Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均待完成。
