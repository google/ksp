package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GetSealedSubclassesIncIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "sealed-subclasses", "test-processor")
        project.setup()
    }

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
            Assertions.assertEquals(expected2, outputs)
        }

        File(project.root, "workload/src/main/kotlin/com/example/Impl3.kt").appendText("package com.example\n\n")
        File(project.root, "workload/src/main/kotlin/com/example/Impl3.kt").appendText("class Impl3 : Sealed()\n")
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assertions.assertEquals(expected3, outputs)
        }

        File(project.root, "workload/src/main/kotlin/com/example/Impl3.kt").delete()
        gradleRunner.withArguments("assemble").build().let { result ->
            val outputs = result.output.lines().filter { it.startsWith("w: [ksp]") }
            Assertions.assertEquals(expected2, outputs)
        }
    }
}
