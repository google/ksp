import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin Symbol Processor"

val kotlinBaseVersion: String by project
val junitVersion: String by project
val googleTruthVersion: String by project
val agpBaseVersion: String by project
val signingKey: String? by project
val signingPassword: String? by project

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
}

plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version ("1.4.32")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlinBaseVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinBaseVersion")
    implementation("org.jetbrains.kotlin:kotlin-compiler-runner:$kotlinBaseVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinBaseVersion")
    // replace AGP dependency w/ gradle-api when we have source registering API available.
    compileOnly("com.android.tools.build:gradle:$agpBaseVersion")
    compileOnly(gradleApi())
    testImplementation(gradleApi())
    testImplementation(project(":api"))
    testImplementation("junit:junit:$junitVersion")
    testImplementation("com.google.truth:truth:$googleTruthVersion")
    testImplementation(gradleTestKit())
}

tasks.named("validatePlugins").configure {
    onlyIf {
        // while traversing classpath, this hits a class not found issue.
        // Disabled until gradle kotlin version and our kotlin version matches
        // java.lang.ClassNotFoundException: org/jetbrains/kotlin/compilerRunner/KotlinLogger
        false
    }
}

gradlePlugin {
    plugins {
        create("symbol-processing-gradle-plugin") {
            id = "com.google.devtools.ksp"
            displayName = "com.google.devtools.ksp.gradle.plugin"
            implementationClass = "com.google.devtools.ksp.gradle.KspGradleSubplugin"
            description = "Kotlin symbol processing integration for Gradle"
        }
    }
}

tasks {
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(project.sourceSets.main.get().allSource)
    }
}

val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        // the name of this publication should match the name java-gradle-plugin looks up
        // https://github.com/gradle/gradle/blob/master/subprojects/plugin-development/src/main/java/org/gradle/plugin/devel/plugins/MavenPluginPublishPlugin.java#L73
        this.create<MavenPublication>("pluginMaven") {
            artifactId = "symbol-processing-gradle-plugin"
            artifact(tasks["sourcesJar"])
            artifact(tasks["dokkaJavadocJar"])
            pom {
                name.set("symbol-processing-gradle-plugin")
                description.set("Kotlin symbol processing integration for Gradle")
            }
        }
    }
}

signing {
    isRequired = hasProperty("signingKey") && !gradle.taskGraph.hasTask("publishToMavenLocal")
    sign(extensions.getByType<PublishingExtension>().publications)
}

/**
 * Create a properties file with that can be read from the gradle-plugin tests to setup test
 * projects.
 */
val testPropsOutDir = project.layout.buildDirectory.dir("test-config")
val writeTestPropsTask = tasks.register<WriteProperties>("prepareTestConfiguration") {
    description = "Generates a properties file with the current environment for gradle integration tests"
    this.setOutputFile(
        testPropsOutDir.map {
            it.file("testprops.properties")
        }
    )
    property("kspVersion", version)
    property("mavenRepoDir", File(rootProject.buildDir, "repos/test").absolutePath)
    property("kspProjectRootDir", rootProject.projectDir.absolutePath)
    property("processorClasspath", project.tasks["compileTestKotlin"].outputs.files.asPath)
    property("kspCompilerRunner", project.properties.getOrDefault("ksp.compiler.runner", "inherited") as String)
}

java {
    sourceSets {
        test {
            resources.srcDir(testPropsOutDir)
        }
    }
}

tasks.named("compileTestKotlin").configure {
    dependsOn(writeTestPropsTask)
}

tasks.named<Test>("test").configure {
    dependsOn(":api:publishAllPublicationsToTestRepository")
    dependsOn(":gradle-plugin:publishAllPublicationsToTestRepository")
    dependsOn(":symbol-processing:publishAllPublicationsToTestRepository")
}

abstract class WriteVersionSrcTask @Inject constructor(
    @get:Input val kspVersion: String,
    @get:Input val kotlinVersion: String,
    @get:org.gradle.api.tasks.OutputDirectory val outputSrcDir: File
) : DefaultTask() {
    @TaskAction
    fun generate() {
        File(outputSrcDir, "KSPVersions.kt").writeText(
            """
            package com.google.devtools.ksp.gradle
            val KSP_KOTLIN_BASE_VERSION = "$kotlinVersion"
            val KSP_VERSION = "$kspVersion"
            """.trimIndent()
        )
    }
}

val kspVersionDir = File(project.buildDir, "generated/ksp-versions")
val writeVersionSrcTask = tasks.register<WriteVersionSrcTask>(
    "generateKSPVersions",
    version.toString(),
    kotlinBaseVersion,
    kspVersionDir
)

kotlin {
    sourceSets {
        main {
            kotlin.srcDir(writeVersionSrcTask.map { it.outputSrcDir })
        }
    }
}
