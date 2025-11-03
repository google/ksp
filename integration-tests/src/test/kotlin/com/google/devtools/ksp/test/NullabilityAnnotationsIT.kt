package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Assume
import org.junit.Rule
import org.junit.Test

class NullabilityAnnotationsIT() {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("nullability-annotations")

    private fun GradleRunner.buildAndCheck(vararg args: String, extraCheck: (BuildResult) -> Unit = {}) =
        buildAndCheckOutcome(*args, outcome = TaskOutcome.SUCCESS, extraCheck = extraCheck)

    private fun GradleRunner.buildAndCheckOutcome(
        vararg args: String,
        outcome: TaskOutcome,
        extraCheck: (BuildResult) -> Unit = {}
    ) {
        val result = this.withArguments(*args).build()

        Assert.assertEquals(outcome, result.task(":workload:build")?.outcome)

        extraCheck(result)
    }

    @Test
    fun testNullableAnnotations() {
        // FIXME: `clean` fails to delete files on windows.
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.buildAndCheck("clean", "build") { result ->
            Assert.assertTrue(result.output.contains("w: [ksp] [Nullability check] s0: (String..String?)\n"))
            Assert.assertTrue(result.output.contains("w: [ksp] [Nullability check] s1: String?\n"))
            Assert.assertTrue(result.output.contains("w: [ksp] [Nullability check] s2: String\n"))
            Assert.assertTrue(result.output.contains("w: [ksp] [Nullability check] s3: [@Nullable] String?\n"))
            Assert.assertTrue(result.output.contains("w: [ksp] [Nullability check] s4: [@NotNull] String\n"))
            Assert.assertTrue(result.output.contains("w: [ksp] [Nullability check] s5: [@Nullable] String?\n"))
            Assert.assertTrue(result.output.contains("w: [ksp] [Nullability check] s6: [@NonNull] String\n"))
            Assert.assertTrue(result.output.contains("w: [ksp] [Nullability check] s7: (String..String?)\n"))
            Assert.assertTrue(result.output.contains("w: [ksp] [Nullability check] s8: (String..String?)\n"))
        }
    }
}
