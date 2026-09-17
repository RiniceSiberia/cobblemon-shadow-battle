# 安全符号重命名审计

本审计以基线 javac 语义绑定结果为准。成员候选沿用早期批次规则：排除 public/protected 成员、构造器、枚举常量、record 自动成员、API 包、Mixin 和无方法体声明；另计非 Mixin 的具名内部类型。声明按路径、种类、owner 和原名与当前 Java 零错误快照精确匹配。

2026-09-17 当前快照共有 3487 个候选，其中 1962 个原名仍存在，1525 个原声明键已消失，原声明键消失比例为 43.73%。该比例是待人工对应的上限，删除、迁入 Kotlin 和真正改名尚未逐项区分，不能作为最终 90% 验收结果。当前 Java 快照含 97 个文件、4277 个声明，javac 分析错误为 0。

剩余原名逐项记录在 `RENAME_REMAINING.csv`。优先按原名数量从高到低推进：

- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/battle/CrossServerBattleService.java`：225 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/client/AuthScreen.java`：175 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/client/TeamPreviewScreen.java`：168 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/client/RoomScreen.java`：141 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/client/ChatRoomScreen.java`：123 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/client/ChatPanel.java`：98 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/client/RankedScreen.java`：97 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/client/MainMenuScreen.java`：91 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/dex/RemoteDex.java`：72 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/client/ServerDex.java`：65 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/battle/BattleQueue.java`：65 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/client/SettingsScreen.java`：64 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/client/Ui.java`：59 项
- `src/main/java/xiaocaoawa/minecraft/mod/cobblebattle/client/ChatHud.java`：45 项

已完成命名批次：`RoomLobbyScreen` 的纹理、布局、颜色、状态、输入、绘制、命中检测和表单流程声明完成语义改名；`LeaderboardScreen` 的纹理、布局、状态、榜单选择、画像缓存、实体姿态保存恢复、输入及绘制声明完成语义改名。当前精确匹配不到这两个文件的基线安全原名。

每个命名批次完成后重新生成零错误快照和剩余清单；只在旧声明与新职责建立对应且适用验证通过后计入最终覆盖率。
