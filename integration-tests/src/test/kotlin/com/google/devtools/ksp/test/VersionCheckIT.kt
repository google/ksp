package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class VersionCheckIT() {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground")

    @Test
    fun testVersion() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val result = gradleRunner.withArguments(
            "-PkotlinVersion=2.0.0", "clean", "build"
        ).run()
        Assert.assertFalse(result.output.contains("is too new for kotlin"))
    }

    @Test
    fun testVersionOK() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val result = gradleRunner.withArguments(
            "clean", "build"
        ).run()
        Assert.assertFalse(result.output.contains("is too new for kotlin"))
        Assert.assertFalse(result.output.contains("is too old for kotlin"))
    }

    @Test
    fun testMuteVersionCheck() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val result = gradleRunner.withArguments(
            "-PkotlinVersion=2.0.0", "-Pksp.version.check=false", "clean", "build"
        ).run()
        Assert.assertFalse(result.output.contains("is too new for kotlin"))
    }
}
