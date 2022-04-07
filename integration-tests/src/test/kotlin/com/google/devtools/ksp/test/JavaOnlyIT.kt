package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import java.io.File

class JavaOnlyIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("java-only", "test-processor")

    @Test
    fun testJavaOnly() {
        // FIXME: `clean` fails to delete files on windows.
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }

        File(project.root, "workload/src/main/java/com/example/Foo.java").delete()

        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.NO_SOURCE, result.task(":workload:kspKotlin")?.outcome)
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }
    }
}
