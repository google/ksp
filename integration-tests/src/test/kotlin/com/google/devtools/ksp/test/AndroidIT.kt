package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import com.google.devtools.ksp.test.utils.assertMergedConfigurationOutput
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class AndroidIT() {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground-android", "playground")

    @Test
    fun testPlaygroundAndroid() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "clean", "build", "minifyReleaseWithR8", "--configuration-cache", "--info", "--stacktrace"
        ).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:build")?.outcome)
            assertMergedConfigurationOutput(project, "-keep class com.example.AClassBuilder { *; }")
            val outputs = result.output.lines()
            assert("w: [ksp] [workload_debug] Mangled name for internalFun: internalFun\$workload_debug" in outputs)
            assert("w: [ksp] [workload_release] Mangled name for internalFun: internalFun\$workload_release" in outputs)
        }
    }
}
