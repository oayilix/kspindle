plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

val usePublishedKspindle = rootProject.extra["kspindleSampleUsePublishedArtifacts"] as Boolean
val kspindleVersion = rootProject.extra["kspindleSampleVersion"] as String

dependencies {
    if (usePublishedKspindle) {
        api("io.github.oayilix:kspindle-annotations:$kspindleVersion")
    } else {
        api(project(":kspindle-annotations"))
    }
}
