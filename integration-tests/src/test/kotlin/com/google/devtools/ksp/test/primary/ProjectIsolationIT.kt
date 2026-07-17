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
    private val propertyName: String,
    private val gradleVersion: String?
) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "playground",
        experimentalPsiResolution = true
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}, Gradle: {1}")
        fun data(): Collection<Array<String?>> {
            return listOf(
                arrayOf("org.gradle.unsafe.isolated-projects", null),
                arrayOf("org.gradle.isolated-projects", "9.7.0-rc-1")
            )
        }
    }

    @Test
    fun testProjectIsolationResources() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleVersion?.let { gradleRunner.withGradleVersion(it) }

        val result = gradleRunner.withArguments(
            "clean", "build", "-D$propertyName=true",
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
