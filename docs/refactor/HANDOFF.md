# 当前恢复状态

B5-battle-choice-restrictions（2026-09-16）：`BattleActorMixin` 的五种机制限制规则迁入 `BattleChoiceRestrictions`，保留注入签名及命中后的清理、重选、提示和取消顺序。JDK21 离线 `clean build` 成功，259 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-battle-choice-restrictions.log`。Mixin 真实回调仍待游戏内验证。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

B4 的队伍预览和排队响应迁移已收束。下一步继续审查 B5 Mixin 中可测试规则，并核算安全符号重命名覆盖率；真实实体/Mixin、远端联调、IDEA 验证、远程提交和最终目录清理仍未完成。部署配置按用户要求保留。
