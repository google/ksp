import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

description = "Kotlin Symbol Processor"

val signingKey: String? by project
val signingPassword: String? by project

plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
    id("com.android.lint")
}

dependencies {
    compileOnly(libs.kotlin.gradlePluginApi)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.compiler.embeddable)
    // replace AGP dependency w/ gradle-api when we have source registering API available.
    compileOnly(libs.agp)
    compileOnly(gradleApi())
    compileOnly(project(":kotlin-analysis-api"))
    // Ensure stdlib version is not inconsistent due to kotlin plugin version.
    compileOnly(libs.kotlin.stdlib)
    implementation(project(":api"))
    implementation(project(":common-deps"))
    testImplementation(gradleApi())
    testImplementation(project(":api"))
    testImplementation(libs.junit4)
    testImplementation(libs.google.truth)
    testImplementation(gradleTestKit())

    lintChecks(libs.androidx.lintGradle)
}

kotlin {
    compilerOptions {
        jvmDefault.set(JvmDefaultMode.ENABLE)
    }
}

tasks.validatePlugins {
    // TODO: https://github.com/gradle/gradle/issues/22600
    enableStricterValidation = true
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

val sourcesJar =
    tasks.register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(project.sourceSets.main.map { it.allSource })
    }
val dokkaJavadocJar =
    tasks.register<Jar>("dokkaJavadocJar") {
        archiveClassifier.set("javadoc")
        from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    }

publishing {
    publications {
        // the name of this publication should match the name java-gradle-plugin looks up
        // https://github.com/gradle/gradle/blob/master/subprojects/plugin-development/src/main/java/org/gradle/plugin/devel/plugins/MavenPluginPublishPlugin.java#L73
        this.create<MavenPublication>("pluginMaven") {
            artifactId = "symbol-processing-gradle-plugin"
            artifact(sourcesJar)
            artifact(dokkaJavadocJar)
            pom {
                name.set("symbol-processing-gradle-plugin")
                description.set("Kotlin symbol processing integration for Gradle")
            }
        }
    }
}

signing {
    isRequired = hasProperty("signingKey")
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(extensions.getByType<PublishingExtension>().publications)
}

/**
 * Create a properties file with that can be read from the gradle-plugin tests to setup test
 * projects.
 */
val testPropsOutDir = layout.buildDirectory.dir("test-config")
val writeTestPropsTask =
    tasks.register<WriteProperties>("prepareTestConfiguration") {
        description =
            "Generates a properties file with the current environment for gradle integration tests"
        destinationFile = testPropsOutDir.map { it.file("testprops.properties") }
        property("kspVersion", version)
        property("kotlinStdlibFloor", libs.versions.kotlin.stdlib.floor.get())
        property(
            "mavenRepoDir",
            rootProject.layout.buildDirectory.dir("repos/test").get().asFile.absolutePath,
        )
        property("kspProjectRootDir", rootDir.absolutePath)
        property("processorClasspath", project.tasks["compileTestKotlin"].outputs.files.asPath)
    }

normalization {
    runtimeClasspath {
        properties("**/testprops.properties") {
            ignoreProperty("kspProjectRootDir")
            ignoreProperty("mavenRepoDir")
            ignoreProperty("processorClasspath")
        }
    }
}

java {
    sourceSets {
        test {
            resources.srcDir(testPropsOutDir)
        }
    }
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.named("compileTestKotlin").configure {
    dependsOn(writeTestPropsTask)
}

tasks.named("processTestResources").configure {
    dependsOn(writeTestPropsTask)
}

// All tests, including the published metadata tasks below, read the published test repository.
tasks.withType<Test>().configureEach {
    dependsOn(":api:publishAllPublicationsToTestRepository")
    dependsOn(":common-deps:publishAllPublicationsToTestRepository")
    dependsOn(":gradle-plugin:publishAllPublicationsToTestRepository")
    dependsOn(":kotlin-analysis-api:publishAllPublicationsToTestRepository")
    dependsOn(":symbol-processing:publishAllPublicationsToTestRepository")
    dependsOn(":symbol-processing-aa-embeddable:publishAllPublicationsToTestRepository")
    inputs
        .files(
            rootProject.layout.buildDirectory.dir("repos/test").map { dir ->
                fileTree(dir) { include("**/*.pom", "**/*.module", "**/maven-metadata.xml") }
            }
        )
        .withPropertyName("publishedTestRepoMetadata")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

/**
 * Registers a task that runs only `PublishedMetadataTest`, which compares the published POMs and Gradle Module
 * Metadata against a golden file. The regular `test` task runs it too, but never updates the golden.
 */
fun registerPublishedMetadataTask(name: String, taskDescription: String, update: Boolean) =
    tasks.register<Test>(name) {
        group = "verification"
        description = taskDescription
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        filter.includeTestsMatching("com.google.devtools.ksp.gradle.PublishedMetadataTest")
        if (update) {
            systemProperty("updateTestSnapshots", "true")
            outputs.upToDateWhen { false }
        }
    }

registerPublishedMetadataTask(
    "checkPublishedMetadata",
    "Checks the published POMs and Gradle Module Metadata against the golden file.",
    update = false,
)
registerPublishedMetadataTask(
    "updatePublishedMetadata",
    "Updates the golden file with the published POMs and Gradle Module Metadata.",
    update = true,
)

abstract class WriteVersionSrcTask : DefaultTask() {
    @get:Input abstract val kspVersion: Property<String>
    @get:Input abstract val coroutinesVersion: Property<String>

    @get:OutputDirectory abstract val outputSrcDir: DirectoryProperty

    @TaskAction
    fun generate() {
        outputSrcDir
            .file("KSPVersions.kt")
            .get()
            .asFile
            .writeText(
                """
            package com.google.devtools.ksp.gradle
            val KSP_VERSION = "${kspVersion.get()}"
            val KSP_COROUTINES_VERSION = "${coroutinesVersion.get()}"
            """
                    .trimIndent()
            )
    }
}

val writeVersionSrcTask =
    tasks.register<WriteVersionSrcTask>("generateKSPVersions") {
        kspVersion = version.toString()
        coroutinesVersion = libs.versions.aa.coroutines.get()
        outputSrcDir = layout.buildDirectory.dir("generated/ksp-versions")
    }

kotlin {
    sourceSets {
        main {
            kotlin.srcDir(writeVersionSrcTask)
        }
    }
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

lint {
    baseline = file("lint-baseline.xml")
    // GradleDependency suggest library upgrades, thus it is not useful in this case.
    disable.add("GradleDependency")
}
