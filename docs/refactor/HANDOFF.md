# 当前恢复状态

B6-chat-panel-symbols（2026-09-17）：`ChatPanel` 的安全内部声明完成语义改名，当前快照中该文件98个基线安全原名为0。JDK21 离线 `clean build` 成功，259项测试、0失败、0错误，日志 `D:/workspace/gradle-chat-panel-symbols.log`。当前 javac 快照为97个文件、4277个声明、0个分析错误；整体原声明键消失比例上限为63.95%，仍不能作为最终覆盖率。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`、`RENAME_AUDIT.md` 和 `RENAME_REMAINING.csv`，再核对 Git 差异、源码与验证版本。使用 `D:/.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步按完整 UI 链路处理 `RankedScreen`，随后处理 `MainMenuScreen`；每批重新生成零错误符号快照。整体仍未完成：90%命名覆盖、真实实体/Mixin、远端联调、IDEA验证、远程提交和最终目录清理均未完成。
