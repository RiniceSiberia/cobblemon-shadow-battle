# 当前恢复状态

B4-queue-action-requests（2026-09-16）：匹配加入/离开及房间查询/离开/开战请求迁入 Kotlin 构造器，主动离队保留 `ref`、断线离队省略 `ref`，玩家和房间动作字段顺序保持不变。JDK21 离线 `clean build` 成功，222 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-queue-action-requests.log`。真实远端请求和断线竞态仍待联调。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/\.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步验证发送失败时 waiting/owner/lookup 引用回滚，必要时提取可注入发送结果的事务规则；随后继续 CrossServerBattleService 剩余请求构造。整体仍未完成：90% 命名覆盖、真实世界实体/Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。