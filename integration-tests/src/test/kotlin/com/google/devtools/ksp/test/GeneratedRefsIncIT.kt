package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GeneratedRefsIncIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "refs-gen", "test-processor")
        project.setup()
    }

    @Test
    fun testGeneratedRefsInc() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val expected = listOf(
            "w: [ksp] 1: [File: Bar.kt, File: Baz.kt]",
            "w: [ksp] 2: [File: Foo.kt]",
            "w: [ksp] 3: [File: Goo.kt]"
        )

        val expectedBar = listOf(
            "w: [ksp] 1: [File: Bar.kt]",
            "w: [ksp] 2: [File: Foo.kt]",
            "w: [ksp] 3: [File: Goo.kt]"
        )

        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assertions.assertEquals(expected, outputs)
        }

        File(project.root, "workload/src/main/kotlin/com/example/Baz.kt").appendText("\n\n")
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assertions.assertEquals(expected, outputs)
        }

        // Baz doesn't depend on Bar, so touching Bar won't invalidate Baz.
        File(project.root, "workload/src/main/kotlin/com/example/Bar.kt").appendText("\n\n")
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assertions.assertEquals(expectedBar, outputs)
        }

        // Make Baz depends on Bar; Bar will be invalidated.
        File(project.root, "workload/src/main/kotlin/com/example/Bar.kt").appendText("\nclass List<T>\n")
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assertions.assertEquals(expected, outputs)
        }

        // Baz depended on Bar, so Baz should be invalidated.
        project.restore("workload/src/main/kotlin/com/example/Bar.kt")
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assertions.assertEquals(expected, outputs)
        }

        // Baz doesn't depend on Bar, so touching Bar won't invalidate Baz.
        File(project.root, "workload/src/main/kotlin/com/example/Bar.kt").appendText("\n\n")
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assertions.assertEquals(expectedBar, outputs)
        }
    }
}
