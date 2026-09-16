# 当前恢复状态

B4-queue-response-decoding（2026-09-16）：排队和房间的六类响应解码迁入 `QueueResponseDecoding`，保持引用领取、协议默认值、异常传播及后续消息/API 副作用。JDK21 离线 `clean build` 成功，256 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-queue-response-decoding.log`。真实跨服响应仍待联调。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

B4 直接 JSON 响应迁移已基本收束。下一步审计剩余直接解析、公开 ABI 和资源协议，再进入 B5 的 Mixin/实体及命名覆盖核算。整体仍未完成：90% 命名覆盖、真实世界实体/Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。
