package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class HmppIT() {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("hmpp")

    val taskToFilesTraditional = mapOf(
        ":workload:kspCommonMainKotlinMetadata" to "w: [ksp] EchoProcessor: CommonMain",
        ":workload:kspJvmJsKotlinMetadata" to "w: [ksp] EchoProcessor: CommonMain_JvmJs",
        ":workload:kspJvmLinuxX64KotlinMetadata" to "w: [ksp] EchoProcessor: CommonMain_JvmLinuxX64",
        ":workload:kspKotlinJvm" to "w: [ksp] EchoProcessor: CommonMain_JvmJs_JvmLinuxX64_JvmMain_JvmOnly",
        ":workload:kspKotlinJs" to "w: [ksp] EchoProcessor: CommonMain_JsMain_JvmJs",
        ":workload:kspKotlinLinuxX64" to "w: [ksp] EchoProcessor: CommonMain_JvmLinuxX64_LinuxX64Main",
    )

    @Test
    fun testTraditional() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        taskToFilesTraditional.forEach { (task, expected) ->
            gradleRunner.withArguments(
                "--configuration-cache-problems=warn",
                task,
            ).build().let { result ->
                val logs = result.output.lines().filter { it.startsWith("w: [ksp] EchoProcessor: ") }.toSet()
                Assert.assertTrue(expected in logs)
            }
        }
    }

    val taskToFilesHmpp = mapOf(
        ":workload:kspCommonMainKotlinMetadata" to setOf(
            "w: [ksp] EchoProcessor: CommonMain",
        ),
        ":workload:kspJvmJsKotlinMetadata" to setOf(
            "w: [ksp] EchoProcessor: CommonMain",
            "w: [ksp] EchoProcessor: (CommonMain)_JvmJs",
        ),
        ":workload:kspJvmLinuxX64KotlinMetadata" to setOf(
            "w: [ksp] EchoProcessor: CommonMain",
            "w: [ksp] EchoProcessor: (CommonMain)_JvmLinuxX64",
        ),
        ":workload:kspKotlinJvm" to setOf(
            "w: [ksp] EchoProcessor: CommonMain",
            "w: [ksp] EchoProcessor: (CommonMain)_JvmJs",
            "w: [ksp] EchoProcessor: (CommonMain)_JvmLinuxX64",
            "w: [ksp] EchoProcessor: ((CommonMain)_JvmJs)_((CommonMain)_JvmLinuxX64)_(CommonMain)_JvmMain_JvmOnly",
        ),
        ":workload:kspKotlinJs" to setOf(
            "w: [ksp] EchoProcessor: CommonMain",
            "w: [ksp] EchoProcessor: (CommonMain)_JvmJs",
            "w: [ksp] EchoProcessor: ((CommonMain)_JvmJs)_(CommonMain)_JsMain",
        ),
        ":workload:kspKotlinLinuxX64" to setOf(
            "w: [ksp] EchoProcessor: CommonMain",
            "w: [ksp] EchoProcessor: (CommonMain)_JvmLinuxX64",
            "w: [ksp] EchoProcessor: ((CommonMain)_JvmLinuxX64)_(CommonMain)_LinuxX64Main",
        ),
    )
}
