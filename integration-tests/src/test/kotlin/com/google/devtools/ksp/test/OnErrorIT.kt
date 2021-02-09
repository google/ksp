package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class OnErrorIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("on-error")

    @Test
    fun testOnError() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "assemble").buildAndFail().let { result ->
            val errors = result.output.split("\n").filter { it.startsWith("e: [ksp]") }
            Assert.assertEquals("e: [ksp] Error processor: errored at 2", errors.first())
            Assert.assertEquals("e: [ksp] NormalProcessor called error on 2", errors.last())
        }
    }
}