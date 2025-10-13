package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.jar.*

class MultiplatformIT() {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground-mpp", "playground")

    @Test
    fun testJVM() {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("mac", ignoreCase = true))
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val resultCleanBuild =
            gradleRunner.withArguments("--configuration-cache-problems=warn", "clean", "build").build()

        Assert.assertEquals(TaskOutcome.SUCCESS, resultCleanBuild.task(":workload:build")?.outcome)

        val artifact = File(project.root, "workload/build/libs/workload-jvm-1.0-SNAPSHOT.jar")
        Assert.assertTrue(artifact.exists())

        JarFile(artifact).use { jarFile ->
            Assert.assertTrue(jarFile.getEntry("TestProcessor.log").size > 0)
            Assert.assertTrue(jarFile.getEntry("hello/HELLO.class").size > 0)
            Assert.assertTrue(jarFile.getEntry("com/example/AClassBuilder.class").size > 0)
        }
    }
}
