package com.google.devtools.ksp.test.secondary

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class HmppIT(experimentalPsiResolution: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "hmpp",
        experimentalPsiResolution = experimentalPsiResolution
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Boolean> = listOf(true, false)
    }

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

    // With ksp.experimental.metadata.own.sources.only, intermediate metadata tasks process only
    // their own source set; upstream stays resolvable via its klib. Platform tasks are unchanged.
    val taskToFilesOwnSourcesOnly = mapOf(
        ":workload:kspCommonMainKotlinMetadata" to "w: [ksp] EchoProcessor: CommonMain",
        ":workload:kspJvmJsKotlinMetadata" to "w: [ksp] EchoProcessor: JvmJs",
        ":workload:kspJvmLinuxX64KotlinMetadata" to "w: [ksp] EchoProcessor: JvmLinuxX64",
        ":workload:kspKotlinJvm" to "w: [ksp] EchoProcessor: CommonMain_JvmJs_JvmLinuxX64_JvmMain_JvmOnly",
        ":workload:kspKotlinJs" to "w: [ksp] EchoProcessor: CommonMain_JsMain_JvmJs",
        ":workload:kspKotlinLinuxX64" to "w: [ksp] EchoProcessor: CommonMain_JvmLinuxX64_LinuxX64Main",
    )

    @Test
    fun testOwnSourcesOnly() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        taskToFilesOwnSourcesOnly.forEach { (task, expected) ->
            gradleRunner.withArguments(
                "--configuration-cache-problems=warn",
                "-Pksp.experimental.metadata.own.sources.only=true",
                task,
            ).build().let { result ->
                val logs = result.output.lines().filter { it.startsWith("w: [ksp] EchoProcessor: ") }.toSet()
                Assert.assertTrue("$task: expected '$expected' in $logs", expected in logs)
            }
        }
    }
}
