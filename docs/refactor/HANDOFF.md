# 当前恢复状态

B6-ui-symbols（2026-09-19）代码与自动验证已完成：`Ui` 的文本、样式、组件数组、循环索引、画布、字体、坐标、颜色、阴影和模式绑定变量完成语义改名。公开静态方法、全部重载签名、字体资源、分隔符和绘制分支保持。

快速 `classes` 与 JDK21 离线 `clean build` 均成功。259 项测试、0 失败、0 错误、0 跳过；javac 快照为 97 个文件、4277 个声明、0 个分析错误；59 个基线旧声明精确匹配为 0；`Ui` 公开 javap 与原 JAR 无差异。日志 `D:/workspace/gradle-ui-symbols.log`。

命名台账已删除 `Ui.java` 的 59 个剩余声明并加入对应工具映射。当前 3487 个候选中仍有 519 个原名，2968 个原声明键已消失，覆盖上限 85.12%。源码状态保持待验证：真实字体资源加载和 13 个调用文件的客户端显示尚未验证。

恢复时先核对 Git 差异、CSV 可解析性和当前提交；若本批次尚未提交，运行 `git diff --check` 并提交 `refactor: clarify shared text rendering names`。随后从 `ChatHud.java` 的 45 个剩余安全声明继续，之后处理 `Msg.java` 和 `MirrorBattle.java`。

完整验证命令：`$env:JAVA_HOME='D:\.jdks\ms-21.0.7'; .\gradlew.bat --offline clean build --console=plain`。整体仍未完成：90% 命名覆盖、真实实体与 Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均待完成。
