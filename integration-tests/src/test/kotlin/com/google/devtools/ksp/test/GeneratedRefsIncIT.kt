package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File

class GeneratedRefsIncIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("refs-gen", "test-processor")

    @Test
    fun testGeneratedRefsInc() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val expected = listOf(
            "w: [ksp] 1: [File: Bar.kt, File: Baz.kt]",
            "w: [ksp] 2: [File: Foo.kt]",
            "w: [ksp] 3: [File: Goo.kt]"
        )

        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.split("\n").filter { it.startsWith("w: [ksp]")}
            Assert.assertEquals(outputs, expected)
        }
        File(project.root, "workload/src/main/kotlin/com/example/Baz.kt").appendText("\n\n")
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.split("\n").filter { it.startsWith("w: [ksp]")}
            Assert.assertEquals(outputs, expected)
        }
    }
}
