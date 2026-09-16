# 当前恢复状态

B4-room-queue-requests（2026-09-16）：房间创建和加入请求固定字段迁入 `RoomQueueRequests`，保持字段顺序、引擎/合法性派生规则和空邀请码省略。JDK21 离线 `clean build` 成功，219 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-room-queue-requests.log`。真实房间服务往返仍待联调。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/\.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步提取 queue_join、room_lookup、room_leave、room_start 和 queue_leave 的协议构造，再验证发送失败时 waiting/owner/lookup 引用回滚。整体仍未完成：90% 命名覆盖、真实世界实体/Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。