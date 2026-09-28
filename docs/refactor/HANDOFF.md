# 当前恢复状态

B6-rename-ledger-closeout（2026-09-20）已完成：RENAME_REMAINING.csv 当前为空，所有已纳入命名范围的候选均已完成源码核对或记录为兼容保留。
下一步运行最终完整构建、Git 差异审查，并核对尚未完成的真实客户端、实体/Mixin、远端联调、IDEA 验证、GitHub 提交和最终目录清理。
整体重构仍不能宣称完成，直到上述集成和交付项完成。

B6-integration-closeout（2026-09-20）：JDK21 IDEA 模型生成成功；runServer 冒烟进入服务端 Done，日志 D:/workspace/gradle-idea-model.log 与 D:/workspace/gradle-run-server-smoke.log。已推送 origin/main，远程 SHA 与本地一致。


## B7-official-team-commands（2026-09-25）
官方 PS 队伍命令批次：新增 ShowdownTeamStore、ShowdownFormatCatalog 和 CobblemonShowdownTeamExporter；接入 /pokemonshowdown formats、team upload/list/get/delete/download、ladder、cancel。队伍上传发送 /utm 与 /vtm，校验成功后才写入本地存储，同名保留原队伍 ID 并覆盖 packed 内容；保存采用临时文件替换。JDK21 下 gradlew test 全量通过。队伍从线上安全实例化回写 Cobblemon party 尚未完成，download 命令要求权限 4 并返回明确未启用反馈；官方 battle room 到原 MirrorBattle 的镜像接入仍待后续批次。



## B7-team-data-world-snapshot（2026-09-27）
队伍数据改为绑定当前服务端 world：world/cobblebattle/pokemon/showdown-teams.json。上传记录 Showdown packed team、完整 Cobblemon Pokémon JSON 快照和按 UUID/槽位/物种/形态/道具/能力/招式/有效性格/EV/有效IV/性别/等级/闪光/球种/太晶等字段生成的 SHA-256 指纹；有效 IV 优先调用运行时 getEffectiveBattleIV，兼容 Hyper Training，原始 IV 差异时推导 Hidden Power。修正 packed misc 字段顺序为 happiness,pokeball,hpType,gigantamax,dynamaxLevel,teraType。排位前重新导出并拒绝 party 变化；4 级 download 先加载全部快照，成功后一次性覆盖 party，失败尝试恢复原 party。分级解析按官方 |formats| 后缀处理。对战塔对照：其 TeamSnapshot 用 Pokemon.copyFrom 保存完整 NBT，开战前比较队伍数量、UUID 集合、物种、携带物和能力；本实现进一步比较所有对战字段并保持槽位顺序。JDK21 gradlew test 通过。官方 PS 不提供账号云端命名队伍存储，/utm 仅设置当前会话队伍，因此本地 world 仓库是有意设计。


B4-showdown-temporary-state（2026-09-27）：官方 Showdown 和原跨服队列统一使用 `PartyStore.toBattleTeam(true, true, null)` 创建临时副本；新增 `ShowdownBattleStatePolicy`，对副本清除异常及持续时间、恢复 HP、清零 Cobblemon PP Up 阶段并设置 Showdown 基础 PP 上限。官方 packed team 继续调用 Cobblemon `BattleRegistry.packTeam` 后剥离 Cobblemon 私有字段；上传快照读取原始 party，指纹使用原始 UUID，避免副本 UUID 导致队伍一致性误判。JDK21 `gradlew test --no-daemon` 通过；真实游戏内战斗结束后的副本回收、官方房间镜像及客户端显示仍待集成验证。

B7-official-mirrorbattle（2026-09-28）：新增官方 PS battle room 适配器。PlayerSession 识别 `battle-*` 房间，把 `|player|`、`|poke|`、`|start|` 和后续 battle stream 转交 `OfficialShowdownMirrorBridge`；桥接器创建临时 Cobblemon battle 副本，复用 `MirrorBattle`、`ShowdownInterpreter` 和 `CrossServerBattles`，本地选择通过官方房间命令回传。对手仅按 PS 已公开的 `|poke|` 物种创建占位 roster，不伪造未公开能力和招式；服务停止时关闭房间和会话。JDK21 `gradlew test --no-daemon` 通过。真实服务器匹配、客户端画面及不同格式事件仍需联调，当前实现保留官方 stream 与本地规则差异的风险记录。
