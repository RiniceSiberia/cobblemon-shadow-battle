# 当前恢复状态

B6-main-menu-screen-symbols（2026-09-17）已完成并提交：MainMenuScreen 的 97 个基线安全原名已完成语义改名，私有 Tile 已改为 MenuActionTile。JDK21 离线 `clean build` 成功，259 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-main-menu-symbols-final.log`。javac 快照为97个文件、4277个声明、0个分析错误；整体原声明键消失比例上限为69.34%，仍不能作为最终90%验收。

当前文件改动包括 MainMenuScreen 与 docs/refactor 台账。MainMenuScreen 的源码状态为待验证：真实客户端渲染、鼠标交互、头像与宝可梦模型展示、以及 rooms/dex/ranked 等远端动作尚未执行。命名映射和符号快照已完成静态核对。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`、`HANDOFF.md`、`RENAME_AUDIT.md` 和 `RENAME_REMAINING.csv`，再核对 Git 差异、源码与验证版本。使用 `D:/.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

当前开始审查 `CrossServerBattleService`：其剩余225项安全内部声明覆盖连接生命周期、服务端消息分发、镜像对战和错误处理。下一步先按这些完整链路建立命名映射，再以零错误符号快照和离线构建验证。整体仍未完成：90%命名覆盖、真实实体/Mixin、远端联调、IDEA验证、远程提交和最终目录清理均未完成。
