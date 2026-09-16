# 当前恢复状态

B4-room-list-decoding（2026-09-16）：跨服 `room_list` 房间数组解码迁入 Kotlin `RoomListDecoding`，缓存 unchanged 分支、哈希处理和客户端交付保持在 `CrossServerBattleService`；新增三项契约，JDK21 离线测试成功，210 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-room-list-decoding.log`。真实服务端列表响应、缓存刷新竞态和客户端展示仍待集成验证。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/\.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步审查 `CrossServerBattleService` 的排行榜响应解析或 `BattleQueue` 的拒绝原因构造，优先提取可独立验证的 JSON/规则逻辑并保持原发送顺序。整体仍未完成：90% 命名覆盖、真实世界实体/Mixin、远端联调、IDEA 人工验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。