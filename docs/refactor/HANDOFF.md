# 当前恢复状态

B6-chat-room-symbols（2026-09-17）：`ChatRoomScreen` 的安全内部声明完成语义改名，当前快照中该文件基线安全原名为0。JDK21 离线 `clean build` 成功，259 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-chat-room-symbols.log`。当前 javac 快照为97个文件、4277个声明、0个分析错误；整体原声明键消失比例上限为61.14%，仍不能作为最终覆盖率。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`、`RENAME_AUDIT.md` 和 `RENAME_REMAINING.csv`，再核对 Git 差异、源码与验证版本。使用 `D:/.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步按完整 UI 链路处理 `ChatPanel`，随后处理 `RankedScreen`；每批重新生成零错误符号快照。整体仍未完成：90% 命名覆盖、真实实体/Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均未完成。
