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

rootProject.name = "spi-framework"

include(":spi-annotations")
include(":spi-compiler")
include(":spi-core")
include(":sample-api")
include(":sample-impl")
include(":sample")
