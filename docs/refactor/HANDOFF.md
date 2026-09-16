# 当前恢复状态

B4-room-state-decoding（2026-09-16）：跨服 `room_state` 消息解码迁入 Kotlin `RoomStateDecoding`，`CrossServerBattleService` 保留玩家查找和网络发送；新增三项契约，JDK21 离线 `clean build` 成功，207 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-room-state-decoding-clean.log`。真实服务端往返、客户端界面展示和异常竞态仍待集成验证。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/\.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步审查 `CrossServerBattleService` 的 `room_list` 解码与缓存交付分支，提取可独立验证的 JSON 规则并保持 `RoomDirectoryState` 的缓存、副作用和发送顺序。整体仍未完成：90% 命名覆盖、真实世界实体/Mixin、远端联调、IDEA 人工验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。