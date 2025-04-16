val testRepo: String by project

plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
    application
}

version = "1.0-SNAPSHOT"

repositories {
    maven(testRepo)
    mavenCentral()
    maven("https://redirector.kotlinlang.org/maven/bootstrap/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":validator"))
    testImplementation("junit:junit:4.12")
    ksp(project(":validator"))
    kspTest(project(":validator"))
}

application {
    mainClass = "p1.MainKt"
}
