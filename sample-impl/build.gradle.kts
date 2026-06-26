plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
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
    implementation(project(":sample-api"))

    if (usePublishedKspindle) {
        implementation("io.github.oayilix:kspindle-annotations:$kspindleVersion")
        // Required for the KSP-generated code that extends ServiceIndexProvider and uses ServiceRegistry
        implementation("io.github.oayilix:kspindle-runtime:$kspindleVersion")
        ksp("io.github.oayilix:kspindle-compiler:$kspindleVersion")
    } else {
        implementation(project(":kspindle-annotations"))
        // Required for the KSP-generated code that extends ServiceIndexProvider and uses ServiceRegistry
        implementation(project(":kspindle-runtime"))
        ksp(project(":kspindle-compiler"))
    }
}
