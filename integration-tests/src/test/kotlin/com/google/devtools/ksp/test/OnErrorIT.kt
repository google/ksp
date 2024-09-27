package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class OnErrorIT(useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("on-error", useKSP2 = useKSP2)

    @Test
    fun testOnError() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "assemble").buildAndFail().let { result ->
            val errors = result.output.lines().filter { it.startsWith("e: [ksp]") }
            Assert.assertEquals("e: [ksp] Error processor: errored at 2", errors.first())
            Assert.assertEquals("e: [ksp] NormalProcessor called error on 2", errors.last())
        }
    }

    @Test
    fun testOnExceptionInInit() {
        File(project.root, "workload/build.gradle.kts").appendText("\nksp { arg(\"exception\", \"init\") }\n")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "assemble").buildAndFail().let { result ->
            val errors = result.output.lines().filter { it.startsWith("e: [ksp]") }
            Assert.assertEquals("e: [ksp] java.lang.Exception: Test Exception in init", errors.first())
        }
        project.restore("workload/build.gradle.kts")
    }

    @Test
    fun testOnExceptionInProcess() {
        File(project.root, "workload/build.gradle.kts").appendText("\nksp { arg(\"exception\", \"process\") }\n")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "assemble").buildAndFail().let { result ->
            val errors = result.output.lines().filter { it.startsWith("e: [ksp]") }
            Assert.assertEquals("e: [ksp] java.lang.Exception: Test Exception in process", errors.first())
        }
        project.restore("workload/build.gradle.kts")
    }

    @Test
    fun testOnExceptionInFinish() {
        File(project.root, "workload/build.gradle.kts").appendText("\nksp { arg(\"exception\", \"finish\") }\n")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "assemble").buildAndFail().let { result ->
            val errors = result.output.lines().filter { it.startsWith("e: [ksp]") }
            Assert.assertEquals("e: [ksp] java.lang.Exception: Test Exception in finish", errors.first())
        }
        project.restore("workload/build.gradle.kts")
    }

    @Test
    fun testOnExceptionInOnError() {
        File(project.root, "workload/build.gradle.kts").appendText("\nksp { arg(\"exception\", \"error\") }\n")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "assemble").buildAndFail().let { result ->
            val errors = result.output.lines().filter { it.startsWith("e: [ksp]") }

            Assert.assertEquals("e: [ksp] Error processor: errored at 2", errors.first())
            Assert.assertEquals("e: [ksp] java.lang.Exception: Test Exception in error", errors[1])
        }
        project.restore("workload/build.gradle.kts")
    }

    @Test
    fun testCreateTwice() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        File(project.root, "workload/build.gradle.kts").appendText("\nksp { arg(\"exception\", \"createTwice\") }\n")
        // Disabling configuration cache. See https://github.com/google/ksp/issues/299 for details
        gradleRunner.withArguments("clean", "assemble", "--no-configuration-cache").buildAndFail().let { result ->
            val errors = result.output.lines().filter { it.startsWith("e: [ksp]") }

            Assert.assertTrue(
                errors.any {
                    it.startsWith("e: [ksp] kotlin.io.FileAlreadyExistsException:")
                }
            )

            Assert.assertFalse(result.output.contains("e: java.lang.IllegalStateException: Should not be called!"))
        }
        project.restore("workload/build.gradle.kts")
    }

    @Test
    fun testCreateTwiceNotOkOnError() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        File(project.root, "workload/build.gradle.kts").appendText("\nksp { arg(\"exception\", \"createTwice\") }\n")
        File(project.root, "gradle.properties").appendText("\nksp.return.ok.on.error=false")
        // Disabling configuration cache. See https://github.com/google/ksp/issues/299 for details
        gradleRunner.withArguments("clean", "assemble", "--no-configuration-cache").buildAndFail().let { result ->
            val errors = result.output.lines().filter { it.startsWith("e: [ksp]") }

            Assert.assertTrue(
                errors.any {
                    it.startsWith("e: [ksp] kotlin.io.FileAlreadyExistsException:")
                }
            )
        }
        project.restore("workload/build.gradle.kts")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
