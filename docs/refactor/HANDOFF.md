# 当前恢复状态

B4-service-error-rules（2026-09-16）：认证和聊天错误映射分别迁入 `AuthenticationErrorRules`、`ChatErrorRules`，未知码回退及 `MUTED` 特殊分支保持原行为。新增两项契约，JDK21 离线 `clean build` 成功，214 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-service-error-rules.log`。真实认证和聊天错误帧仍待联调。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/\.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步继续 `BattleQueue` 队伍准备、合法性拒绝消息和发送失败回滚链路，优先拆出可纯测试的拒绝投影。整体仍未完成：90% 命名覆盖、真实世界实体/Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。