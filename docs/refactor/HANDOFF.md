# 当前恢复状态

B4-battle-queue-private-names（2026-09-16）：`BattleQueue` 私有队伍准备、槽位不足、请求发送、准备结果和拒绝异常已重命名，公开 ABI、检查顺序、协议字段和回滚行为未改。JDK21 离线测试成功，214 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-battle-queue-private-names.log`。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/\.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步提取 `BattleQueue` 合法性拒绝消息投影，随后审查发送失败回滚和房间创建/加入请求构造。整体仍未完成：90% 命名覆盖、真实世界实体/Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。