# 恢复入口

当前批次 B1：建立标准 Gradle/Kotlin 构建。B0 已完成快照和初步盘点，所有业务文件仍待逐文件审查。

最近验证：2026-09-14 build.ps1 成功，日志 build/run-20260914-130217-919；基线只有编译证据，无测试。远程基线 main=1a14f5caf1c92bf01fcd18051833a53dc4472586。用户确认 Kotlin/JVM + Gradle Kotlin DSL + NeoForge 1.21.1 方案及安全重命名口径。

正在修改：构建配置、docs/refactor、AGENTS.md。尚未迁移业务类、未推送、未清理。备份位于 PLAN.md 指定的项目外目录。

恢复步骤：先读 PLAN.md、FILES.csv、BEHAVIOR.md，再 git status / git diff / git log；核对验证对应版本与实际文件哈希，纠正过期状态。检查未完成批次的源码与日志，不把中断前的计划当成已完成。

下一步：建立 Wrapper 和可移植依赖解析，运行 clean build；补齐配置及传输的基线行为测试，再按完整链路迁移。发布和清理在所有可完成批次审计后进行；未验证游戏集成必须明确保留风险。
