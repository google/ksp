package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GeneratedSrcsIncIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "srcs-gen", "test-processor")
        project.setup()
    }

    @Test
    fun testGeneratedSrcsInc() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val expected = listOf(
            "w: [ksp] 1: [File: A.kt, File: Bar.kt, File: Baz.kt, File: MyJavaClass.java, File: MyKotlinClass.kt]",
            "w: [ksp] 2: [File: Foo.kt, File: MyJavaClassBuilder.kt, File: MyKotlinClassBuilder.kt]",
            "w: [ksp] 3: [File: FooBar.kt, File: FooBaz.kt]"
        )

        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }.distinct()
            Assertions.assertEquals(expected, outputs)
        }

        val expected2 = listOf(
            "w: [ksp] 1: [File: Bar.kt, File: Baz.kt, File: MyJavaClass.java, File: MyKotlinClass.kt]",
            "w: [ksp] 2: [File: Foo.kt, File: MyJavaClassBuilder.kt, File: MyKotlinClassBuilder.kt]",
            "w: [ksp] 3: [File: FooBar.kt, File: FooBaz.kt]"
        )

        File(project.root, "workload/src/main/kotlin/com/example/Baz.kt").appendText(System.lineSeparator())
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }.distinct()
            Assertions.assertEquals(expected2, outputs)
        }
    }
}
