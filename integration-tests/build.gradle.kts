import com.google.devtools.ksp.RelativizingInternalPathProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.math.max

val junitVersion: String by project
val kotlinBaseVersion: String by project
val agpBaseVersion: String by project

plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation("junit:junit:$junitVersion")
    testImplementation(gradleTestKit())
    testImplementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinBaseVersion")
    testImplementation(project(":api"))
    testImplementation(project(":gradle-plugin"))
    testImplementation(project(":symbol-processing"))
    testImplementation(project(":symbol-processing-cmdline"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

tasks.withType<Test>().configureEach {
    maxParallelForks = max(1, Runtime.getRuntime().availableProcessors() / 2)
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
    dependsOn(":symbol-processing-cmdline:publishAllPublicationsToTestRepository")
    dependsOn(":symbol-processing-aa-embeddable:publishAllPublicationsToTestRepository")

    // Java 17 is required to run tests with AGP
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    )
}

tasks.withType<JavaCompile>().configureEach {
    // ":gradle-plugin" dependency requires VERSION_11
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        // ":gradle-plugin" dependency requires JVM_11 here
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}
