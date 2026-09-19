# 当前恢复状态

B6-message-catalogue-symbols（2026-09-19）代码与自动验证已完成：`Msg` 的默认语言、资源目录、活动消息、语言解析、内置与外部目录读取、模板替换和组件组合声明完成语义改名。早期误名的语言变量和两个输出变量已修正，默认语言与内置目录常量恢复为实际引用。

快速 `classes` 与 JDK21 离线 `clean build` 均成功。259 项测试、0 失败、0 错误、0 跳过；javac 快照为 97 个文件、4277 个声明、0 个分析错误；45 个基线旧声明精确匹配为 0；`Msg` 公开 javap 与原 JAR 无差异。日志 `D:/workspace/gradle-msg-symbols.log`。

命名台账已删除 `Msg.java` 的 45 个剩余声明，并加入 48 项工具映射，其中三项用于修正早期误名。当前 3487 个候选中仍有 429 个原名，3058 个原声明键已消失，覆盖上限 87.70%。源码状态保持待验证：真实外部语言文件、读取失败日志和全部消息目录尚未验证。

恢复时先核对 Git 差异、CSV 可解析性和当前提交；若本批次尚未提交，运行 `git diff --check` 并提交 `refactor: clarify message catalogue names`。随后从 `MirrorBattle.java` 的 40 个剩余安全声明继续，之后处理 `CobblemonCompat.java`、`ChatScreen.java` 和 `TeamPreviews.java`。

完整验证命令：`$env:JAVA_HOME='D:\.jdks\ms-21.0.7'; .\gradlew.bat --offline clean build --console=plain`。整体仍未完成：90% 命名覆盖、真实实体与 Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均待完成。