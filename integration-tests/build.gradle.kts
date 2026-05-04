import com.google.devtools.ksp.RelativizingInternalPathProvider
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import kotlin.math.max

val junitVersion: String by project
val kotlinBaseVersion: String by project
val kotlinxSerializationVersion: String by project
val agpBaseVersion: String by project
val aaCoroutinesVersion: String by project

plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation("junit:junit:$junitVersion")
    testImplementation(gradleTestKit())
    testImplementation(project(":api"))
    testImplementation(project(":gradle-plugin"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$aaCoroutinesVersion")
}

fun Test.configureCommonSettings() {
    val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    maxParallelForks = if (isWindows) 1 else max(1, Runtime.getRuntime().availableProcessors() / 2)
    systemProperty("kotlinVersion", kotlinBaseVersion)
    systemProperty("kspVersion", version)
    systemProperty("agpVersion", agpBaseVersion)
    jvmArgumentProviders.add(
        RelativizingInternalPathProvider(
            "testRepo",
            rootProject.layout.buildDirectory.dir("repos/test").get().asFile
        )
    )
    dependsOn(":api:publishAllPublicationsToTestRepository")
    dependsOn(":gradle-plugin:publishAllPublicationsToTestRepository")
    dependsOn(":common-deps:publishAllPublicationsToTestRepository")
    dependsOn(":symbol-processing:publishAllPublicationsToTestRepository")
    dependsOn(":symbol-processing-aa-embeddable:publishAllPublicationsToTestRepository")
}

// Create a new test task for the primary package
val primaryTest by tasks.registering(Test::class) {
    description = "Runs integration tests in the primary package"
    group = "verification"
    include("com/google/devtools/ksp/test/primary/*.class")
    // Set maxParallelForks to 1 to avoid race conditions when downloading SDKs with old AGPs
    maxParallelForks = 1
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    configureCommonSettings()
}

// Create a new test task for the secondary package
val secondaryTest by tasks.registering(Test::class) {
    description = "Runs integration tests in the secondary package"
    group = "verification"
    include("com/google/devtools/ksp/test/secondary/*.class")
    // Set maxParallelForks to 1 to avoid race conditions when downloading SDKs with old AGPs
    maxParallelForks = 1
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    configureCommonSettings()
}

tasks.test {
    exclude("**/*")
    dependsOn(primaryTest, secondaryTest)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}
