# 当前恢复状态

B4-service-requests（2026-09-16）：菜单、排行榜和聊天请求迁入 `ServiceRequests`，请求台账集中处理登记和发送结果。菜单/排行榜 false 撤销引用，聊天 false 保留引用，异常继续传播并保留状态。JDK21 离线 `clean build` 成功，228 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-service-requests.log`。真实服务响应和并发竞态仍待联调。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/\.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步审查 `CrossServerBattleService` 剩余握手、图鉴、战斗输出和聊天观察者协议构造；优先选择完整业务链路并补字段契约。整体仍未完成：90% 命名覆盖、真实世界实体/Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。