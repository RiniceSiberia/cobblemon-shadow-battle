# 当前恢复状态

B4-matched-assembly (base 5c5127c)：MirrorFactory远端和双本地建场迁入Kotlin MatchedBattleAssembly，Java保留包内构造/build签名。LocalMatchClaims保留双次领取及第一次缺失仍领取第二次；构造异常回收仍在endConstruction后执行，缺失Mixin不额外forget，成功通知/API事件先于ack且不新增release。新增3项领取契约与2项构造边界测试；clean build后局部复核build均成功，全量193项通过。日志D:/workspace/gradle-matched-assembly.log及gradle-matched-assembly-final.log。包内可调用ABI与原JAR一致；真实NPC/BattleRegistry/远端匹配与事件副作用尚待集成验证。

本次完成匹配对手解析和完整建场实现迁移。下一批核对并集中管理battle_ack/battle_abort协议帧，再继续BattleQueue和CrossServerBattleService剩余业务。

恢复先读AGENTS.md、PLAN.md、FILES.csv、BEHAVIOR.md，再核对实际Git差异、内容哈希与验证版本。使用JDK21执行./gradlew.bat --offline clean build --console=plain，日志放项目外。

整体仍未完成：90%命名覆盖、实际世界实体/Mixin与远端联调、IDEA人工验证、推送和清理未完成。部署配置按用户要求保留。历史批次见PLAN/BEHAVIOR与Git。

B4-battle-control-messages（2026-09-16）：统一确认/中止帧生成并完成调用方替换。`BattleControlMessages` 和三处调用已完成；测试 XML 统计为 196 项、0 失败、0 错误、55 套。日志 `D:/workspace/gradle-battle-control.log`，当前需提交本批次。下一步审查 `BattleQueue` 与 `CrossServerBattleService` 的断线、房间关闭、队列释放和匹配响应清理，并补协议 payload 边界测试。`CrossServerBattleService`、实体/Mixin、真实网络和远端服务仍待集成验证。

B4-queue-rules（2026-09-16）：QueueRules 已迁入并接入 BattleQueue，测试构建成功。下一步继续排队发送/断线引用清理，或进入 payload 编码审计。

B4-queue-player-payload（2026-09-16）：BattleQueue 的重复玩家 payload 已迁入 QueuePlayerPayload，2项契约通过，日志 D:/workspace/gradle-queue-player-payload.log。下一步继续引用释放和房间/匹配响应清理。
