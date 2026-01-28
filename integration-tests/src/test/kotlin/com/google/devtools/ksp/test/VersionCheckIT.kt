package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class VersionCheckIT(val useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground", useKSP2 = useKSP2)

    fun String.containsOnce(substring: String, ignoreCase: Boolean = false): Boolean {
        val firstIndex = this.indexOf(substring, ignoreCase = ignoreCase)
        return firstIndex != -1 && firstIndex == this.lastIndexOf(substring, ignoreCase = ignoreCase)
    }

    @Test
    fun testKsp1Usage() {
        Assume.assumeFalse(useKSP2)
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val result = gradleRunner.withArguments(
            "clean", "build"
        ).run()

        Assert.assertTrue(
            result.output.containsOnce(
                "We noticed you are using KSP1 which is deprecated and support for it will be removed " +
                    "soon - please migrate to KSP2 as soon as possible. KSP1 will no " +
                    "longer be compatible with Android Gradle Plugin 9.0.0 (and above) and Kotlin 2.3.0 (and above)"
            )
        )
    }

    @Test
    fun testKsp1Usage_noWarning() {
        Assume.assumeFalse(useKSP2)
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val result = gradleRunner.withArguments(
            "-Pwarn.on.ksp1.usage=false", "clean", "build"
        ).run()

        Assert.assertFalse(
            result.output.containsOnce(
                "We noticed you are using KSP1 which is deprecated and support for it will be removed " +
                    "soon - please migrate to KSP2 as soon as possible. KSP1 will no " +
                    "longer be compatible with Android Gradle Plugin 9.0.0 (and above) and Kotlin 2.3.0 (and above)"
            )
        )
    }

    @Test
    fun testVersion() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val result = gradleRunner.withArguments(
            "-PkotlinVersion=2.0.0", "clean", "build"
        ).run()
        if (!useKSP2) {
            Assert.assertTrue(result.output.contains("is too new for kotlin"))
        } else {
            Assert.assertFalse(result.output.contains("is too new for kotlin"))
        }
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

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
