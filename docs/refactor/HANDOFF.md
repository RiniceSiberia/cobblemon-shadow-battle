# 当前恢复状态

B4-team-preview-roster（2026-09-16）：队伍预览阵容解码迁入 `TeamPreviewRosterDecoding`，选择消息迁入 `TeamPreviewMessages`，保持过滤、默认值、对手席位顺序和选择序号。JDK21 离线 `clean build` 成功，248 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-team-preview-roster.log`。真实预览往返和客户端界面仍待联调。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/\.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

B4 审计显示剩余直接 JSON 主要集中于 `TeamPreviews` 的打开/状态/关闭包和 `BattleQueue` 的响应字段。下一步迁移队伍预览状态与截止时间解码，再处理排队响应 DTO。整体仍未完成：90% 命名覆盖、真实世界实体/Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。