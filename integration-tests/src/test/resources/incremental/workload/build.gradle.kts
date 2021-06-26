val testRepo: String by project

plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

version = "1.0-SNAPSHOT"

repositories {
    maven(testRepo)
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":validator"))
    testImplementation("junit:junit:4.12")
    ksp(project(":validator"))
}

