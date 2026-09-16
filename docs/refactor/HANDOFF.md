# 当前恢复状态

B4-chat-line-decoding（2026-09-16）：远端聊天消息字段解析迁入 `ChatLineDecoding`，保持空文本忽略、global 默认频道、零 UUID 回退和原字段默认值；频道分发副作用仍在服务中。JDK21 离线 `clean build` 成功，245 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-chat-line-decoding.log`。真实聊天广播和战斗频道成员仍待联调。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/\.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步进行 B4 完整链路审计，清点 battle 包剩余 Java 实现、未覆盖分支和受后续改动影响的台账条目；随后进入 B5 未完成项与命名覆盖统计。整体仍未完成：90% 命名覆盖、真实世界实体/Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。