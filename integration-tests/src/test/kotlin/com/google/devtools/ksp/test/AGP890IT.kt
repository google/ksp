package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File

class AGP890IT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground-android-multi", "playground")

    @Test
    fun testRunsKSP() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("8.11.1")

        File(project.root, "gradle.properties").appendText("\nagpVersion=8.9.0")
        gradleRunner.withArguments(":workload:compileDebugKotlin").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspDebugKotlin")?.outcome)
        }
    }

    /**
     * Similar to ProcessorClasspathConfigurationsTest.testConfigurationsForAndroidApp(), but we want to test with AGP
     * version < 8.10.0 too because we use AGP's legacy Variant API in that case.
     */
    @Test
    fun testConfigurationsForAndroidApp() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("8.11.1")

        File(project.root, "gradle.properties").appendText("\nagpVersion=8.9.0")
        File(project.root, "workload/build.gradle.kts").appendText(
            """
                android {
                    flavorDimensions += listOf("tier", "region")
                    productFlavors {
                        create("free") {
                            dimension = "tier"
                        }
                        create("premium") {
                            dimension = "tier"
                        }
                        create("us") {
                            dimension = "region"
                        }
                        create("eu") {
                            dimension = "region"
                        }
                    }
                }
                configurations.matching { it.name.startsWith("ksp") && !it.name.endsWith("ProcessorClasspath") }.all {
                    // Make sure ksp configs are not empty.
                    project.dependencies.add(name, "androidx.room:room-compiler:2.4.2")
                }
                tasks.register("testConfigurations") {
                    // Resolve all tasks to trigger classpath config creation
                    dependsOn(tasks["tasks"])
                    doLast {
                        val freeUsDebugConfig = configurations["kspFreeUsDebugKotlinProcessorClasspath"]
                        val testFreeUsDebugConfig = configurations["kspFreeUsDebugUnitTestKotlinProcessorClasspath"]
                        val androidTestFreeUsDebugConfig =
                            configurations["kspFreeUsDebugAndroidTestKotlinProcessorClasspath"]
                        val freeUsDebugParentConfigs =
                            setOf(
                                "ksp",
                                "kspDebug",
                                "kspFree",
                                "kspUs",
                                "kspFreeUs",
                                "kspFreeUsDebug"
                            )
                        val testFreeUsDebugParentConfigs =
                            setOf(
                                "ksp",
                                "kspTest",
                                "kspTestDebug",
                                "kspTestFree",
                                "kspTestUs",
                                "kspTestFreeUs",
                                "kspTestFreeUsDebug"
                            )
                        val androidTestFreeUsDebugParentConfigs =
                            setOf(
                                "ksp",
                                "kspAndroidTest",
                                "kspAndroidTestDebug",
                                "kspAndroidTestFree",
                                "kspAndroidTestUs",
                                "kspAndroidTestFreeUs",
                                "kspAndroidTestFreeUsDebug"
                            )
                        require(freeUsDebugConfig.extendsFrom.map { it.name }.toSet() == freeUsDebugParentConfigs)
                        require(
                            testFreeUsDebugConfig.extendsFrom.map { it.name }.toSet() == testFreeUsDebugParentConfigs
                        )
                        require(
                            androidTestFreeUsDebugConfig.extendsFrom.map { it.name }.toSet() == androidTestFreeUsDebugParentConfigs
                        )
                    }
                }
            """.trimIndent()
        )

        gradleRunner.withArguments(":workload:testConfigurations", "--no-configuration-cache").build()
    }
}
