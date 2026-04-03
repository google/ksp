package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

class AGPVersionBuiltInKotlinIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    companion object {
        @JvmStatic
        fun data(): Collection<Arguments> {
            return listOf(
                Arguments.of("9.0.0-alpha14", "2.2.10", "9.1.0"),
                Arguments.of("9.0.0-alpha14", "2.3.0-RC", "9.1.0"),
                Arguments.of("9.0.0-beta01", "2.3.0-RC", "9.1.0"),
                Arguments.of("9.0.0-beta01", "2.2.10", "9.1.0"),
            )
        }
    }

    @ParameterizedTest(name = "AGP: {0}, KGP: {1}, Gradle: {2}")
    @MethodSource("data")
    fun testRunsKSP(agpVersion: String?, kotlinVersion: String?, gradleVersion: String?) {
        project = TemporaryTestProject(
            tempDir,
            "playground-android-builtinkotlin",
            "playground"
        )
        project.setup()

        val gradleRunner = GradleRunner.create()
            .withProjectDir(project.root)
            .withArguments(":workload:compileDebugKotlin")
        gradleVersion?.let { gradleRunner.withGradleVersion(it) }

        agpVersion?.let { project.setAgpVersion(it) }
        kotlinVersion?.let {
            project.setKotlinVersion(it)
            setKotlinInBuildClasspath(it)
        }

        gradleRunner.build().let { result ->
            Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspDebugKotlin")?.outcome)
        }
    }

    private fun setKotlinInBuildClasspath(version: String) {
        // override AGPs bundled kotlin gradle plugin version
        File(project.root, "workload/build.gradle.kts").appendText(
            """
                buildscript {
                    dependencies {
                        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$version")
                    }
                }
            """.trimIndent()
        )
    }
}
