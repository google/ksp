package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.jar.*

class VersionCheckIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground")

    @Test
    fun testVersion() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val result = gradleRunner.withArguments("-Dkotlin.compiler.execution.strategy=in-process", "-PkotlinVersion=1.4.20", "clean", "build").buildAndFail()
        Assert.assertTrue(result.output.contains("Please pick the same version of ksp and kotlin plugins."))
    }
}