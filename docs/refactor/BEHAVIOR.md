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
