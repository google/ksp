package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class AGPVersionIT(
    private val agpVersion: String?,
    private val kotlinVersion: String?,
    private val gradleVersion: String?
) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "playground-android-multi",
        "playground"
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "AGP: {0}, KGP: {1}, Gradle: {2}")
        fun data(): Collection<Array<String?>> {
            return listOf(
                // Latest
                arrayOf(null, null, null),

                // Alpha/beta versions
                arrayOf("8.10.0-alpha03", "2.2.10", "8.11.1"),
                arrayOf("8.10.0-alpha03", "2.3.0-RC", "8.11.1"),
                arrayOf("8.12.0-alpha06", "2.2.10", "8.13"),
                arrayOf("8.12.0-alpha06", "2.3.0-RC", "8.13"),
                arrayOf("9.0.0-alpha12", "2.2.10", "9.1.0"),
                arrayOf("9.0.0-alpha12", "2.3.0-RC", "9.1.0"),

                // AGP 8.7.0
                arrayOf("8.7.0", "2.3.0-RC", "8.11.1"),
                arrayOf("8.7.0", "2.2.10", "8.11.1"),
                // AGP 8.8.0
                arrayOf("8.8.0", "2.3.0-RC", "8.11.1"),
                arrayOf("8.8.0", "2.2.10", "8.11.1"),
                // AGP 8.9.0
                arrayOf("8.9.0", "2.3.0-RC", "8.11.1"),
                arrayOf("8.9.0", "2.2.10", "8.11.1"),
                // AGP 8.10.0
                arrayOf("8.10.0", "2.3.0-RC", "8.11.1"),
                arrayOf("8.10.0", "2.2.10", "8.11.1"),
                // AGP 8.11.0
                arrayOf("8.11.0", "2.3.0-RC", "8.13"),
                arrayOf("8.11.0", "2.2.10", "8.13"),
                // AGP 8.12.0
                arrayOf("8.12.0", "2.3.0-RC", "8.13"),
                arrayOf("8.12.0", "2.2.10", "8.13"),
                // AGP 9.0.0-beta01
                arrayOf("9.0.0-beta01", "2.3.0-RC", "9.1.0"),
                arrayOf("9.0.0-beta01", "2.2.10", "9.1.0"),
            )
        }
    }

    @Test
    fun testRunsKSP() {
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
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspDebugKotlin")?.outcome)
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
