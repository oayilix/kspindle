plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

val usePublishedKspindle = providers.gradleProperty("usePublishedKspindle")
    .map(String::toBoolean)
    .getOrElse(false)
val kspindleVersion = providers.gradleProperty("kspindleVersion")
    .orElse(providers.gradleProperty("VERSION_NAME"))
    .get()

dependencies {
    if (usePublishedKspindle) {
        api("io.github.oayilix:kspindle-annotations:$kspindleVersion")
    } else {
        api(project(":kspindle-annotations"))
    }
}
