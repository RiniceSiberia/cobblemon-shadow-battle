# 当前恢复状态

B4-ranked-decoding（2026-09-16）：远端排位赛配置解析迁入 `RankedCompetitionDecoding`，保持条目过滤、字段默认、规则顺序和重复标识覆盖。JDK21 离线 `clean build` 成功，234 项测试、0 失败、0 错误，日志 `D:/workspace/gradle-ranked-decoding.log`。真实远端 ranked 配置仍待联调。

恢复时先读取 `AGENTS.md`、`docs/refactor/PLAN.md`、`FILES.csv`、`BEHAVIOR.md`，再核对 Git 差异、源码与验证版本。使用 `D:/\.jdks/ms-21.0.7` 执行 `./gradlew.bat --offline clean build --console=plain`，日志写到项目外。

下一步审查连接生命周期和 `onHelloAck` 图鉴决策，随后继续剩余 JSON 响应解析。整体仍未完成：90% 命名覆盖、真实世界实体/Mixin、远端联调、IDEA 验证、远程提交和最终目录清理均未完成。部署配置按用户要求保留。