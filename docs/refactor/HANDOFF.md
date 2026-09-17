# 当前恢复状态

B6-symbol-coverage-baseline（2026-09-17）：建立当前 Java 零错误语义快照和逐声明剩余清单。3487 个安全候选中 2409 个原名仍存在，原声明键消失比例上限为 30.91%；`docs/refactor/RENAME_REMAINING.csv` 是后续命名批次入口，不能把该上限当作最终覆盖率。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`、`RENAME_AUDIT.md` 和 `RENAME_REMAINING.csv`，再核对 Git 差异、源码与验证版本。使用 `D:/.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步按剩余清单优先处理原名数量最多的完整 UI 业务链路，先从 RoomLobbyScreen 或认证界面开始，逐批建立旧新对应并重跑行为测试。整体仍未完成：90% 命名覆盖、真实实体/Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。
