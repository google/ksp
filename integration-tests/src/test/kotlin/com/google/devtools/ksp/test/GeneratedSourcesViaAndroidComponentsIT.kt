package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File

class GeneratedSourcesViaAndroidComponentsIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground-android-multi", "playground")

    @Test
    fun `test no circular dependency for other source generating tasks depending on ksp`() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("8.12.1")

        File(project.root, "gradle.properties").appendText("\nagpVersion=8.9.0")
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
            Assert.assertTrue(result.output.contains(":workload:kspDebugKotlin"))
            Assert.assertTrue(result.output.contains(":workload:compileDebugAidl"))
            Assert.assertTrue(result.output.contains(":workload:generateDebugSource"))
        }
    }
}
