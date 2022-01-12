package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Ignore
class VersionCheckIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground")

    @Test
    fun testVersion() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val result = gradleRunner.withArguments(
            "-PkotlinVersion=1.4.20", "clean", "build"
        ).buildAndFail()
        Assert.assertTrue(result.output.contains("is too new for kotlin"))
    }

    @Test
    fun testVersionOK() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val result = gradleRunner.withArguments(
            "clean", "build"
        ).build()
        Assert.assertFalse(result.output.contains("is too new for kotlin"))
        Assert.assertFalse(result.output.contains("is too old for kotlin"))
    }

    @Test
    fun testMuteVersionCheck() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val result = gradleRunner.withArguments(
            "-PkotlinVersion=1.4.20", "-Pksp.version.check=false", "clean", "build"
        ).buildAndFail()
        Assert.assertFalse(result.output.contains("is too new for kotlin"))
    }
}
