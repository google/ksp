package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class BuildCacheIT(useK2: Boolean) {
    @Rule
    @JvmField
    val project1: TemporaryTestProject = TemporaryTestProject("buildcache", "playground", useK2)

    @Rule
    @JvmField
    val project2: TemporaryTestProject = TemporaryTestProject("buildcache", "playground", useK2)

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
            Assert.assertEquals(TaskOutcome.SUCCESS, it.task(":workload:kspKotlin")?.outcome)
        }

        GradleRunner.create().withProjectDir(project2.root).withArguments(
            "--build-cache",
            ":workload:clean",
            "build"
        ).build().let {
            Assert.assertEquals(TaskOutcome.FROM_CACHE, it.task(":workload:kspKotlin")?.outcome)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "K2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
