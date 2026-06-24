plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":kspindle-annotations"))
    implementation(project(":sample-api"))
    // Required for the KSP-generated code that extends ServiceIndexProvider and uses ServiceRegistry
    implementation(project(":kspindle-runtime"))
    ksp(project(":kspindle-compiler"))
}
