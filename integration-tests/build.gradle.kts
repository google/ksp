val junitVersion: String by project
val kotlinBaseVersion: String by project

plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation("junit:junit:$junitVersion")
    testImplementation(gradleTestKit())
}

tasks.test {
    systemProperty("kotlinVersion", kotlinBaseVersion)
    systemProperty("kspVersion", version)
    systemProperty("testRepo", File(rootProject.buildDir, "repos/test").absolutePath)
    dependsOn(":api:publishAllPublicationsToTestRepository")
    dependsOn(":gradle-plugin:publishAllPublicationsToTestRepository")
    dependsOn(":symbol-processing:publishAllPublicationsToTestRepository")
}