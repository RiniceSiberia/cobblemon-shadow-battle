# 恢复入口

当前批次 B2：配置与身份业务 Kotlin 迁移。B1 已完成 Gradle Wrapper 8.12.1、Kotlin 2.2.20、ModDevGradle 2.0.147，固定 Minecraft 1.21.1 / NeoForge 21.1.66 / Architectury 13.0.8 / Cobblemon 1.7.0。Gradle build 成功，13个测试全部通过（Configuration 5，Value 3，Transport 2，Chat 3）。IDEA使用相同Gradle模型，实际界面导入尚未测试。

业务源码基线为本地提交 2b4ea8d；本批次提交包含测试和构建。首次 Modrinth 下载 TLS 中断，通过缓存相同版本原件重试成功，项目不依赖 .deps 绝对路径。此前 tests 缺 Gson 依赖已修复。

恢复时先读 PLAN.md、FILES.csv、BEHAVIOR.md，再核对 git status/diff/log 和验证版本。构建命令：JDK21 下 ./gradlew.bat build。当前网络需要代理可在命令行临时传 -Dhttps.proxyHost=localhost -Dhttps.proxyPort=7897，不写入项目配置。

下一步：将配置解析、文件读写和身份缓存迁移至 Kotlin，保持原配置键/错误规则，重跑配置与完整测试后立刻更新记录；之后处理传输与认证。尚未推送，也未清理。用户要求暂保留部署地址和凭证；公开发布前仍需处理这部分，禁止把当前带凭证的本地历史直接推送。

B2已完成：配置/身份迁移及16项测试通过，见 b2-tests.json。当前下一步B3：传输及认证业务。SnakeYAML源码仍在，计划替换为锁定2.6版本并隔离打包。

B3传输实现已通过16项原测试；继续认证基线与迁移。当前认证2项首次失败属于测试缺少Minecraft类，已补测试classpath。传输文件标记待验证，保留重连/真实服务未验证范围。

B3-account已完成Kotlin实现及21项测试，证据b3-tests.json。接下来清理第三方源码依赖、迁移聊天状态及对战状态模块，补足各自基线测试。当前没有重命名比例已达标的证据，未宣称完成。
