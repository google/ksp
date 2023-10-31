package com.google.devtools.ksp.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class KotlinConstsInJavaIT(useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("kotlin-consts-in-java", useKSP2 = useKSP2)

    private fun GradleRunner.buildAndCheck(vararg args: String, extraCheck: (BuildResult) -> Unit = {}) =
        buildAndCheckOutcome(*args, outcome = TaskOutcome.SUCCESS, extraCheck = extraCheck)

    private fun GradleRunner.buildAndCheckOutcome(
        vararg args: String,
        outcome: TaskOutcome,
        extraCheck: (BuildResult) -> Unit = {}
    ) {
        val result = this.withArguments(*args).build()

        Assert.assertEquals(outcome, result.task(":workload:kspKotlin")?.outcome)

        extraCheck(result)
    }

    @Test
    fun testKotlinConstsInJava() {
        // FIXME: `clean` fails to delete files on windows.
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withDebug(true)
        gradleRunner.buildAndCheck(":workload:kspKotlin")

        File(project.root, "workload/src/main/java/com/example/JavaClass.java").appendText("\n")
        gradleRunner.buildAndCheck(":workload:kspKotlin")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
