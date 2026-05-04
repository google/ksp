package com.google.devtools.ksp.test.primary

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.jar.JarFile

class ProjectIsolationIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "playground",
        experimentalPsiResolution = true
    )

    @Test
    fun testProjectIsolationResources() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val result = gradleRunner.withArguments(
            "clean", "build", "-Dorg.gradle.unsafe.isolated-projects=true",
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
