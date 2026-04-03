package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PartialCleanIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "partial-clean", "test-processor")
        project.setup()
    }

    @Test
    fun testWorkaroundForIncorrectlyMarkedInputs() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("assemble").build().let { result ->
            Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }
        File(project.root, "workload/src/main/kotlin/com/example/Baz.kt").appendText(System.lineSeparator())
        gradleRunner.withArguments("assemble").build().let { result ->
            Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }
    }
}
