package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AndroidDataBindingIT(useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("android-data-binding", useKSP2 = useKSP2)

    @Test
    fun testPlaygroundAndroid() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        // Disabling configuration cache. See https://github.com/google/ksp/issues/299 for details
        gradleRunner.withArguments(
            "clean",
            ":app:assemble",
            "--configuration-cache-problems=warn",
            "--info",
            "--stacktrace"
        )
            .build().let { result ->
                val output = result.output.lines()
                val kspTask = output.filter {
                    it.contains(":app:kspDebugKotlin")
                }
                Assert.assertTrue(kspTask.isNotEmpty())
            }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
