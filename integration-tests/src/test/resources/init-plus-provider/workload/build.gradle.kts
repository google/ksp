val testRepo: String by project

plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

version = "1.0-SNAPSHOT"

repositories {
    maven(testRepo)
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    ksp(project(":init-processor"))
    ksp(project(":provider-processor"))
}

ksp {
    arg("option1", "value1")
    arg("option2", "value2")
}
