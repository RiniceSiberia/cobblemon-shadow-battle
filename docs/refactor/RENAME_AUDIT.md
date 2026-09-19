# 安全符号重命名审计

本审计以基线 javac 语义绑定结果为准。成员候选沿用早期批次规则：排除 public/protected 成员、构造器、枚举常量、record 自动成员、API 包、Mixin 和无方法体声明；另计非 Mixin 的具名内部类型。声明按路径、种类、owner 和原名与当前 Java 零错误快照精确匹配。

2026-09-19 当前快照共有 3487 个候选，其中 707 个原名仍存在，2780 个原声明键已消失，原声明键消失比例上限为 79.72%。删除、迁入 Kotlin 和真正改名仍需逐项对应，该数值不能单独作为最终 90% 验收结果。当前 Java 快照含 97 个文件、4277 个声明，javac 分析错误为 0。

剩余原名逐项记录在 `RENAME_REMAINING.csv`。当前数量较多的文件为：

- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/battle/BattleQueue.java`：65 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/client/SettingsScreen.java`：64 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/client/Ui.java`：59 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/client/ChatHud.java`：45 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/lang/Msg.java`：45 项

已完成命名批次：`RoomLobbyScreen`、`LeaderboardScreen`、`AuthScreen`、`TeamPreviewScreen`、`RoomScreen`、`ChatRoomScreen`、`ChatPanel`、`RankedScreen`、`MainMenuScreen`、`CrossServerBattleService`、`RemoteDex` 和 `ServerDex` 的安全内部声明已按业务职责完成语义改名。当前精确匹配不到这些文件的基线安全原名。

每个命名批次完成后重新生成零错误快照和剩余清单；只在旧声明与新职责建立对应且适用验证通过后计入最终覆盖率。

B6-ranked-screen-symbols（2026-09-17）：完成 RankedScreen 的安全内部声明语义改名；259 项测试通过，真实客户端渲染和远端请求仍待联调。

B6-main-menu-screen-symbols（2026-09-17）：完成 MainMenuScreen 的 97 个安全内部声明语义改名；259 项测试通过，真实客户端渲染、点击和远端动作仍待联调。

B6-cross-server-service-symbols（2026-09-19）：CrossServerBattleService 的 225 个基线剩余声明已建立对应，其中 222 项在本批次改名，3 个 lambda 参数此前已改为 participant。Java/Kotlin 调用点同步更新；JDK21 离线 clean build 成功，259 项测试零失败，javac 97 个文件、4277 个声明、0 个分析错误，公开 javap 签名与原 JAR 一致。真实跨服和游戏内副作用仍待联调。

B6-remote-dex-symbols（2026-09-19）：RemoteDex 的 72 个基线剩余声明已全部建立语义对应并改名。JDK21 离线 clean build 成功，259 项测试零失败，javac 97 个文件、4277 个声明、0 个分析错误，公开 javap 签名与原 JAR 一致。真实 Pokémon 对象、缓存文件和远端图鉴握手仍待联调。

B6-server-dex-symbols（2026-09-19）：ServerDex 的 65 个基线剩余声明已全部建立语义对应并改名，其中私有内部类型 `Records` 改为 `RecordFields`。JDK21 离线 clean build 成功，259 项测试零失败，javac 97 个文件、4277 个声明、0 个分析错误，公开 javap 签名与原 JAR 一致。真实 Dexes 替换、反射字段、GUI、Mixin 和客户端生命周期仍待联调。
