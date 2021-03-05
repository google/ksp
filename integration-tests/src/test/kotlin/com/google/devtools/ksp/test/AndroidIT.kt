package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.jar.*

class AndroidIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground-android", "playground")

    @Test
    fun testPlaygroundAndroid() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "build", "--info", "--stacktrace").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:build")?.outcome)
        }
    }
}