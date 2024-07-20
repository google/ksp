package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class GetSealedSubclassesIncIT(val useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("sealed-subclasses", "test-processor", useKSP2)

    @Test
    fun testGetSealedSubclassesInc() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val expected2 = listOf(
            "w: [ksp] Processing Impl1.kt",
            "w: [ksp] Impl1 : []",
            "w: [ksp] Processing Impl2.kt",
            "w: [ksp] Impl2 : []",
            "w: [ksp] Processing Sealed.kt",
            "w: [ksp] Sealed : [Impl1, Impl2]",
        )

        val expected3 = listOf(
            "w: [ksp] Processing Impl1.kt",
            "w: [ksp] Impl1 : []",
            "w: [ksp] Processing Impl2.kt",
            "w: [ksp] Impl2 : []",
            "w: [ksp] Processing Impl3.kt",
            "w: [ksp] Impl3 : []",
            "w: [ksp] Processing Sealed.kt",
            "w: [ksp] Sealed : [Impl1, Impl2, Impl3]",
        )

        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assert.assertEquals(expected2, outputs)
        }

        File(project.root, "workload/src/main/kotlin/com/example/Impl3.kt").appendText("package com.example\n\n")
        File(project.root, "workload/src/main/kotlin/com/example/Impl3.kt").appendText("class Impl3 : Sealed()\n")
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assert.assertEquals(expected3, outputs)
        }

        File(project.root, "workload/src/main/kotlin/com/example/Impl3.kt").delete()
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assert.assertEquals(expected2, outputs)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
