import com.google.devtools.ksp.configureKtlint
import com.google.devtools.ksp.configureKtlintApplyToIdea
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

val sonatypeUserName: String? by project
val sonatypePassword: String? by project

val kotlinBaseVersion: String? by project
if (extra.has("kspOnlyVersion") && kotlinBaseVersion != null) {
    val kspOnlyVersion = extra.get("kspOnlyVersion") as String
    extra.set("kspVersion", "$kotlinBaseVersion-$kspOnlyVersion")
}

if (!extra.has("kspVersion")) {
    extra.set("kspVersion", "2.0.255-SNAPSHOT")
}

repositories {
    mavenCentral()
    maven("https://redirector.kotlinlang.org/maven/bootstrap/")
}

plugins {
    kotlin("jvm")
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"

    // Adding plugins used in multiple places to the classpath for centralized version control
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
    id("org.jetbrains.dokka") version "1.9.20" apply false
}

nexusPublishing {
    packageGroup.set("com.google.devtools.ksp")
    repositories {
        sonatype {
            username.set(sonatypeUserName)
            password.set(sonatypePassword)
        }
    }
}

version = rootProject.extra.get("kspVersion") as String

configureKtlintApplyToIdea()
subprojects {
    group = "com.google.devtools.ksp"
    version = rootProject.extra.get("kspVersion") as String
    configureKtlint()
    repositories {
        mavenCentral()
        google()
        maven("https://redirector.kotlinlang.org/maven/bootstrap/")
        maven("https://www.jetbrains.com/intellij-repository/releases")
    }
    pluginManager.withPlugin("maven-publish") {
        val publishExtension = extensions.getByType<PublishingExtension>()
        publishExtension.repositories {
            if (extra.has("outRepo")) {
                val outRepo = extra.get("outRepo") as String
                maven {
                    url = File(outRepo).toURI()
                }
            } else {
                mavenLocal()
            }
            maven {
                name = "test"
                url = uri("${rootProject.layout.buildDirectory.get().asFile}/repos/test")
            }
        }
        publishExtension.publications.whenObjectAdded {
            check(this is MavenPublication) {
                "unexpected publication $this"
            }
            val publication = this
            publication.pom {
                url.set("https://goo.gle/ksp")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        name.set("KSP Team")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/google/ksp.git")
                    developerConnection.set("scm:git:https://github.com/google/ksp.git")
                    url.set("https://github.com/google/ksp")
                }
            }
        }
    }

    val compileJavaVersion = JavaLanguageVersion.of(17)

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        configure<JavaPluginExtension> {
            toolchain.languageVersion.set(compileJavaVersion)
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        configure<KotlinJvmProjectExtension> {
            compilerOptions {
                jvmTarget = JvmTarget.JVM_1_8
                languageVersion.set(KotlinVersion.KOTLIN_1_9)
                apiVersion.set(languageVersion)
            }
            jvmToolchain {
                languageVersion = compileJavaVersion
            }
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions.freeCompilerArgs.add("-Xskip-prerelease-check")
    }
}
