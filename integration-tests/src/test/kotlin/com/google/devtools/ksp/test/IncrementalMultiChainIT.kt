package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class IncrementalMultiChainIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "incremental-multi-chain")
        project.setup()
    }

    @Test
    fun testMultiChain() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val k2 = File(project.root, "workload/src/main/kotlin/K2.kt")

        gradleRunner.withArguments("run").build().let { result ->
            Assertions.assertTrue(result.output.contains("validating K1.kt"))
            Assertions.assertTrue(result.output.contains("validating Main.kt"))
            Assertions.assertTrue(result.output.contains("validating K1Impl.kt"))
            Assertions.assertTrue(result.output.contains("validating AllImpls.kt"))
            Assertions.assertTrue(result.output.contains("[K1Impl]"))
        }

        k2.writeText(
            "@NeedsImpl\ninterface K2\n"
        )
        gradleRunner.withArguments("run").build().let { result ->
            Assertions.assertTrue(result.output.contains("validating K1.kt"))
            Assertions.assertTrue(result.output.contains("validating K2.kt"))
            Assertions.assertTrue(result.output.contains("validating Main.kt"))
            Assertions.assertTrue(result.output.contains("validating K1Impl.kt"))
            Assertions.assertTrue(result.output.contains("validating K2Impl.kt"))
            Assertions.assertTrue(result.output.contains("validating AllImpls.kt"))
            Assertions.assertTrue(result.output.contains("[K1Impl, K2Impl]"))
        }

        k2.delete()
        gradleRunner.withArguments("run").build().let { result ->
            Assertions.assertTrue(result.output.contains("validating K1.kt"))
            Assertions.assertTrue(result.output.contains("validating Main.kt"))
            Assertions.assertTrue(result.output.contains("validating K1Impl.kt"))
            Assertions.assertTrue(result.output.contains("validating AllImpls.kt"))
            Assertions.assertTrue(result.output.contains("[K1Impl]"))
            Assertions.assertFalse(
                File(project.root, "workload/build/generated/ksp/main/kotlin/K2ImplInfo.kt").exists()
            )
        }
    }
}
