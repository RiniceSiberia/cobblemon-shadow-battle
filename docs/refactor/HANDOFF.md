# 当前恢复状态

B6-battle-server-client-symbols（2026-09-19）代码与自动验证已完成：`BattleServerClient` 的传输对象、构造回调、状态、连接原因、失败回调和 JSON 字段读取参数完成语义改名，公开方法和协议读取入口保持。

JDK21 离线 `clean build` 成功。78 个测试套件、259 项测试、0 失败、0 错误、0 跳过；javac 快照为 97 个文件、4277 个声明、0 个分析错误；23 个基线旧声明精确匹配为 0；`BattleServerClient` 公开 javap 与原 JAR 无差异。日志 `D:/workspace/gradle-battle-server-client-symbols.log`。

命名台账已删除 `BattleServerClient.java` 的 23 个剩余声明，并加入 23 项语义映射。当前 3487 个候选中仍有 289 个原名，3198 个原声明键已消失，覆盖上限 91.72%。该比例仍需逐项职责对应审计，不能单独作为最终 90% 验收结论。源码状态保持待验证：真实远端连接、帧往返、断线重连和认证握手尚未联调。

恢复时先核对 Git 差异、CSV 可解析性和当前提交；若本批次尚未提交，运行 `git diff --check` 并提交 `refactor: clarify battle server client transport`。随后从 `RoomStatePayload.java` 的 20 个剩余安全声明继续，之后处理 `Backdrop.java`、`BackButton.java`、`ApiEvents.java` 和 `PlayerPortrait.java`。

完整验证命令：`$env:JAVA_HOME='D:\.jdks\ms-21.0.7'; .\gradlew.bat --offline clean build --console=plain`。整体仍未完成：90% 命名对应审计、真实实体与 Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均待完成。

B6-room-state-payload-symbols（2026-09-19）已完成：RoomStatePayload 20 个编解码器内部声明完成语义重命名，协议和公开 record 保持。JDK21 离线 clean build 成功，78 个测试套件、259 项测试零失败；日志 D:/workspace/gradle-room-state-payload.log。当前覆盖上限更新为 3487 个候选、3218 个原声明键已消失、92.29%。下一批处理 Backdrop.java。真实客户端网络路由仍待集成验证。

B6-backdrop-symbols（2026-09-19）已完成：Backdrop 20 个安全内部声明完成语义重命名，公开 draw 签名和渲染常量保持。JDK21 离线 clean build 成功，78 个测试套件、259 项测试零失败；日志 D:/workspace/gradle-backdrop-symbols.log。当前覆盖上限更新为 3487 个候选、3238 个原声明键已消失、92.86%。下一批处理 BackButton.java。真实客户端贴图和渲染效果仍待集成验证。
