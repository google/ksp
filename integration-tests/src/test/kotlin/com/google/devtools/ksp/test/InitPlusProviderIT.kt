package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.jar.JarFile

class InitPlusProviderIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("init-plus-provider")

    @Test
    fun testInitPlusProvider() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val resultCleanBuild = gradleRunner.withArguments("clean", "build").build()

        Assert.assertEquals(TaskOutcome.SUCCESS, resultCleanBuild.task(":workload:build")?.outcome)

        val artifact = File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar")
        Assert.assertTrue(artifact.exists())

        JarFile(artifact).use { jarFile ->
            Assert.assertTrue(jarFile.getEntry("TestProcessor.log").size > 0)
            Assert.assertTrue(jarFile.getEntry("TestProcessorInit.log").size > 0)
            Assert.assertTrue(jarFile.getEntry("HelloFromInit.class").size > 0)
            Assert.assertTrue(jarFile.getEntry("HelloFromProvider.class").size > 0)
            Assert.assertTrue(jarFile.getEntry("GeneratedFromInit.class").size > 0)
            Assert.assertTrue(jarFile.getEntry("GeneratedFromProvider.class").size > 0)
        }
    }
}
