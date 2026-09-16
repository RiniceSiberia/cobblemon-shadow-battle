# 当前恢复状态

B4-matched-assembly (base 5c5127c)：MirrorFactory远端和双本地建场迁入Kotlin MatchedBattleAssembly，Java保留包内构造/build签名。LocalMatchClaims保留双次领取及第一次缺失仍领取第二次；构造异常回收仍在endConstruction后执行，缺失Mixin不额外forget，成功通知/API事件先于ack且不新增release。新增3项领取契约与2项构造边界测试；clean build后局部复核build均成功，全量193项通过。日志D:/workspace/gradle-matched-assembly.log及gradle-matched-assembly-final.log。包内可调用ABI与原JAR一致；真实NPC/BattleRegistry/远端匹配与事件副作用尚待集成验证。

本次完成匹配对手解析和完整建场实现迁移。下一批核对并集中管理battle_ack/battle_abort协议帧，再继续BattleQueue和CrossServerBattleService剩余业务。

恢复先读AGENTS.md、PLAN.md、FILES.csv、BEHAVIOR.md，再核对实际Git差异、内容哈希与验证版本。使用JDK21执行./gradlew.bat --offline clean build --console=plain，日志放项目外。

整体仍未完成：90%命名覆盖、实际世界实体/Mixin与远端联调、IDEA人工验证、推送和清理未完成。部署配置按用户要求保留。历史批次见PLAN/BEHAVIOR与Git。
