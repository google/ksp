val testRepo: String by project

plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
    kotlin("kapt")
}

version = "1.0-SNAPSHOT"

repositories {
    maven(testRepo)
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":test-processor"))
    ksp(project(":test-processor"))
    kapt("com.google.auto.service:auto-service:1.0")
}

ksp {
    arg("option1", "value1")
    arg("option2", "value2")
}
