import com.google.devtools.ksp.RelativizingInternalPathProvider

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

tasks.named<Test>("test") {
    systemProperty("kotlinVersion", kotlinBaseVersion)
    systemProperty("kspVersion", version)
    systemProperty("agpVersion", agpBaseVersion)
    jvmArgumentProviders.add(RelativizingInternalPathProvider("testRepo", File(rootProject.buildDir, "repos/test")))
    dependsOn(":api:publishAllPublicationsToTestRepository")
    dependsOn(":gradle-plugin:publishAllPublicationsToTestRepository")
    dependsOn(":symbol-processing:publishAllPublicationsToTestRepository")
    dependsOn(":symbol-processing-cmdline:publishAllPublicationsToTestRepository")
}
