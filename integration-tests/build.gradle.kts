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
}

tasks.named<Test>("test") {
    systemProperty("kotlinVersion", kotlinBaseVersion)
    systemProperty("kspVersion", version)
    systemProperty("agpVersion", agpBaseVersion)
    systemProperty("testRepo", File(rootProject.buildDir, "repos/test").absolutePath)
    systemProperty("kspCompilerRunner", project.properties.getOrDefault("ksp.compiler.runner", "inherited") as String)
    dependsOn(":api:publishAllPublicationsToTestRepository")
    dependsOn(":gradle-plugin:publishAllPublicationsToTestRepository")
    dependsOn(":symbol-processing:publishAllPublicationsToTestRepository")
    dependsOn(":symbol-processing-cmdline:publishAllPublicationsToTestRepository")
}
