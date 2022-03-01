val kspVersion: String by project

plugins {
    kotlin("jvm")
}

group = "com.example"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
}
