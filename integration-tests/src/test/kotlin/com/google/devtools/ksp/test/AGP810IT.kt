package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File

class AGP810IT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground-android-multi", "playground")

    @Test
    fun testRunsKSP() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("8.11.1")

        File(project.root, "gradle.properties").appendText("\nagpVersion=8.10.0")
        gradleRunner.withArguments(":workload:compileDebugKotlin").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspDebugKotlin")?.outcome)
        }
    }
}
