package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class AndroidComponentsIT(useKSP2: Boolean) {

    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground-android-multi", "playground", useKSP2)

    @Test
    fun testDependencyResolutionCheck() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("8.12.1")

        File(project.root, "gradle.properties").appendText("\nagpVersion=8.9.0")
        gradleRunner.withArguments(":workload:compileDebugKotlin").build().let { result ->
            Assert.assertFalse(result.output.contains("was resolved during configuration time."))
        }
    }

    @Test
    fun testBreakingCircularTaskDependencyWithAndroidComponentGeneratedCode() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("8.12.1")

        File(project.root, "gradle.properties").appendText("\nagpVersion=8.9.0")
        File(project.root, "workload/build.gradle.kts").appendText(
            """
                android {
                    flavorDimensions += listOf("tier")
                    productFlavors {
                        create("free") {
                            dimension = "tier"
                        }
                    }
                }
                configurations.matching { it.name.startsWith("ksp") && !it.name.endsWith("ProcessorClasspath") }.all {
                    // Make sure ksp configs are not empty.
                    project.dependencies.add(name, "androidx.room:room-compiler:2.4.2")
                }
                androidComponents {
                    onVariants { variant ->
                        val task = project.tasks.register(variant.name + "Gen", GenTask::class) {
                            dependsOn(project.tasks.named("ksp" + variant.name.replaceFirstChar(Char::uppercase) + "Kotlin"))
                            getOutputDirectory().set(project.layout.buildDirectory.dir("generated/" + variant.name))
                        }
                        ksp.excludedSources.from(task) // This breaks the circular dependency
                        variant.sources.java?.addGeneratedSourceDirectory(task, GenTask::getOutputDirectory)
                    }
                }
                abstract class GenTask : DefaultTask() {
                    @OutputDirectory
                    abstract fun getOutputDirectory(): DirectoryProperty

                    @TaskAction
                    fun generateCode() { }
                }
            """.trimIndent()
        )

        gradleRunner.withArguments(":workload:assemble", "--dry-run", "--stacktrace").build().let { result ->
            val outputs = result.output.lines().joinToString("\n")
            println(outputs)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
