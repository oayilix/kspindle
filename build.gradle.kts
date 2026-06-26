import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.plugins.signing.SigningExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

val sdkProjects = mapOf(
    "kspindle-annotations" to "KSPindle annotations",
    "kspindle-runtime" to "KSPindle runtime",
    "kspindle-compiler" to "KSPindle KSP compiler"
)

subprojects {
    // Only the three public SDK modules are published. Sample/demo modules stay private to this repo.
    // 仅发布三个对外 SDK 模块；示例模块只用于仓库内验证，不进入 Maven 坐标。
    if (name !in sdkProjects) return@subprojects

    // Maven coordinates are centralized in gradle.properties so release automation can override them.
    // Maven 坐标集中放在 gradle.properties，方便 CI/发布脚本统一覆盖版本号。
    group = providers.gradleProperty("GROUP").get()
    version = providers.gradleProperty("VERSION_NAME").get()

    // java-library exposes api/implementation separation for generated POM scopes.
    // maven-publish creates publish/publishToMavenLocal tasks.
    // signing is wired for release builds but optional for local SNAPSHOT verification.
    // java-library 会把 api/implementation 映射成合理的 POM scope；
    // maven-publish 提供发布任务；signing 为正式发布预留，本地快照验证不强制签名。
    pluginManager.apply("java-library")
    pluginManager.apply("maven-publish")
    pluginManager.apply("signing")

    extensions.configure<JavaPluginExtension>("java") {
        // Keep bytecode/toolchain consistent across all published artifacts.
        // 统一所有发布产物的 Java toolchain。
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }

        // Sources/Javadoc artifacts are required by Maven Central and useful for IDE consumers.
        // sources/javadoc 包是 Maven Central 的常规要求，也能改善 IDE 跳转体验。
        withSourcesJar()
        withJavadocJar()
    }

    extensions.configure<PublishingExtension>("publishing") {
        publications {
            // Publish the standard Java component: main JAR, sources JAR, javadoc JAR,
            // Gradle module metadata, and the generated Maven POM.
            // 发布标准 Java 组件：主 JAR、源码包、javadoc 包、Gradle metadata 和 Maven POM。
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifactId = project.name

                pom {
                    // POM metadata required by public Maven repositories and dependency portals.
                    // 公共 Maven 仓库/依赖门户需要这些 POM 元数据。
                    name.set(sdkProjects.getValue(project.name))
                    description.set(
                        "Compile-time, annotation-driven Service Provider Interface framework for Kotlin and Android."
                    )
                    url.set("https://github.com/oayilix/kspindle")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("oayilix")
                            name.set("oayilix")
                        }
                    }
                    scm {
                        connection.set("scm:git:https://github.com/oayilix/kspindle.git")
                        developerConnection.set("scm:git:ssh://git@github.com/oayilix/kspindle.git")
                        url.set("https://github.com/oayilix/kspindle")
                    }
                }
            }
        }

        repositories {
            maven {
                // Local staging repository for inspecting the exact Maven layout before wiring
                // a remote repository such as Maven Central or GitHub Packages.
                // 本地 staging 仓库用于检查最终 Maven 目录结构；之后可替换/扩展到远端仓库。
                name = "localStaging"
                url = uri(layout.buildDirectory.dir("repo").get().asFile)
            }
            maven {
                // First remote publishing target. GitHub Actions can publish with the built-in
                // GITHUB_TOKEN, while local releases can pass -Pgpr.user/-Pgpr.key.
                // 第一个远端发布目标。GitHub Actions 可使用内置 GITHUB_TOKEN；
                // 本地发布时可通过 -Pgpr.user/-Pgpr.key 传入凭证。
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/oayilix/kspindle")
                credentials {
                    username = providers.gradleProperty("gpr.user")
                        .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                        .getOrElse("")
                    password = providers.gradleProperty("gpr.key")
                        .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                        .orElse(providers.environmentVariable("GITHUB_PACKAGES_TOKEN"))
                        .getOrElse("")
                }
            }
        }
    }

    extensions.configure<SigningExtension>("signing") {
        // Signing is configured only when in-memory PGP keys are provided.
        // GitHub Packages does not require signed artifacts; future Maven Central publishing
        // should make missing keys fail explicitly in its release workflow.
        // 仅当提供内存 PGP 密钥时才配置签名。GitHub Packages 不强制签名；
        // 未来接入 Maven Central 时，应在对应 release workflow 中显式校验缺失密钥并失败。
        val signingKey = providers.gradleProperty("signingInMemoryKey").orNull
        val signingPassword = providers.gradleProperty("signingInMemoryKeyPassword").orNull

        isRequired = providers.gradleProperty("release").map(String::toBoolean).getOrElse(false)
        if (!signingKey.isNullOrBlank()) {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(extensions.getByType(PublishingExtension::class.java).publications)
        }
    }
}
