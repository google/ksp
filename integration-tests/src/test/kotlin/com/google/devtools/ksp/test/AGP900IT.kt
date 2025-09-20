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
class AGP900IT(useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground-android-multi", "playground", useKSP2)

    @Test
    fun testRunsKSP() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("9.0.0")

        File(project.root, "gradle.properties").appendText("\nagpVersion=9.0.0-alpha05")
        File(project.root, "gradle.properties").appendText("\nandroid.builtInKotlin=false")
        File(project.root, "gradle.properties").appendText("\nandroid.newDsl=false")

        gradleRunner.withArguments(":workload:compileDebugKotlin").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspDebugKotlin")?.outcome)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
