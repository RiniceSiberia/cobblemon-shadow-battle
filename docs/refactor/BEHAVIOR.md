# 行为契约与证据

基线版本 baseline-local-20260914；仅全量编译通过，以下行为尚未建立运行证据。

| 链路 | 原实现 | 正常、异常及副作用约束 | 新实现/证据 | 状态 |
| --- | --- | --- | --- | --- |
| 配置 | CobbleBattleConfig / ConfigFile / YamlTree | 首次写模板；空映射默认值；非法端口回退18470；帧上限至少1MiB；读取失败回退；reload 失败抛异常并保留原值；区分连接参数 | 待实现对照测试 | 待重构 |
| 服务器身份 | ServerIdentity | 复用非空文件；空/不可读时生成；保存失败仍返回；并发缓存；mc-前缀 | 待验证 | 待审查 |
| TCP 传输 | BattleServerClient | 压缩流、四字节长度、引用序号、握手、按需连接、拒绝、重连及回调 | 待审查完整分支 | 待审查 |
| 认证 | AuthService / AuthMode | 枚举顺序及越界LOGIN；登录注册绑定邮箱验证码；事件及清理 | 待审查 | 待审查 |
| 游戏包 | network/* | 17个文件；包ID、字段顺序、线程调度、客户端/服务器路由 | 待提取契约 | 待审查 |
| 排队及对战 | BattleQueue / CrossServerBattleService | 加入退出、远端匹配、选择与回合中继、断线、结束清理、事件顺序 | 1575行服务需细分方法审查 | 待审查 |
| 镜像及观战 | MirrorBattle / MirrorFactory / SpectatorFactory | 实体、队伍、座位、回合、退出及错误回收 | 待审查 | 待审查 |
| 图鉴 | RemoteDex / ServerDex | 远端数据、同步、缓存及错误处理 | 待审查 | 待审查 |
| UI及命令 | client/* / command/* / lang/* | 权限、显示、输入、翻译、数据请求、副作用 | 待审查 | 待审查 |
| 公开API | api/* | 类名、record访问器、事件类型、静态字段及JVM签名 | 保留ABI，待验证 | 待审查 |
| Mixin及引导 | mixin/* / neoforge/* / CobbleBattle | 注入目标/描述符、客户端隔离、注册顺序 | 待验证 | 待审查 |

测试只能在实际执行后填写通过，后续改动影响调用链时需重新执行。生产服务器身份的随机生成不能因测试可复现而改成固定 seed；测试输入使用固定值。

B1-gradle-baseline：Gradle build 成功，13个测试全部通过。ConfigurationContractTest 5项，ValueContractTest 3项，TransportContractTest 2项（本地真实Socket双向压缩/异常长度），ChatContractTest 3项。此证据建立于业务源码迁移前；未覆盖完整认证请求、游戏实体和UI。配置模板部署字段按用户决定保持。

B2-configuration：ConfigFile→ConfigurationRepository，YamlTree→ConfigurationDocument，reload→ConfigurationReloading，ServerIdentity内部缓存/IO→PersistentServerIdentity；配置字段和公开入口保留。原配置5项契约继续通过；新增身份3项验证Java trim、缓存、16次并发只生成一次、磁盘写入失败仍返回。全量16 tests / 0 failures，详细计数见 b2-tests.json。

B3-transport：BattleServerClient保留公开Java入口，状态机和帧收发迁至RemoteBattleConnection，标量读取迁至MessageFields。16项既有测试通过；网络2项验证握手/压缩/普通发送门控/Unicode/非对象忽略/手动断线原因/非法长度。尚未覆盖真实远端服务、超时重连压力和所有并发交错。新增AccountContractTest初次2项因测试运行类路径缺Minecraft Component失败，已补测试类路径后重跑，不能归为业务回归。

B3-account：AuthService→AccountSessions+AuthenticationMessages，保留公开入口和Outcome/Signed记录。21项测试通过，含原认证会话2项以及新增三项消息字段/空白边界。代码保持Java isBlank与trim规则、绑定输入不裁剪、验证码请求不携带player、先写pending后发送及失败撤回。真实玩家提交和远端服务联调仍待验证。

B-vendor差分验证：删除前用同一组16个有效YAML和2个非法/不安全标签文档，对比原内嵌解析器与Maven org.yaml:snakeyaml:2.6，全部结果/异常接受性一致（YamlDependencyContractTest，build/gradle-yaml-comparison.log，22项测试全通过）。随后移除123个复制库文件及Java9 Logger源码，使用锁定依赖并隔离打包。最终保留配置契约测试，原差分测试只用于这次替换验证。

B-chat：ChatLog→ChatHistory，ChatState→ConversationState，保留公开入口/Channel/Line。原聊天3项测试通过，保留40条上限、不可变快照、战斗退出恢复频道、注销清空、负滚动归零、reset不改enabled。B-artifact：最终shadowJar在仅含Gson和Kotlin标准库的隔离加载器中解析配置成功，未误打包Minecraft/Cobblemon。

B4-projection：新增迁移前对战契约5项通过，再提取BattleProjectionIndex和SequencedOutputBuffer，新增序号精确交付/先限容量/两分钟边界3项通过。全量30项通过。注入ThreadLocal清理、权威仅中继强制结束、非权威吞掉选择、双本地座位路由保留。未覆盖真实Showdown解释和NPC实体生命周期。
