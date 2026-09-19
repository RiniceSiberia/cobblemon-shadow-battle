# 当前恢复状态

B6-server-dex-symbols（2026-09-19）已完成：ServerDex 的 65 个基线剩余声明已按服务端图鉴状态、原图鉴备份、候选条目、完整知识记录和反射字段访问职责完成语义改名。公开方法名、网络载荷、Dex 标识、反射目标字符串和 GUI 副作用顺序保持。

JDK21 离线 clean build 成功，259 项测试、0 失败、0 错误、0 跳过，日志 `D:/workspace/gradle-server-dex-symbols.log`。javac 快照为 97 个文件、4277 个声明、0 个分析错误；ServerDex 公开 javap 签名与原 JAR 无差异。整体原声明键消失比例上限为 79.72%，仍不能作为最终 90% 验收。

源码状态保持待验证或进行中：真实 Dexes 替换、反射字段、GUI、Mixin、跨服连接和镜像实体尚未联调。恢复时先核对 Git 差异、当前提交和台账，再从 `BattleQueue.java` 的 65 个剩余安全声明继续；随后按依赖关系处理 `SettingsScreen.java`、`Ui.java`、`ChatHud.java`、`Msg.java` 和 `MirrorBattle.java`。

完整验证命令：`$env:JAVA_HOME='D:\.jdks\ms-21.0.7'; .\gradlew.bat --offline clean build --console=plain`。整体仍未完成：90%命名覆盖、真实实体/Mixin、远端联调、IDEA验证、远程提交和最终目录清理均未完成。
