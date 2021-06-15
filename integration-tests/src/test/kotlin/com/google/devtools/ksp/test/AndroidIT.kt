package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File

class AndroidIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground-android", "playground")

    @Test
    fun testPlaygroundAndroid() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "build", "minifyReleaseWithR8", "--info", "--stacktrace").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:build")?.outcome)
            val mergedConfiguration = File(project.root, "build/outputs/mapping/release/configuration.txt")
            assert(mergedConfiguration.exists()) {
                "Merged configuration file not found!"
            }
            val configurationText = mergedConfiguration.readText()
            assert("-keep class com.example.android.AClassBuilder { *; }" in configurationText) {
                "Merged configuration did not contain generated proguard rules!\n$configurationText"
            }
        }
    }
}
