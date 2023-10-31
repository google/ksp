package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MapAnnotationArgumentsIT(val useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("map-annotation-arguments", "test-processor", useKSP2)

    val expectedErrors = listOf(
        "e: [ksp] unboxedChar: Char != Character\n",
        "e: [ksp] boxedChar: (Char..Char?) != Character\n",
        "e: Error occurred in KSP, check log for detail\n",
    )

    @Test
    fun testMapAnnotationArguments() {
        Assume.assumeFalse(useKSP2)
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("assemble", "-Pksp.map.annotation.arguments.in.java=true").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }

        gradleRunner.withArguments("clean", "assemble", "--rerun-tasks").buildAndFail().let { result ->
            Assert.assertEquals(TaskOutcome.FAILED, result.task(":workload:kspKotlin")?.outcome)
            Assert.assertTrue(expectedErrors.all { it in result.output })
        }

        gradleRunner.withArguments("clean", "assemble", "-Pksp.map.annotation.arguments.in.java=false", "--rerun-tasks")
            .buildAndFail().let { result ->
                Assert.assertEquals(TaskOutcome.FAILED, result.task(":workload:kspKotlin")?.outcome)
                Assert.assertTrue(expectedErrors.all { it in result.output })
            }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
