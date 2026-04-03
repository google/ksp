package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JavaOnlyIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "java-only", "test-processor")
        project.setup()
    }

    @Test
    fun testJavaOnly() {
        // FIXME: `clean` fails to delete files on windows.
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("assemble").build().let { result ->
            Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
            Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }

        File(project.root, "workload/src/main/java/com/example/Foo.java").delete()

        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assertions.assertEquals(TaskOutcome.NO_SOURCE, result.task(":workload:kspKotlin")?.outcome)
            Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }
    }
}
