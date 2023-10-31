package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class GeneratedRefsIncIT(val useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("refs-gen", "test-processor", useKSP2)

    @Test
    fun testGeneratedRefsInc() {
        Assume.assumeFalse(useKSP2)
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
            Assert.assertEquals(expected, outputs)
        }

        File(project.root, "workload/src/main/kotlin/com/example/Baz.kt").appendText("\n\n")
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assert.assertEquals(expected, outputs)
        }

        // Baz doesn't depend on Bar, so touching Bar won't invalidate Baz.
        File(project.root, "workload/src/main/kotlin/com/example/Bar.kt").appendText("\n\n")
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assert.assertEquals(expectedBar, outputs)
        }

        // Make Baz depends on Bar; Bar will be invalidated.
        File(project.root, "workload/src/main/kotlin/com/example/Bar.kt").appendText("\nclass List<T>\n")
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assert.assertEquals(expected, outputs)
        }

        // Baz depended on Bar, so Baz should be invalidated.
        project.restore("workload/src/main/kotlin/com/example/Bar.kt")
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assert.assertEquals(expected, outputs)
        }

        // Baz doesn't depend on Bar, so touching Bar won't invalidate Baz.
        File(project.root, "workload/src/main/kotlin/com/example/Bar.kt").appendText("\n\n")
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assert.assertEquals(expectedBar, outputs)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
