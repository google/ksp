val testRepo: String by project

plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
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
    implementation(project(":l1"))
    implementation(project(":l2"))
    implementation(project(":l3"))
    implementation(project(":l4"))
    implementation(project(":l5"))
    testImplementation("junit:junit:4.12")
    ksp(project(":validator"))
}
