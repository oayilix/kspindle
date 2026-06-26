# Publishing KSPindle SDK

This document describes how maintainers verify and publish KSPindle artifacts.

本文档说明维护者如何验证和发布 KSPindle SDK 产物。

The current remote publishing target is GitHub Packages. Maven Central work is
tracked separately in `TODO.md`.

当前远端发布目标是 GitHub Packages。Maven Central 相关事项单独记录在 `TODO.md`。

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
./gradlew publishAllPublicationsToLocalStagingRepository -PVERSION_NAME=0.5.0
```

Tag releases should use the `vX.Y.Z` format, for example:

发布 tag 使用 `vX.Y.Z` 格式，例如：

```bash
git tag v0.5.0
git push origin v0.5.0
```

## Local verification / 本地验证

Before publishing remotely, verify the SDK modules, sample app, and local Maven layout:

远端发布前，先验证 SDK 模块、sample app 和本地 Maven 目录结构：

```bash
./gradlew test lintDebug :sample:assembleRelease
./gradlew publishToMavenLocal
./gradlew publishAllPublicationsToLocalStagingRepository
```

`publishToMavenLocal` publishes artifacts to Maven Local for inspection or manual consumption.

`publishToMavenLocal` 会把产物发布到本机 Maven 仓库，便于检查或手动消费。

`publishAllPublicationsToLocalStagingRepository` writes a Maven repository layout under the root `build/repo` directory, including JARs, source JARs, javadoc JARs, POM files, Gradle module metadata, and checksums.

`publishAllPublicationsToLocalStagingRepository` 会在根目录 `build/repo` 下生成 Maven 仓库结构，包括 JAR、源码包、javadoc 包、POM、Gradle metadata 和校验和。

The main maintainer workflow publishes to GitHub Packages first, then verifies
the sample against those published artifacts. If you need to debug the generated
Maven layout before publishing, optionally point the sample at the local staging
repository:

主维护流程会先发布到 GitHub Packages，再用已发布产物验证 sample。如果需要在发布前调试生成的
Maven 目录结构，可以选择让 sample 指向本地 staging 仓库：

```bash
./gradlew publishAllPublicationsToLocalStagingRepository \
  -PVERSION_NAME=0.5.0-SNAPSHOT

./gradlew :sample:assembleRelease :sample-impl:kspKotlin \
  -PusePublishedKspindle=true \
  -PkspindleVersion=0.5.0-SNAPSHOT \
  -PkspindleRepositoryUrl="$PWD/build/repo"
```

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

After publishing, the workflow verifies the sample against the GitHub Packages
artifacts with an isolated Gradle cache and refreshed dependencies.

发布完成后，workflow 会使用隔离的 Gradle 缓存和刷新后的依赖，验证 sample 可以消费
GitHub Packages 上的产物。

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

### Verify published artifacts / 验证已发布产物

After publishing, verify the artifacts from GitHub Packages directly:

发布后，可直接验证 GitHub Packages 上的产物：

```bash
GITHUB_ACTOR=<github-username> \
GITHUB_PACKAGES_TOKEN=<github-token-with-read-packages> \
GRADLE_USER_HOME="$(mktemp -d)" \
./gradlew clean \
  :sample:assembleRelease \
  :sample-impl:kspKotlin \
  -PusePublishedKspindle=true \
  -PkspindleVersion=0.5.0-SNAPSHOT \
  --refresh-dependencies \
  --rerun-tasks
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

## GitHub Packages release checklist / GitHub Packages 发布检查清单

Before cutting a GitHub Packages release:

发布到 GitHub Packages 前确认：

- `VERSION_NAME` or `-PVERSION_NAME` matches the tag;
- `CHANGELOG.md` documents the release;
- `README.md` dependency snippets use the target version;
- `./gradlew test lintDebug :sample:assembleRelease` passes;
- `./gradlew publishAllPublicationsToLocalStagingRepository` passes;
- runtime JAR contains `META-INF/proguard/kspindle-runtime.pro`;
- GitHub Actions CI is green.
