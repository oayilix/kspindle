plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":spi-annotations"))
    implementation(project(":sample-api"))
    // Required for the KSP-generated code that extends ServiceIndexProvider and uses ServiceRegistry
    implementation(project(":spi-core"))
    ksp(project(":spi-compiler"))
}
