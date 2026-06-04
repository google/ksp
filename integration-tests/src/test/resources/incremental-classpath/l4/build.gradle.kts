val testRepo: String by project

plugins {
    kotlin("jvm")
}

version = "1.0-SNAPSHOT"

repositories {
    maven(testRepo)
    maven("https://redirector.kotlinlang.org/maven/bootstrap/")
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}
