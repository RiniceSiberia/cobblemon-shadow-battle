# 当前恢复状态

B4-queue-send-rollback（2026-09-16）：查询、房主与等待队伍请求的登记和发送回滚集中到 `QueueReferenceBook`。发送 false 按类型撤销引用，发送异常继续传播并保留原有登记状态。JDK21 离线 `clean build` 成功，224 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-queue-send-rollback.log`。真实套接字失败和并发响应仍待联调。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/\.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步继续 `CrossServerBattleService` 剩余菜单、排行榜和聊天请求构造，随后审查请求台账的发送失败回滚。整体仍未完成：90% 命名覆盖、真实世界实体/Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。