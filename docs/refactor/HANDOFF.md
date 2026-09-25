# 当前恢复状态

B6-rename-ledger-closeout（2026-09-20）已完成：RENAME_REMAINING.csv 当前为空，所有已纳入命名范围的候选均已完成源码核对或记录为兼容保留。
下一步运行最终完整构建、Git 差异审查，并核对尚未完成的真实客户端、实体/Mixin、远端联调、IDEA 验证、GitHub 提交和最终目录清理。
整体重构仍不能宣称完成，直到上述集成和交付项完成。

B6-integration-closeout（2026-09-20）：JDK21 IDEA 模型生成成功；runServer 冒烟进入服务端 Done，日志 D:/workspace/gradle-idea-model.log 与 D:/workspace/gradle-run-server-smoke.log。已推送 origin/main，远程 SHA 与本地一致。


## B7-official-team-commands（2026-09-25）
官方 PS 队伍命令批次：新增 ShowdownTeamStore、ShowdownFormatCatalog 和 CobblemonShowdownTeamExporter；接入 /pokemonshowdown formats、team upload/list/get/delete/download、ladder、cancel。队伍上传发送 /utm 与 /vtm，校验成功后才写入本地存储，同名保留原队伍 ID 并覆盖 packed 内容；保存采用临时文件替换。JDK21 下 gradlew test 全量通过。队伍从线上安全实例化回写 Cobblemon party 尚未完成，download 命令要求权限 4 并返回明确未启用反馈；官方 battle room 到原 MirrorBattle 的镜像接入仍待后续批次。

