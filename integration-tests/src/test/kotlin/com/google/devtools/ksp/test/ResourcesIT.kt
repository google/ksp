package com.google.devtools.ksp.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.kotlin.incremental.deleteRecursivelyOrThrow
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ResourcesIT(val useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("resources", useKSP2 = useKSP2)

    @Test
    fun testResources() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments(":workload:assemble").build().let { result ->
            result.assertOutputContains("foo.txt: Hello")
            result.assertOutputContains("foo/bar/baz/quux: World")
        }
    }

    @Test
    fun testUpToDate() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments(":workload:assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }
        gradleRunner.withArguments(":workload:assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.UP_TO_DATE, result.task(":workload:kspKotlin")?.outcome)
        }
    }

    @Test
    fun skipsWhenEmpty() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        project.root.resolve("workload/src/main/resources").deleteRecursivelyOrThrow()
        gradleRunner.withArguments(":workload:assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.NO_SOURCE, result.task(":workload:kspKotlin")?.outcome)
        }
    }

    @Test
    fun incremental() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments(":workload:assemble").build().let { result ->
            result.assertOutputContains("foo.txt: Hello")
            result.assertOutputContains("foo/bar/baz/quux: World")
        }
        project.root.resolve("workload/src/main/resources/foo.txt").writeText("Goodbye")
        gradleRunner.withArguments(":workload:assemble").build().let { result ->
            result.assertOutputContains("foo.txt: Goodbye")
            result.assertOutputContains("foo/bar/baz/quux: World")
        }
    }

    private fun BuildResult.assertOutputContains(expected: String) {
        Assert.assertTrue(
            "Expected output to contain $expected but did not. Output:\n$output",
            expected in output
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
