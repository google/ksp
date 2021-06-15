package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.jar.*

class KMPImplementedIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("kmp")

    private fun verify(jarName: String, contents: List<String>) {
        val artifact = File(project.root, jarName)
        Assert.assertTrue(artifact.exists())

        JarFile(artifact).use { jarFile ->
            contents.forEach {
                Assert.assertTrue(jarFile.getEntry(it).size > 0)
            }
        }
    }

    @Test
    fun testAll() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val resultCleanBuild = gradleRunner.withArguments("clean", "build").build()

        Assert.assertEquals(TaskOutcome.SUCCESS, resultCleanBuild.task(":workload:build")?.outcome)

        verify(
            "workload/build/libs/workload-jvm-1.0-SNAPSHOT.jar",
            listOf(
                "com/example/Foo.class"
            )
        )

        verify(
            "workload/build/libs/workload-js-1.0-SNAPSHOT.jar",
            listOf(
                "playground-workload.js"
            )
        )

        verify(
            "workload/build/libs/workload-metadata-1.0-SNAPSHOT.jar",
            listOf(
                "com/example/Foo.kotlin_metadata"
            )
        )
    }
}
