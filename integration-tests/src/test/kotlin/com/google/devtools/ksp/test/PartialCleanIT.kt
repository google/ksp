package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class PartialCleanIT(experimentalPsiResolution: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "partial-clean",
        "test-processor",
        experimentalPsiResolution = experimentalPsiResolution
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Boolean> = listOf(true, false)
    }

    @Test
    fun testWorkaroundForIncorrectlyMarkedInputs() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }
        File(project.root, "workload/src/main/kotlin/com/example/Baz.kt").appendText(System.lineSeparator())
        gradleRunner.withArguments("assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }
    }
}
