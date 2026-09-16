# 当前恢复状态

B4-leaderboard-decoding（2026-09-16）：跨服排行榜响应解码迁入 Kotlin `LeaderboardDecoding`，`CrossServerBattleService` 保留 ref 领取、菜单回退和网络发送；新增两项契约，JDK21 离线 `clean build` 成功，212 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-leaderboard-decoding-clean.log`。真实服务端排行榜响应、客户端展示和请求竞态仍待集成验证。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/\.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步审查 `BattleQueue` 的队伍准备/拒绝消息或 `CrossServerBattleService` 的聊天与认证响应，优先提取可独立验证的纯规则并保持外部行为。整体仍未完成：90% 命名覆盖、真实世界实体/Mixin、远端联调、IDEA 人工验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。