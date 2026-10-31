package com.google.devtools.ksp.test.primary

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.jar.JarFile

@RunWith(Parameterized::class)
class ProjectIsolationIT(
    private val isolationArgument: String,
    private val gradleVersion: String?
) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "playground",
        experimentalPsiResolution = true
    )

    companion object {
        // The --isolated-projects command line option was added in Gradle 9.7, before that Isolated Projects
        // could only be turned on through a property.
        private const val ISOLATION_CLI_GRADLE_VERSION = "9.7.1"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}, Gradle: {1}")
        fun data(): Collection<Array<String?>> {
            return listOf(
                arrayOf("-Dorg.gradle.unsafe.isolated-projects=true", null),
                arrayOf("-Dorg.gradle.isolated-projects=true", ISOLATION_CLI_GRADLE_VERSION),
                arrayOf("--isolated-projects", ISOLATION_CLI_GRADLE_VERSION)
            )
        }
    }

    @Test
    fun testProjectIsolationResources() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleVersion?.let { gradleRunner.withGradleVersion(it) }

        val result = gradleRunner.withArguments(
            "clean", "build", isolationArgument,
            "--configuration-cache", "--info", "--stacktrace"
        ).build()

        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:build")?.outcome)

        val artifact = File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar")
        Assert.assertTrue("Artifact should exist at ${artifact.absolutePath}", artifact.exists())

        JarFile(artifact).use { jarFile ->
            // This is the resource that KSP generates
            val entry = jarFile.getEntry("TestProcessor.log")
            Assert.assertNotNull("TestProcessor.log should be present in the JAR", entry)
        }
    }
}
