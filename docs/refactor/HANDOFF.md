# 当前恢复状态

B4-mirror-sweep (base eaa604d)：MirrorLifecycleCleanup使用MirrorSweepSequence约束归属释放、服务端查找、延迟和主线程销毁顺序。4项新测试覆盖空快照、服务端缺失、零/负/正宽限时间、prop先于body及异常中止。JDK21离线clean build成功，184项测试（52套）零失败/错误/跳过，9任务全部执行；日志D:/workspace/gradle-mirror-sweep.log。MirrorNpc/MirrorPokemon/SpectatorFactory包内与公开可调用签名一致；javap差异仅静态初始化块迁入Kotlin。真实世界回收仍待验证。

2026-09-16已完成NPC运行实现（f2b00cc）、Pokemon归属与遗留实体（d3e93c0）、观战会话与结束回收顺序四个批次。工作版本为本提交，逐文件验证内容哈希见FILES.csv。历史批次和失败证据保存在PLAN.md、BEHAVIOR.md及Git历史，不以旧的运行中描述作为当前状态。

下一批：继续MirrorFactory匹配建场的普通远端与双本地路径，约束构造失败/缺失Mixin/启动确认副作用；随后推进BattleQueue、CrossServerBattleService和剩余客户端迁移。恢复须先读AGENTS.md及四份台账，再核对Git差异与哈希。

验证：设置JAVA_HOME=D:/.jdks/ms-21.0.7后执行./gradlew.bat --offline clean build --console=plain，日志放项目外，避免clean删除自身日志。当前184项测试零失败；Java仍有原有unchecked提示。公开MirrorNpc/MirrorPokemon接口保持原JAR签名。

全项目未完成：90%可安全符号改名未达标；实际NPC/纹理、Pokemon回收、观战包和远端服务联调未覆盖；本次迁移后尚未重新启动双端，IDEA界面导入未人工验证；尚未推送和最终清理。用户要求暂保留部署配置，公开推送前仍需处理。
