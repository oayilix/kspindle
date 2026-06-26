plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

val usePublishedKspindle = rootProject.extra["kspindleSampleUsePublishedArtifacts"] as Boolean
val kspindleVersion = rootProject.extra["kspindleSampleVersion"] as String

android {
    namespace = "io.github.oayilix.kspindle.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.oayilix.kspindle.sample"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            if (usePublishedKspindle) {
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            } else {
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "../kspindle-runtime/consumer-rules.pro",
                    "proguard-rules.pro"
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // sample-api provides the interfaces — visible at compile time
    implementation(project(":sample-api"))

    // sample-impl provides implementations — available only at runtime, never at compile time
    runtimeOnly(project(":sample-impl"))

    if (usePublishedKspindle) {
        implementation("io.github.oayilix:kspindle-runtime:$kspindleVersion")
        ksp("io.github.oayilix:kspindle-compiler:$kspindleVersion")
    } else {
        implementation(project(":kspindle-runtime"))
        ksp(project(":kspindle-compiler"))
    }

    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
}
