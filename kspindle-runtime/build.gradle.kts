import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":kspindle-annotations"))

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    from("consumer-rules.pro") {
        into("META-INF/proguard")
        rename { "kspindle-runtime.pro" }
    }
}

tasks.register("checkConsumerProguardRulesInJar") {
    dependsOn(tasks.jar)

    val jarFile = tasks.jar.flatMap { it.archiveFile }
    inputs.file(jarFile)

    doLast {
        val expectedEntry = "META-INF/proguard/kspindle-runtime.pro"
        ZipFile(jarFile.get().asFile).use { zip ->
            check(zip.getEntry(expectedEntry) != null) {
                "Missing $expectedEntry in ${jarFile.get().asFile}"
            }
        }
    }
}

tasks.check {
    dependsOn("checkConsumerProguardRulesInJar")
}
