package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GeneratedSourcesViaAndroidComponentsIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "playground-android-multi", "playground")
        project.setup()
    }

    @Test
    fun `test no circular dependency for other source generating tasks depending on ksp`() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        File(project.root, "workload/build.gradle.kts").appendText(
            """                
                android {
                    buildFeatures {
                        aidl = true
                    }

                    androidComponents.onVariants { variant ->
                        val name = variant.name
                
                        val task = project.tasks.register("generate${'$'}{name.replaceFirstChar(Char::uppercase)}Source", SourceGenerationTask::class) {
                            dependsOn(project.tasks.named("ksp${'$'}{name.replaceFirstChar(Char::uppercase)}Kotlin")) // ksp must run before generator task
                            getOutputDirectory().set(project.layout.buildDirectory.dir("generated/${'$'}{name}"))
                        }
                
                        ksp.excludedSources.from(task) // This breaks the circular dependency
                        variant.sources.java?.addGeneratedSourceDirectory(task, SourceGenerationTask::getOutputDirectory)
                
                        sourceSets {
                            getByName(name)
                                .aidl
                                .srcDirs(
                                    layout.buildDirectory
                                        .dir("generated/ksp/${'$'}name/resources/aidl_generated")
                                        .get()
                                        .asFile
                                )
                        }
                
                        afterEvaluate {
                            tasks.named("compile${'$'}{name.replaceFirstChar(Char::uppercase)}Aidl") {
                                dependsOn("ksp${'$'}{name.replaceFirstChar(Char::uppercase)}Kotlin")
                            }
                
                            ksp.excludedSources.from(tasks.named("compile${'$'}{name.replaceFirstChar(Char::uppercase)}Aidl")) // This breaks the circular dependency
                        }
                    }
                }

                abstract class SourceGenerationTask : DefaultTask() {
                    @OutputDirectory
                    abstract fun getOutputDirectory(): DirectoryProperty
                    @TaskAction
                    fun generateCode() { }
                }
                
            """.trimIndent()
        )

        gradleRunner.withArguments(":workload:assembleDebug", "--dry-run", "--stacktrace").build().let { result ->
            Assertions.assertTrue(result.output.contains(":workload:kspDebugKotlin"))
            Assertions.assertTrue(result.output.contains(":workload:compileDebugAidl"))
            Assertions.assertTrue(result.output.contains(":workload:generateDebugSource"))
        }
    }
}
