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
    }
}

rootProject.name = "kspindle"

include(":kspindle-annotations")
include(":kspindle-compiler")
include(":kspindle-runtime")
include(":sample-api")
include(":sample-impl")
include(":sample")
