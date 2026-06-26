pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven {
                    val kspindleRepositoryUrl = providers.gradleProperty("kspindleRepositoryUrl")
                        .orElse("https://maven.pkg.github.com/oayilix/kspindle")
                        .get()

                    url = uri(kspindleRepositoryUrl)

                    // Local staging verification points this repository at a file path, while
                    // published artifact verification points it at GitHub Packages. Only HTTP(S)
                    // repositories need credentials, so file-based local staging stays
                    // credential-free.
                    if (kspindleRepositoryUrl.startsWith("http://") || kspindleRepositoryUrl.startsWith("https://")) {
                        credentials {
                            username = providers.gradleProperty("gpr.user").orElse(
                                providers.environmentVariable("GITHUB_ACTOR")
                            ).orNull
                            password = providers.gradleProperty("gpr.key").orElse(
                                providers.environmentVariable("GITHUB_PACKAGES_TOKEN")
                            ).orElse(
                                providers.environmentVariable("GITHUB_TOKEN")
                            ).orNull
                        }
                    }
                }
            }
            filter {
                includeGroup("io.github.oayilix")
            }
        }
    }
}

rootProject.name = "kspindle"

include(":kspindle-annotations")
include(":kspindle-compiler")
include(":kspindle-runtime")
include(":sample-api")
include(":sample-impl")
include(":sample")
