import com.google.devtools.ksp.RelativizingInternalPathProvider
import kotlin.math.max
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(libs.junit4)
    testImplementation(gradleTestKit())
    testImplementation(project(":api"))
    testImplementation(project(":gradle-plugin"))
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.core.jvm)
}

fun Test.configureCommonSettings() {
    val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    maxParallelForks = if (isWindows) 1 else max(1, Runtime.getRuntime().availableProcessors() / 2)
    systemProperty("kotlinVersion", libs.versions.kotlin.base.get())
    systemProperty("kspVersion", version)
    systemProperty("agpVersion", libs.versions.agp.base.get())
    jvmArgumentProviders.add(
        RelativizingInternalPathProvider(
            "testRepo",
            rootProject.layout.buildDirectory.dir("repos/test").get().asFile,
        )
    )
    dependsOn(":api:publishAllPublicationsToTestRepository")
    dependsOn(":gradle-plugin:publishAllPublicationsToTestRepository")
    dependsOn(":common-deps:publishAllPublicationsToTestRepository")
    dependsOn(":symbol-processing:publishAllPublicationsToTestRepository")
    dependsOn(":symbol-processing-aa-embeddable:publishAllPublicationsToTestRepository")
}

// Create a new test task for the primary package
val primaryTest by
    tasks.registering(Test::class) {
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
val secondaryTest by
    tasks.registering(Test::class) {
        description = "Runs integration tests in the secondary package"
        group = "verification"
        include("com/google/devtools/ksp/test/secondary/*.class")
        // Set maxParallelForks to 1 to avoid race conditions when downloading SDKs with old AGPs
        maxParallelForks = 1
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        configureCommonSettings()
    }

// Create a new test task for the secondary package
val agpTest by
    tasks.registering(Test::class) {
        description = "Runs integration tests in the agp package"
        group = "verification"
        include("com/google/devtools/ksp/test/agp/*.class")
        // Set maxParallelForks to 1 to avoid race conditions when downloading SDKs with old AGPs
        maxParallelForks = 1
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        configureCommonSettings()
    }

tasks.test {
    exclude("**/*")
    dependsOn(primaryTest, secondaryTest, agpTest)
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
