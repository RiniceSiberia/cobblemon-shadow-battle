# 当前恢复状态

B6-remote-dex-symbols（2026-09-19）已完成：RemoteDex 的 72 个基线剩余声明已按缓存文件、图鉴快照、种族值、能力、招式、EV/IV 和队伍元数据职责完成语义改名。公开方法名、record 字段、协议字段、缓存路径、日志文本、判断与拒绝顺序保持。

JDK21 离线 clean build 成功，259 项测试、0 失败、0 错误、0 跳过，日志 `D:/workspace/gradle-remote-dex-symbols.log`。javac 快照为 97 个文件、4277 个声明、0 个分析错误；RemoteDex 公开 javap 签名与原 JAR 无差异。整体原声明键消失比例上限为 77.86%，仍不能作为最终 90% 验收。

源码状态保持待验证或进行中：真实 Pokémon 注册对象、图鉴缓存文件、远端图鉴握手、跨服连接、镜像实体、Mixin 和客户端副作用尚未联调。恢复时先核对 Git 差异、当前提交和台账，再从 `ServerDex.java` 的 65 个剩余安全声明继续；随后按依赖关系处理 `BattleQueue.java`、`SettingsScreen.java`、`Ui.java`、`ChatHud.java` 和 `Msg.java`。

完整验证命令：`$env:JAVA_HOME='D:\.jdks\ms-21.0.7'; .\gradlew.bat --offline clean build --console=plain`。整体仍未完成：90%命名覆盖、真实实体/Mixin、远端联调、IDEA验证、远程提交和最终目录清理均未完成。
