package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BuildCacheIT {
    @TempDir
    lateinit var tempDir1: File

    @TempDir
    lateinit var tempDir2: File

    lateinit var project1: TemporaryTestProject
    lateinit var project2: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project1 = TemporaryTestProject(tempDir1, "buildcache", "playground")
        project1.setup()
        project2 = TemporaryTestProject(tempDir2, "buildcache", "playground")
        project2.setup()
    }

    @Test
    fun testBuildCache() {
        val buildCacheDir = File(project1.root, "build-cache").absolutePath.replace(File.separatorChar, '/')
        File(project1.root, "gradle.properties").appendText("\nbuildCacheDir=$buildCacheDir")
        File(project2.root, "gradle.properties").appendText("\nbuildCacheDir=$buildCacheDir")

        GradleRunner.create().withProjectDir(project1.root).withArguments(
            "--build-cache",
            ":workload:clean",
            "build"
        ).build().let {
            Assertions.assertEquals(TaskOutcome.SUCCESS, it.task(":workload:kspKotlin")?.outcome)
        }

        GradleRunner.create().withProjectDir(project2.root).withArguments(
            "--build-cache",
            ":workload:clean",
            "build"
        ).build().let {
            Assertions.assertEquals(TaskOutcome.FROM_CACHE, it.task(":workload:kspKotlin")?.outcome)
        }
    }
}
