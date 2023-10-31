package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class GeneratedSrcsIncIT(useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("srcs-gen", "test-processor", useKSP2)

    @Test
    fun testGeneratedSrcsInc() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val expected = listOf(
            "w: [ksp] 1: [File: Bar.kt, File: Baz.kt]",
            "w: [ksp] 2: [File: Foo.kt]",
            "w: [ksp] 3: [File: FooBar.kt, File: FooBaz.kt]"
        )

        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assert.assertEquals(expected, outputs)
        }
        File(project.root, "workload/src/main/kotlin/com/example/Baz.kt").appendText(System.lineSeparator())
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assert.assertEquals(expected, outputs)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
