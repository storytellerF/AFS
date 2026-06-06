# AFS

AFS 是一组面向 Android 的文件系统抽象库，用统一的 `FileInstance` API 访问不同来源的文件：

- 本地文件与 Android `content://` 文档树
- 内存文件系统
- FTP / FTPS / SFTP / SMB / WebDAV / HTTP(S) 远程文件
- ZIP 归档内文件
- 文件图标、文件类型判断等 UI/扩展能力

项目同时提供基于 `java.nio.file.spi.FileSystemProvider` 和 `ServiceLoader` 的自动发现能力。只要应用依赖了对应模块，相关文件系统实现就会被注册。

## 模块

| 模块 | 说明 |
| --- | --- |
| `file-system` | 核心 API，包含 `FileInstance`、路径工具、文件属性模型和复制/移动/删除操作。 |
| `file-system-local` | Android 本地文件、`file://`、`content://`、SAF 权限请求和常见存储路径适配。 |
| `file-system-memory` | 基于 NIO/Jimfs 的 `memory://` 文件系统实现，适合测试或临时文件场景。 |
| `file-system-remote` | 远程协议实现，支持 `ftp`、`ftps`、`ftp_es`、`sftp`、`smb`、`webdav`、`http`、`https`。 |
| `file-system-archive` | ZIP 归档读取和嵌套文件访问。 |
| `file-system-ktx` | 常用扩展函数和文件类型图标资源。 |
| `app` | 示例 Android 应用。 |

`file-system-root` 目前未在 `settings.gradle.kts` 中启用。

## 环境要求

- JDK 21
- Android SDK，`compileSdk = 36`，`minSdk = 26`
- Gradle Wrapper：使用仓库内的 `./gradlew`

仓库依赖 `com.storyteller_f.common_ui_list`，解析 GitHub Packages 时需要在 `~/.gradle/gradle.properties` 配置：

```properties
gpr.user=你的 GitHub 用户名
gpr.key=你的 GitHub token
```

## 集成

通过 JitPack 使用时，先添加仓库：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

然后按需引入模块：

```kotlin
dependencies {
    implementation("com.github.storytellerF.AFS:file-system:<version>")
    implementation("com.github.storytellerF.AFS:file-system-local:<version>")
    implementation("com.github.storytellerF.AFS:file-system-remote:<version>")
    implementation("com.github.storytellerF.AFS:file-system-archive:<version>")
    implementation("com.github.storytellerF.AFS:file-system-ktx:<version>")
}
```

如果使用 GitHub Packages 发布产物，默认 group 为：

```text
com.storyteller_f.afs
```

## 基础用法

获取文件实例：

```kotlin
import android.net.Uri
import com.storyteller_f.file_system.getFileInstance

val uri = Uri.parse("file:///sdcard/Download/demo.txt")
val file = getFileInstance(context, uri)
```

读取文件信息和目录列表：

```kotlin
val info = file?.getFileInfo()
val children = file?.list()
```

创建文件或目录：

```kotlin
import com.storyteller_f.file_system.instance.FileCreatePolicy

val child = file?.toChild(
    name = "new.txt",
    policy = FileCreatePolicy.Create(isFile = true)
)
```

跨文件系统进入子路径时，优先使用 `toChildEfficiently`：

```kotlin
import com.storyteller_f.file_system.toChildEfficiently

val child = file?.toChildEfficiently(context, "nested")
```

远程文件 URI 示例：

```text
ftp://user:password@example.com/path/to/file.txt
sftp://user:password@example.com/path/to/file.txt
smb://domain;user:password@example.com/share/path/to/file.txt
webdav://user:password@example.com/path/to/file.txt
https://example.com/file.txt
```

ZIP 内部文件可以通过 `file-system-archive` 自动构建嵌套 URI；当当前 `FileInstance` 是 `.zip` 文件时，`toChildEfficiently(context, name)` 会尝试进入归档内容。

## 构建与测试

构建全部模块：

```shell
./gradlew build
```

运行单元测试：

```shell
./gradlew test
```

运行静态检查：

```shell
./gradlew detekt
```

生成覆盖率报告：

```shell
./gradlew koverHtmlReport
```

## 发布

发布到 Maven Local：

```shell
./gradlew clean -xtest -xlint assemble publishToMavenLocal \
  -Pgroup=com.github.storytellerF.AFS \
  -Pversion=<version>
```

发布到 GitHub Packages：

```shell
./gradlew publishAllPublicationsToGitHubPackagesRepository \
  -Pgpr.user=<github-user> \
  -Pgpr.key=<github-token> \
  -Pgroup=com.storyteller_f.afs \
  -Pversion=<version>
```

也可以使用仓库脚本：

```shell
./jitpack-publish.sh
./github-publish.sh
```

## 开发说明

- 新增文件系统实现时，实现 `FileInstanceFactory` 或 `FileInstanceFactory2`，并在 `src/main/resources/META-INF/services/com.storyteller_f.file_system.FileInstanceFactory` 中注册。
- 如需接入 NIO `FileSystemProvider`，同时在 `src/main/resources/META-INF/services/java.nio.file.spi.FileSystemProvider` 中注册。
- Android 本地存储路径在不同系统版本上行为不同，修改 `file-system-local` 后建议同时运行本地单元测试和 Android instrumentation 测试。
- `file-system-remote` 的部分测试依赖 Testcontainers 和网络服务模拟环境。
