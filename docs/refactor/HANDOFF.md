# 当前恢复状态

B4-service-protocol-messages（2026-09-16）：握手、图鉴查询、房间目录查询、对战输出、选择转发和聊天观察者消息迁入 `ServiceProtocolMessages`。字段顺序、协议版本和可选缓存字段保持不变。JDK21 离线 `clean build` 成功，231 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-service-protocol-messages.log`。真实远端握手、图鉴和对战转发仍待联调。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/\.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步审查 `CrossServerBattleService` 的排行榜配置解析、连接生命周期和剩余 JSON 解析分支；继续按完整链路补契约。整体仍未完成：90% 命名覆盖、真实世界实体/Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。