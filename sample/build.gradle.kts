plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.spi.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.spi.sample"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "../spi-core/consumer-rules.pro",
                "proguard-rules.pro"
            )
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

    implementation(project(":spi-core"))
    ksp(project(":spi-compiler"))

    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
}
