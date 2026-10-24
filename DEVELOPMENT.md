# 开发与验证

使用 JDK 21、仓库内的 Gradle Wrapper 和 `gradle/libs.versions.toml` 指定的 Android SDK。
Windows 下将以下命令的 `./gradlew` 换成 `./gradlew.bat`。

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
