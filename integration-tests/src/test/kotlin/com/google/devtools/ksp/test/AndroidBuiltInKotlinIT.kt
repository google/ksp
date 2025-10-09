package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File

class AndroidBuiltInKotlinIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground-android-builtinkotlin", "playground")

    @Test
    fun testPlaygroundAndroidWithBuiltInKotlin() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "clean", "build", "minifyReleaseWithR8", "--configuration-cache", "--info", "--stacktrace"
        ).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:build")?.outcome)
            val mergedConfiguration = File(project.root, "workload/build/outputs/mapping/release/configuration.txt")
            assert(mergedConfiguration.exists()) {
                "Merged configuration file not found!\n${printDirectoryTree(project.root)}"
            }
            val configurationText = mergedConfiguration.readText()
            assert("-keep class com.example.AClassBuilder { *; }" in configurationText) {
                "Merged configuration did not contain generated proguard rules!\n$configurationText"
            }
            val outputs = result.output.lines()
            assert("w: [ksp] [workload_debug] Mangled name for internalFun: internalFun\$workload_debug" in outputs)
            assert("w: [ksp] [workload_release] Mangled name for internalFun: internalFun\$workload_release" in outputs)
        }
    }

    @Test
    fun testPlaygroundAndroidWithBuiltInKotlinAGP90() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("9.0.0")

        File(project.root, "gradle.properties").appendText("\nagpVersion=9.0.0-alpha05")

        gradleRunner.withArguments(
            "clean", "build", "minifyReleaseWithR8", "--configuration-cache", "--info", "--stacktrace"
        ).buildAndFail().let { result ->
            Assert.assertTrue(
                result.output.contains(
                    "KSP is not compatible with Android Gradle Plugin's built-in Kotlin. " +
                        "Please disable by adding android.builtInKotlin=false to gradle.properties " +
                        "and apply kotlin(\"android\") plugin"
                )
            )
        }
    }
}
