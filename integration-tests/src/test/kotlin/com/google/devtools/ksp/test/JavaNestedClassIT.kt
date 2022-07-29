package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class JavaNestedClassIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("javaNestedClass")

    @Test
    fun testJavaNestedClass() {

        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val resultCleanBuild = gradleRunner.withArguments("clean", "build").build()
        Assert.assertEquals(TaskOutcome.SUCCESS, resultCleanBuild.task(":workload:build")?.outcome)
    }
}
