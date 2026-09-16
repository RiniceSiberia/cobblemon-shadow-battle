# 当前恢复状态

B4-connection-lifecycle-rules（2026-09-16）：连接启动、按需连接、空闲延迟和释放判定迁入 `ConnectionLifecycleRules`，调度器和网络副作用保持原位置。JDK21 离线 `clean build` 成功，242 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-connection-lifecycle-rules.log`。真实计时、玩家进出和网络断开竞态仍待验证。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/\.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步进行 B4 完整链路审计，清点 `battle` 包剩余 Java 实现、未覆盖分支和受后续改动影响的已验证项，再决定最后的迁移批次。整体仍未完成：90% 命名覆盖、真实世界实体/Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。