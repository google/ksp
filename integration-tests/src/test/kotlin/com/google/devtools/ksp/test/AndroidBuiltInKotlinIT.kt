package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.utils.assertMergedConfigurationOutput
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
            assertMergedConfigurationOutput(project, "-keep class com.example.AClassBuilder { *; }")
            val outputs = result.output.lines()
            assert("w: [ksp] [workload_debug] Mangled name for internalFun: internalFun\$workload_debug" in outputs)
            assert("w: [ksp] [workload_release] Mangled name for internalFun: internalFun\$workload_release" in outputs)
        }
    }

    @Test
    fun testPlaygroundAndroidWithBuiltInKotlinAGP90BelowAlpha12() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("9.0.0")

        File(project.root, "gradle.properties").appendText("\nagpVersion=9.0.0-alpha05")

        gradleRunner.withArguments(
            "clean", "build", "minifyReleaseWithR8", "--configuration-cache", "--info", "--stacktrace"
        ).buildAndFail().let { result ->
            Assert.assertTrue(
                result.output.contains(
                    "KSP is not compatible with Android Gradle Plugin's built-in Kotlin prior to AGP " +
                        "version 9.0.0-alpha12. Please upgrade to AGP 9.0.0-alpha12 or alternatively disable " +
                        "built-in kotlin by adding android.builtInKotlin=false and android.newDsl=false to " +
                        "gradle.properties and apply kotlin(\"android\") plugin"
                )
            )
        }
    }

    @Test
    fun testPlaygroundAndroidWithBuiltInKotlinAGP90AboveAlpha12() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("9.0.0")

        File(project.root, "gradle.properties").appendText("\nagpVersion=9.0.0-alpha12")

        gradleRunner.withArguments(
            "clean", "build", "minifyReleaseWithR8", "--configuration-cache", "--info", "--stacktrace"
        ).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:build")?.outcome)
            assertMergedConfigurationOutput(project, "-keep class com.example.AClassBuilder { *; }")
            val outputs = result.output.lines()
            assert("w: [ksp] [workload_debug] Mangled name for internalFun: internalFun\$workload_debug" in outputs)
            assert("w: [ksp] [workload_release] Mangled name for internalFun: internalFun\$workload_release" in outputs)
        }
    }
}
