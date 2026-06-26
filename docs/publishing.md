# Publishing KSPindle SDK

This document describes how maintainers verify and publish KSPindle artifacts.

本文档说明维护者如何验证和发布 KSPindle SDK 产物。

## Published artifacts / 发布产物

KSPindle publishes three Maven artifacts:

KSPindle 发布三个 Maven 工件：

```text
io.github.oayilix:kspindle-annotations
io.github.oayilix:kspindle-runtime
io.github.oayilix:kspindle-compiler
```

The sample modules are not published. They exist only for integration checks and demos.

示例模块不会发布，只用于集成验证和演示。

## Versioning / 版本

The default development version lives in `gradle.properties`:

默认开发版本位于 `gradle.properties`：

```properties
VERSION_NAME=0.5.0-SNAPSHOT
```

For release builds, override it from the command line or CI:

正式发布时，通过命令行或 CI 覆盖版本：

```bash
./gradlew publishToMavenLocal -PVERSION_NAME=0.5.0
```

Tag releases should use the `vX.Y.Z` format, for example:

发布 tag 使用 `vX.Y.Z` 格式，例如：

```bash
git tag v0.5.0
git push origin v0.5.0
```

## Local verification / 本地验证

Before publishing remotely, run:

远端发布前，先执行：

```bash
./gradlew test lintDebug :sample:assembleRelease
./gradlew publishToMavenLocal
./gradlew publishAllPublicationsToLocalStagingRepository
```

`publishToMavenLocal` verifies local Maven consumption.

`publishToMavenLocal` 用于验证本机 Maven 消费。

`publishAllPublicationsToLocalStagingRepository` writes a Maven repository layout under each SDK module's `build/repo` directory, including JARs, source JARs, javadoc JARs, POM files, Gradle module metadata, and checksums.

`publishAllPublicationsToLocalStagingRepository` 会在各 SDK 模块的 `build/repo` 目录下生成 Maven 仓库结构，包括 JAR、源码包、javadoc 包、POM、Gradle metadata 和校验和。

## GitHub Packages publishing / 发布到 GitHub Packages

The Gradle build defines a `GitHubPackages` Maven repository:

Gradle 构建已配置 `GitHubPackages` Maven 仓库：

```text
https://maven.pkg.github.com/oayilix/kspindle
```

### Publish from GitHub Actions / 从 GitHub Actions 发布

The `Publish SDK` workflow publishes when either:

`Publish SDK` workflow 会在以下情况发布：

- a tag matching `v*.*.*` is pushed;
- it is manually triggered with a version input.

The workflow uses the built-in `GITHUB_TOKEN`, so the repository must grant workflow package write permission.

该 workflow 使用内置 `GITHUB_TOKEN`，仓库需要允许 workflow 写入 Packages。

### Publish from a local machine / 从本机发布

Create a GitHub token with package write permission, then run:

创建一个具备 package write 权限的 GitHub token，然后执行：

```bash
./gradlew publishAllPublicationsToGitHubPackagesRepository \
  -PVERSION_NAME=0.5.0 \
  -Pgpr.user=<github-username> \
  -Pgpr.key=<github-token>
```

Alternatively, provide credentials with environment variables:

也可以通过环境变量提供凭证：

```bash
GITHUB_ACTOR=<github-username> \
GITHUB_PACKAGES_TOKEN=<github-token> \
./gradlew publishAllPublicationsToGitHubPackagesRepository -PVERSION_NAME=0.5.0
```

## Consuming from GitHub Packages / 从 GitHub Packages 消费

Consumers must add the GitHub Packages repository and credentials:

使用方需要添加 GitHub Packages 仓库和凭证：

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/oayilix/kspindle")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull
                ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull
                ?: System.getenv("GITHUB_PACKAGES_TOKEN")
        }
    }
}
```

Then depend on the SDK:

然后依赖 SDK：

```kotlin
dependencies {
    implementation("io.github.oayilix:kspindle-runtime:0.5.0")
    ksp("io.github.oayilix:kspindle-compiler:0.5.0")
}
```

## Maven Central future work / Maven Central 后续工作

GitHub Packages is the first remote publishing target. Maven Central still needs:

GitHub Packages 是当前第一个远端发布目标。Maven Central 还需要：

- Sonatype Central Portal namespace ownership for `io.github.oayilix`;
- release repository configuration;
- signing key provisioning in CI;
- close/release workflow validation;
- final release checklist.

## Release checklist / 发布检查清单

Before cutting a release:

发布前确认：

- `VERSION_NAME` or `-PVERSION_NAME` matches the tag;
- `CHANGELOG.md` documents the release;
- `README.md` dependency snippets use the target version;
- `./gradlew test lintDebug :sample:assembleRelease` passes;
- `./gradlew publishAllPublicationsToLocalStagingRepository` passes;
- runtime JAR contains `META-INF/proguard/kspindle-runtime.pro`;
- GitHub Actions CI is green.
