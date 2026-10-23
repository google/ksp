val testRepo: String by project

plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

repositories {
    maven(testRepo)
    maven("https://redirector.kotlinlang.org/maven/bootstrap/")
    mavenCentral()
}

dependencies {
    ksp(project(":processors"))
}
