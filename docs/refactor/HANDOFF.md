# 当前恢复状态

B4-handshake-response（2026-09-16）：握手设置解码和图鉴缓存决策迁入 `HandshakeResponseDecoding`，保持字段默认值及未就绪失效、摘要相同复用、其余请求快照的优先级。JDK21 离线 `clean build` 成功，237 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-handshake-response.log`。真实远端握手和缓存文件仍待联调。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/\.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步审查连接生命周期、房间目录交付投影和剩余 JSON 响应解析；随后开始 B4 完整链路审计与 B5 未完成项核对。整体仍未完成：90% 命名覆盖、真实世界实体/Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。