package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarFile

class InitPlusProviderIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "init-plus-provider")
        project.setup()
    }

    @Test
    fun testInitPlusProvider() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val resultCleanBuild = gradleRunner.withArguments("clean", "build").build()

        Assertions.assertEquals(TaskOutcome.SUCCESS, resultCleanBuild.task(":workload:build")?.outcome)

        val artifact = File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar")
        Assertions.assertTrue(artifact.exists())

        JarFile(artifact).use { jarFile ->
            Assertions.assertTrue(jarFile.getEntry("TestProcessor.log").size > 0)
            Assertions.assertTrue(jarFile.getEntry("HelloFromProvider.class").size > 0)
            Assertions.assertTrue(jarFile.getEntry("GeneratedFromProvider.class").size > 0)
        }
    }
}
