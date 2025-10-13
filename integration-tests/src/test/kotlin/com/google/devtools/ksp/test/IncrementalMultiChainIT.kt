package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File

class IncrementalMultiChainIT() {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("incremental-multi-chain")

    @Test
    fun testMultiChain() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val k2 = File(project.root, "workload/src/main/kotlin/K2.kt")

        gradleRunner.withArguments("run").build().let { result ->
            Assert.assertTrue(result.output.contains("validating K1.kt"))
            Assert.assertTrue(result.output.contains("validating Main.kt"))
            Assert.assertTrue(result.output.contains("validating K1Impl.kt"))
            Assert.assertTrue(result.output.contains("validating AllImpls.kt"))
            Assert.assertTrue(result.output.contains("[K1Impl]"))
        }

        k2.writeText(
            "@NeedsImpl\ninterface K2\n"
        )
        gradleRunner.withArguments("run").build().let { result ->
            Assert.assertTrue(result.output.contains("validating K1.kt"))
            Assert.assertTrue(result.output.contains("validating K2.kt"))
            Assert.assertTrue(result.output.contains("validating Main.kt"))
            Assert.assertTrue(result.output.contains("validating K1Impl.kt"))
            Assert.assertTrue(result.output.contains("validating K2Impl.kt"))
            Assert.assertTrue(result.output.contains("validating AllImpls.kt"))
            Assert.assertTrue(result.output.contains("[K1Impl, K2Impl]"))
        }

        k2.delete()
        gradleRunner.withArguments("run").build().let { result ->
            Assert.assertTrue(result.output.contains("validating K1.kt"))
            Assert.assertTrue(result.output.contains("validating Main.kt"))
            Assert.assertTrue(result.output.contains("validating K1Impl.kt"))
            Assert.assertTrue(result.output.contains("validating AllImpls.kt"))
            Assert.assertTrue(result.output.contains("[K1Impl]"))
            Assert.assertFalse(
                File(project.root, "workload/build/generated/ksp/main/kotlin/K2ImplInfo.kt").exists()
            )
        }
    }
}
