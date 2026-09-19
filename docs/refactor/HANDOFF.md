# 当前恢复状态

B6-settings-screen-symbols（2026-09-19）代码与自动验证已完成：`SettingsScreen` 的纹理、布局、颜色、父界面、选项行、渲染、命中检测和点击声明完成语义改名，私有 `Row` 记录改为 `PreferenceToggle`。公开构造、Screen 覆写、配置键、纹理路径、坐标、颜色值和配置写入时机保持。

快速 `classes` 与 JDK21 离线 `clean build` 均成功。259 项测试、0 失败、0 错误、0 跳过；javac 快照为 97 个文件、4277 个声明、0 个分析错误；64 个基线旧声明精确匹配为 0；`SettingsScreen` 公开 javap 与原 JAR 无差异。日志 `D:/workspace/gradle-settings-screen-symbols.log`。

命名台账已删除 `SettingsScreen.java` 的 64 个剩余声明，并加入 63 项工具映射和一项私有记录映射。当前 3487 个候选中仍有 578 个原名，2909 个原声明键已消失，覆盖上限 83.42%。源码状态保持待验证：真实客户端渲染、返回按钮和偏好开关点击尚未验证。

恢复时先核对 Git 差异、CSV 可解析性和当前提交；若本批次尚未提交，运行 `git diff --check` 并提交 `refactor: clarify client settings presentation`。随后从 `Ui.java` 的 59 个剩余安全声明继续，之后依次处理 `ChatHud.java`、`Msg.java` 和 `MirrorBattle.java`。

完整验证命令：`$env:JAVA_HOME='D:\.jdks\ms-21.0.7'; .\gradlew.bat --offline clean build --console=plain`。整体仍未完成：90% 命名覆盖、真实实体与 Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均待完成。
