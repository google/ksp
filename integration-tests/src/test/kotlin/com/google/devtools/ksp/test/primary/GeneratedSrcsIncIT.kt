package com.google.devtools.ksp.test.primary

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class GeneratedSrcsIncIT(experimentalPsiResolution: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "srcs-gen",
        "test-processor",
        experimentalPsiResolution = experimentalPsiResolution
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Boolean> = listOf(true, false)
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
            Assert.assertEquals(expected, outputs)
        }

        val expected2 = listOf(
            "w: [ksp] 1: [File: Bar.kt, File: Baz.kt, File: MyJavaClass.java, File: MyKotlinClass.kt]",
            "w: [ksp] 2: [File: Foo.kt, File: MyJavaClassBuilder.kt, File: MyKotlinClassBuilder.kt]",
            "w: [ksp] 3: [File: FooBar.kt, File: FooBaz.kt]"
        )

        File(project.root, "workload/src/main/kotlin/com/example/Baz.kt").appendText(System.lineSeparator())
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }.distinct()
            Assert.assertEquals(expected2, outputs)
        }
    }
}
