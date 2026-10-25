# 开发与验证

使用 JDK 21、仓库内的 Gradle Wrapper 和 `gradle/libs.versions.toml` 指定的 Android SDK。
Windows 下将以下命令的 `./gradlew` 换成 `./gradlew.bat`。

## CI Gradle 缓存

Test workflow 在推送 `main` 和面向 `main` 的 PR 上运行
`./gradlew detekt testDebugUnitTest assembleDebug`。仅 `main` 的 push 可写入缓存；
PR 和 tag 发布只读取缓存。并发组按 PR 编号或分支 ref 区分，避免不同 PR 和
`main` 相互取消，同时取消同一组的过时运行。

Test 和 Publish 保留 `setup-gradle` 默认的 `enhanced` provider，不设置
`cache-provider`。它将 Gradle User Home 按细粒度条目缓存，并支持去重和清理。
继续使用仓库 Wrapper，不安装独立 Gradle，也不额外启用 `setup-java` 缓存。
此缓存与 Gradle 配置缓存不同；未提供加密密钥，因此不会跨 runner 保存配置缓存。

验证时先记录缓存列表，在没有该 provider 缓存的 `main` 上运行一次，再重跑一次，并创建新的
PR 运行。检查首次结束时的缓存写入日志、后续 Setup Gradle 的恢复日志，以及
PR 结束时的只读日志；PR 不应生成 `refs/pull/*` 缓存。无需删除已有缓存来制造冷运行。
用 `gh run view <run-id> --log` 查看日志，使用
`gh api repos/<owner>/<repo>/actions/caches` 核查 `ref`、`key` 和 `size_in_bytes`。
用 `gh run view <run-id> --json jobs` 获取步骤时间；需要区分重跑时，使用
`gh api repos/<owner>/<repo>/actions/runs/<run-id>/attempts/<attempt>/jobs`。
从 jobs API 的 `started_at` / `completed_at` 记录每次完整 job 耗时，并单独记录
Gradle 步骤耗时；缓存体积记录当前 provider 所有条目的合计，排除其他 provider
遗留的条目。enhanced 会记录入口和依赖、Wrapper 等独立条目，应核对日志中的
`Restored Key` / `Saved Key`，而非只统计入口条目或仅看请求的 key。
热运行指新 runner 恢复 User Home，
并非保留工作区的 UP-TO-DATE 运行。单次冷热样本不能当作稳定性能基准。

新增成本为每次未被取消的 `main` push 执行一个 `ubuntu-latest` 验证 job，
runner 时间约等于该 job 的完整耗时；PR 原有验证数量不变。具体费用取决于仓库
可见性和账户额度，不将 runner 分钟直接视为付费分钟。

## 模块并行与共享资源

`org.gradle.parallel=true` 允许独立模块并行执行；不额外设置 worker 数或单测
`maxParallelForks`。模块的编译、单测、Kover 和 Detekt 报告保留在各自的 `build/`
目录，根项目聚合覆盖率；SARIF 汇总等待本次任务图中的各模块 Detekt 全部完成。
现有根脚本仍通过 `subprojects` 配置插件，这不代表已支持 Gradle Isolated Projects。

常规 Detekt 检查不自动修改源码。需要修复时，单独执行，例如：

```shell
./gradlew :file-system-memory:detekt --auto-correct
```

自动修复不能与编译、测试或另一个 Gradle 调用同时处理同一份源码。
各模块 baseline 位于 `config/detekt/baselines/<module>.xml`；变体 baseline 由
Detekt 添加变体后缀。仓库原先没有共享 baseline 文件，因此无需迁移既有豁免。
更新 baseline 后应检查差异，不要用生成 baseline 来掩盖新问题。

设备、固定端口、共享可写目录仍需单独互斥；模块并行设置不提供跨 Gradle 调用的锁。
常规单测沿用远程模块对 `docker_test`、`mock_test` 的排除配置，不增加测试进程数。

## SARIF 与 Gradle 弃用警告复查

```shell
./gradlew detekt --warning-mode all --stacktrace --configuration-cache --configuration-cache-problems=fail
./gradlew detekt --rerun-tasks --warning-mode all --stacktrace --configuration-cache --configuration-cache-problems=fail
```

首轮应成功存储配置缓存，第二轮应显示 `Configuration cache entry reused`。
`--rerun-tasks` 确保实际执行分析和合并，而不是只检查 UP-TO-DATE 状态。
合并任务通过显式 `tasks.register<ReportMergeTask>` 返回的 Provider 懒配置，
在本次任务图中各模块分析之后执行。检查七个模块的 `build/reports/detekt/detekt.sarif`
及根目录的 `build/reports/detekt/merge.sarif`：均应为 SARIF 2.1.0，合并结果应包含
各模块的结果。不要仅凭缓存命中时没有警告就认定弃用问题已消失；需要重新配置时
可用 `--no-configuration-cache --warning-mode all --stacktrace` 检查。

2026-10-25 使用 Gradle 9.8.1 核对：项目的 `registering(Type::class)` 委托警告已修复；
仍有以下第三方插件内部调用（由警告调用栈定位）：

| 弃用 API | 来源 | 计划移除版本 |
| --- | --- | --- |
| `ReportingExtension.file(String)` | Detekt 1.23.8，`DetektPlugin.apply` | Gradle 10 |
| `Configuration.setVisible(boolean)` | Detekt 1.23.8、AGP 9.4.1、KSP 2.3.12、Kover 0.9.11 | Gradle 11 |

这些警告仍需插件升级解决，不应通过关闭警告隐藏，也不能据此宣称已兼容 Gradle 10。
当前 Publish workflow 不再调用历史日志中的 `sdkmanager` 步骤。
发布任务图可用以下命令检查；这不执行上传、签名或验证远程发布：

```shell
./gradlew publishAllPublicationsToMavenCentralRepository --dry-run --warning-mode all --stacktrace --configuration-cache --configuration-cache-problems=fail
./gradlew publishAllPublicationsToMavenCentralRepository --dry-run --warning-mode all --stacktrace --configuration-cache --configuration-cache-problems=fail
```

2026-10-25 发布预演已通过：首轮存储配置缓存，第二轮复用配置缓存，
两轮的 241 个待执行任务一致且全部 SKIPPED，包括六个发布模块的签名和上传任务。
另加 `--offline` 的预演也成功存储配置缓存。未执行实际签名、上传或远程发布验证。

此前阻塞来自 `org.jetbrains.dokka:analysis-kotlin-descriptors:2.2.0` 的大文件下载
（85,354,458 字节），不是已确认的配置缓存兼容性问题。Maven Central 返回 HTTP 200；
等待 Gradle 完成下载并核对 JAR 的 SHA-1 后，原先
`:file-system:javaDocReleaseGeneration` 的缓存序列化失败消失。
遇到同类问题，先用 `--info` 检查下载 URL 及进度；离线模式要求该任务图的依赖已完整缓存。

## 完整宿主验证

```shell
./gradlew detekt testDebugUnitTest assembleDebug koverXmlReport koverHtmlReport --continue
```

检查各模块的 `build/test-results/`、`build/reports/detekt/`，以及根项目的
`build/reports/detekt/merge.sarif` 和 `build/reports/kover/`。
以上命令不运行 instrumentation 或依赖外部服务的集成测试。

## 串行与并行对比

选择 `file-system-memory` 和 `file-system-local`：两者共享 `file-system` 依赖，
但彼此没有项目依赖。分别使用 `--no-parallel` 和 `--parallel` 运行：

```shell
./gradlew :file-system:clean :file-system-memory:clean :file-system-local:clean --no-configuration-cache --no-build-cache
./gradlew :file-system-memory:testDebugUnitTest :file-system-local:testDebugUnitTest --no-parallel --no-configuration-cache --no-build-cache --no-daemon --profile
./gradlew :file-system-memory:testDebugUnitTest :file-system-local:testDebugUnitTest --no-parallel --no-configuration-cache --no-build-cache --no-daemon --profile
```

再次清理同样的三个模块，将两条测试命令中的 `--no-parallel` 替换为 `--parallel`。
首轮是构建输出冷缓存，第二轮是输出热缓存；依赖下载缓存和操作系统文件缓存保留，
不能称为完全冷机测试。配置缓存与构建缓存关闭，避免混淆 `parallel` 开关的效果：
配置缓存本身也允许任务并行。`--no-daemon` 使用独立的单次 daemon，方便进程树内存采样。

同时记录墙钟耗时、构建进程树 RSS 峰值和任务执行区间，确认独立模块确实重叠。
热缓存任务可能全部 UP-TO-DATE，此时没有可衡量的任务重叠。
`--profile` 报告位于 `build/reports/profile/`；它不代替进程树内存采样或精确重叠追踪。
比较时保持 worker、测试 fork、JDK 与机器负载一致，多次交替测试后再作性能结论。
