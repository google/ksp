plugins {
    kotlin("jvm")
    application
    id("com.google.devtools.ksp") version "2.3.5"
}

application {
    mainClass.set("MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":annotations"))
    ksp(project(":processor"))
}
