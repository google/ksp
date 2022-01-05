plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.5.20-1.0.0-beta04")
}

sourceSets.main {
    java.srcDirs("src/main/kotlin")
}

publishing {
    publications {
        create<MavenPublication>("default") {
            artifactId = "exhaustive-processor"
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}