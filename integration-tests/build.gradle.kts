import com.google.devtools.ksp.RelativizingInternalPathProvider
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
}

tasks.withType<Test> {
    maxParallelForks = max(1, Runtime.getRuntime().availableProcessors() / 2)
    systemProperty("kotlinVersion", kotlinBaseVersion)
    systemProperty("kspVersion", version)
    systemProperty("agpVersion", agpBaseVersion)
    jvmArgumentProviders.add(RelativizingInternalPathProvider("testRepo", File(rootProject.buildDir, "repos/test")))
    dependsOn(":api:publishAllPublicationsToTestRepository")
    dependsOn(":gradle-plugin:publishAllPublicationsToTestRepository")
    dependsOn(":common-deps:publishAllPublicationsToTestRepository")
    dependsOn(":symbol-processing:publishAllPublicationsToTestRepository")
    dependsOn(":symbol-processing-cmdline:publishAllPublicationsToTestRepository")
    dependsOn(":kotlin-analysis-api:publishAllPublicationsToTestRepository")

    // JDK_9 environment property is required.
    // To add a custom location (if not detected automatically) follow https://docs.gradle.org/current/userguide/toolchains.html#sec:custom_loc
    if (System.getenv("JDK_9") == null) {
        val launcher9 = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(9))
        }
        environment["JDK_9"] = launcher9.map { it.metadata.installationPath }
    }
}
