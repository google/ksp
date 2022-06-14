package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class KMPWithHmppIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("kmp-hmpp")

    @Test
    fun testCustomSourceSetHierarchyBuild() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val subprojectName = "workload"

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":$subprojectName:assemble",
            ":$subprojectName:testClasses",
        )
            .build()
            .let { result ->
                val output: String = result.output
                val relevantOutput =
                    output.lines().filter { it.startsWith("> Task :$subprojectName:ksp") || it.startsWith("w: [ksp] ") }
                        .joinToString("\n")

                listOf(
                    """
                        > Task :$subprojectName:kspCommonMainKotlinMetadata
                        w: [ksp] current file: CommonMainAnnotated.kt
                        w: [ksp] all files: [CommonMainAnnotated.kt]
                        w: [ksp] option: 'a' -> 'a_commonMain'
                        w: [ksp] option: 'b' -> 'b_global'
                        w: [ksp] option: 'c' -> 'c_commonMain'
                        > Task :$subprojectName:kspClientMainKotlinMetadata
                        w: [ksp] current file: ClientMainAnnotated.kt
                        w: [ksp] all files: [ClientMainAnnotated.kt]
                        w: [ksp] option: 'a' -> 'a_clientMain'
                        w: [ksp] option: 'b' -> 'b_global'
                        w: [ksp] option: 'c' -> 'c_commonMain'
                        w: [ksp] option: 'd' -> 'd_clientMain'
                    """,
                    """
                        > Task :$subprojectName:kspKotlinJvm
                        w: [ksp] current file: JvmMainAnnotated.kt
                        w: [ksp] all files: [ClientMainAnnotated.kt, ClientMainAnnotatedGenerated.kt, CommonMainAnnotated.kt, CommonMainAnnotatedGenerated.kt, JvmMainAnnotated.kt]
                        w: [ksp] option: 'a' -> 'a_commonMain'
                        w: [ksp] option: 'b' -> 'b_global'
                        w: [ksp] option: 'c' -> 'c_commonMain'
                    """,
                    """
                        > Task :$subprojectName:kspKotlinJs
                        w: [ksp] current file: JsMainAnnotated.kt
                        w: [ksp] all files: [ClientMainAnnotated.kt, ClientMainAnnotatedGenerated.kt, CommonMainAnnotated.kt, CommonMainAnnotatedGenerated.kt, JsMainAnnotated.kt]
                        w: [ksp] option: 'a' -> 'a_commonMain'
                        w: [ksp] option: 'b' -> 'b_global'
                        w: [ksp] option: 'c' -> 'c_commonMain'
                    """,
                    """
                        > Task :$subprojectName:kspTestKotlinJvm
                        w: [ksp] current file: JvmTestAnnotated.kt
                        w: [ksp] all files: [CommonTestAnnotated.kt, JvmTestAnnotated.kt]
                        w: [ksp] option: 'a' -> 'a_global'
                        w: [ksp] option: 'b' -> 'b_global'
                    """,
                ).forEach {
                    Assert.assertTrue(it.trimIndent() in relevantOutput)
                }

                Assert.assertTrue("> Task :annotations:ksp" !in output)
                Assert.assertTrue("Execution optimizations have been disabled" !in output)
            }
    }

    @Test
    fun testCustomSourceSetHierarchyDependencies() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val subprojectName = "workload"

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":$subprojectName:showMe",
        )
            .build()
            .let { result ->
                val output: String = result.output
                val relevantOutput =
                    output.lines()
                        .mapNotNull { if (it.startsWith("[showMe] ")) it.substringAfter("[showMe] ") else null }
                        .joinToString("\n")

                Assert.assertTrue(
                    """

                        Kotlin targets/compilations/allKotlinSourceSets:
                        
                        * target `js`
                          * compilation `main`, default sourceSet: `jsMain`
                            * sourceSet `jsMain`, depends on `clientMain`, `commonMain`
                            * sourceSet `commonMain`
                            * sourceSet `clientMain`, depends on `commonMain`
                          * compilation `test`, default sourceSet: `jsTest`
                            * sourceSet `jsTest`, depends on `commonTest`
                            * sourceSet `commonTest`
                        * target `jvm`
                          * compilation `main`, default sourceSet: `jvmMain`
                            * sourceSet `jvmMain`, depends on `clientMain`, `commonMain`
                            * sourceSet `commonMain`
                            * sourceSet `clientMain`, depends on `commonMain`
                          * compilation `test`, default sourceSet: `jvmTest`
                            * sourceSet `jvmTest`, depends on `commonTest`
                            * sourceSet `commonTest`
                        * target `metadata`
                          * compilation `clientMain` [common], default sourceSet: `clientMain`
                          * compilation `commonMain` [common], default sourceSet: `commonMain`
                          * compilation `main` [common], default sourceSet: `commonMain`
                            * sourceSet `commonMain`
                        
                        Kotlin targets/compilations/bottomUpSourceSets:
                        
                        * target `js`
                          * compilation `main`, ordered source sets: `jsMain`, `commonMain`, `clientMain`, `commonMain`
                          * compilation `test`, ordered source sets: `jsTest`, `commonTest`
                        * target `jvm`
                          * compilation `main`, ordered source sets: `jvmMain`, `commonMain`, `clientMain`, `commonMain`
                          * compilation `test`, ordered source sets: `jvmTest`, `commonTest`
                        * target `metadata`
                          * compilation `clientMain` [common], ordered source sets: `clientMain`, `commonMain`
                          * compilation `commonMain` [common], ordered source sets: `commonMain`
                          * compilation `main` [common], ordered source sets: `commonMain`
                        
                        KSP configurations:
                        
                        * `ksp`, artifacts: [], dependencies: []
                        * `kspCommonMainMetadata`, artifacts: [], dependencies: [test-processor]
                        * `kspJs`, artifacts: [], dependencies: [test-processor]
                        * `kspJsTest`, artifacts: [], dependencies: []
                        * `kspJvm`, artifacts: [], dependencies: [test-processor]
                        * `kspJvmTest`, artifacts: [], dependencies: [test-processor]
                        * `kspMetadataClientMain`, artifacts: [], dependencies: [test-processor]
                        
                        Tasks [compile, ksp] and their ksp/compile dependencies:
                        
                        * `compileClientMainKotlinMetadata` depends on [kspClientMainKotlinMetadata, kspCommonMainKotlinMetadata]
                        * `compileCommonMainKotlinMetadata` depends on [kspCommonMainKotlinMetadata]
                        * `compileJava` depends on []
                        * `compileKotlinJs` depends on [kspClientMainKotlinMetadata, kspCommonMainKotlinMetadata, kspKotlinJs]
                        * `compileKotlinJvm` depends on [kspClientMainKotlinMetadata, kspCommonMainKotlinMetadata, kspKotlinJvm]
                        * `compileKotlinMetadata` depends on []
                        * `compileTestDevelopmentExecutableKotlinJs` depends on [`compileTestKotlinJs`]
                        * `compileTestJava` depends on []
                        * `compileTestKotlinJs` depends on []
                        * `compileTestKotlinJvm` depends on [kspTestKotlinJvm]
                        * `compileTestProductionExecutableKotlinJs` depends on [`compileTestKotlinJs`]
                        * `kspClientMainKotlinMetadata` depends on [kspClientMainKotlinMetadataProcessorClasspath, kspCommonMainKotlinMetadata]
                        * `kspCommonMainKotlinMetadata` depends on [kspCommonMainKotlinMetadataProcessorClasspath]
                        * `kspKotlinJs` depends on [kspClientMainKotlinMetadata, kspCommonMainKotlinMetadata, kspKotlinJsProcessorClasspath]
                        * `kspKotlinJvm` depends on [kspClientMainKotlinMetadata, kspCommonMainKotlinMetadata, kspKotlinJvmProcessorClasspath]
                        * `kspTestKotlinJvm` depends on [kspTestKotlinJvmProcessorClasspath]

                    """.trimIndent() in relevantOutput
                )
            }
    }
}
