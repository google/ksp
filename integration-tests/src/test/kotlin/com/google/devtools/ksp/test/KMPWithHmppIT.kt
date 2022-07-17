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

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload:assemble",
            ":workload:testClasses",
        )
            // .withDebug(true)
            .build()
            .let { result ->
                val output: String = result.output
                val relevantOutput =
                    output.lines().filter { it.startsWith("> Task :workload:ksp") || it.startsWith("w: [ksp] ") }
                        .joinToString("\n")

                listOf(
                    """
                        > Task :workload:kspCommonMainKotlinMetadata
                        w: [ksp] all files: [commonMain:CommonMainAnnotated.kt]
                        w: [ksp] new files: [commonMain:CommonMainAnnotated.kt]
                        w: [ksp] option: 'a' -> 'a_commonMain'
                        w: [ksp] option: 'b' -> 'b_global'
                        w: [ksp] option: 'c' -> 'c_commonMain'
                        w: [ksp] all files: [commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt]
                        w: [ksp] new files: [commonMain:Generated.kt]
                        > Task :workload:kspClientMainKotlinMetadata
                        w: [ksp] all files: [clientMain:ClientMainAnnotated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt]
                        w: [ksp] new files: [clientMain:ClientMainAnnotated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt]
                        w: [ksp] option: 'a' -> 'a_clientMain'
                        w: [ksp] option: 'b' -> 'b_global'
                        w: [ksp] option: 'c' -> 'c_commonMain'
                        w: [ksp] option: 'd' -> 'd_clientMain'
                        w: [ksp] all files: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt]
                        w: [ksp] new files: [clientMain:Generated.kt]
                    """,
                    """
                        > Task :workload:kspKotlinJvm
                        w: [ksp] all files: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt, jvmMain:JvmMainAnnotated.kt]
                        w: [ksp] new files: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt, jvmMain:JvmMainAnnotated.kt]
                        w: [ksp] option: 'a' -> 'a_commonMain'
                        w: [ksp] option: 'b' -> 'b_global'
                        w: [ksp] option: 'c' -> 'c_commonMain'
                        w: [ksp] all files: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt, jvmMain:Generated.kt, jvmMain:JvmMainAnnotated.kt]
                        w: [ksp] new files: [jvmMain:Generated.kt]
                    """,
                    """
                        > Task :workload:kspKotlinJs
                        w: [ksp] all files: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt, jsMain:JsMainAnnotated.kt]
                        w: [ksp] new files: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt, jsMain:JsMainAnnotated.kt]
                        w: [ksp] option: 'a' -> 'a_commonMain'
                        w: [ksp] option: 'b' -> 'b_global'
                        w: [ksp] option: 'c' -> 'c_commonMain'
                        w: [ksp] all files: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt, jsMain:Generated.kt, jsMain:JsMainAnnotated.kt]
                        w: [ksp] new files: [jsMain:Generated.kt]
                    """,
                    """
                        > Task :workload:kspTestKotlinJvm
                        w: [ksp] all files: [commonTest:CommonTestAnnotated.kt, jvmTest:JvmTestAnnotated.kt]
                        w: [ksp] new files: [commonTest:CommonTestAnnotated.kt, jvmTest:JvmTestAnnotated.kt]
                        w: [ksp] option: 'a' -> 'a_global'
                        w: [ksp] option: 'b' -> 'b_global'
                        w: [ksp] all files: [commonTest:CommonTestAnnotated.kt, jvmTest:Generated.kt, jvmTest:JvmTestAnnotated.kt]
                        w: [ksp] new files: [jvmTest:Generated.kt]
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

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload:showMe",
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
                        
                        * `compileClientMainKotlinMetadata` depends on [kspClientMainKotlinMetadata]
                        * `compileCommonMainKotlinMetadata` depends on [kspCommonMainKotlinMetadata]
                        * `compileJava` depends on []
                        * `compileKotlinJs` depends on [kspKotlinJs]
                        * `compileKotlinJvm` depends on [kspKotlinJvm]
                        * `compileKotlinMetadata` depends on []
                        * `compileTestDevelopmentExecutableKotlinJs` depends on [`compileTestKotlinJs`]
                        * `compileTestJava` depends on []
                        * `compileTestKotlinJs` depends on []
                        * `compileTestKotlinJvm` depends on [kspTestKotlinJvm]
                        * `compileTestProductionExecutableKotlinJs` depends on [`compileTestKotlinJs`]
                        * `kspClientMainKotlinMetadata` depends on [kspClientMainKotlinMetadataProcessorClasspath]
                        * `kspCommonMainKotlinMetadata` depends on [kspCommonMainKotlinMetadataProcessorClasspath]
                        * `kspKotlinJs` depends on [kspKotlinJsProcessorClasspath]
                        * `kspKotlinJvm` depends on [kspKotlinJvmProcessorClasspath]
                        * `kspTestKotlinJvm` depends on [kspTestKotlinJvmProcessorClasspath]

                    """.trimIndent() in relevantOutput
                )
            }
    }
}
