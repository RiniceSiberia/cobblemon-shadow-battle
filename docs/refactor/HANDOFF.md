# 当前恢复状态

B4-player-identity-payload（2026-09-16）：`QueuePlayerPayload` 更名为 `PlayerIdentityPayload`，并统一用于排队、房间、菜单、排行榜和聊天请求。两项 JSON 契约保持通过，JDK21 离线测试成功，212 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-player-identity-payload.log`。真实跨服请求仍待联调。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/\.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步提取聊天和认证错误码映射，分别建立独立规则，避免再次混用业务键；随后继续 `BattleQueue` 队伍准备与拒绝消息链路。整体仍未完成：90% 命名覆盖、真实世界实体/Mixin、远端联调、IDEA 人工验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。