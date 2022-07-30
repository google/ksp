package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File

class KMPWithHmppIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("kmp-hmpp")

    @Test
    fun testCustomSourceSetHierarchyBuild() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        fun checkBuild(
            tasks: List<String> = listOf(":workload:assemble", ":workload:testClasses"),
            classToAdd: String? = null,
            checkBuildResult: (allOutput: String, kspOutput: String) -> Unit = { _, _ -> }
        ) {
            if (classToAdd != null) {
                val (sourceSetName, className) = classToAdd.split(':')
                File(project.root, "workload/src/$sourceSetName/kotlin/com/example/$className.kt").appendText(
                    """
                        package com.example
                        
                        @MyAnnotation
                        class $className {
                            val allFiles = GeneratedFor${sourceSetName.replaceFirstChar { it.uppercase() }}.allFiles
                        }
                    """.trimIndent()
                )
            }

            gradleRunner.withArguments(
                "--configuration-cache-problems=warn",
                *tasks.toTypedArray(),
            )
                // .withDebug(true)
                .build()
                .let { result ->
                    val allOutput: String = result.output
                    val kspOutput =
                        allOutput.lines().filter { it.startsWith("> Task :workload:ksp") || it.startsWith("w: [ksp] ") }
                            .joinToString("\n")

                    Assert.assertTrue("> Task :annotations:ksp" !in allOutput)
                    Assert.assertTrue("Execution optimizations have been disabled" !in allOutput)

                    checkBuildResult(allOutput, kspOutput)
                }
        }

        checkBuild(
            listOf(
                "clean",
                ":workload:run",
                ":workload:jsNodeDevelopmentRun",
                ":workload:jvmTest",
                ":workload:jsNodeTest"
            )
        ) { allOutput, kspOutput ->
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
                    w: [ksp] all files: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt, jvmMain:JvmMainAnnotated.kt, jvmMain:Main.kt]
                    w: [ksp] new files: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt, jvmMain:JvmMainAnnotated.kt, jvmMain:Main.kt]
                    w: [ksp] option: 'a' -> 'a_commonMain'
                    w: [ksp] option: 'b' -> 'b_global'
                    w: [ksp] option: 'c' -> 'c_commonMain'
                    w: [ksp] all files: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt, jvmMain:Generated.kt, jvmMain:JvmMainAnnotated.kt, jvmMain:Main.kt]
                    w: [ksp] new files: [jvmMain:Generated.kt]
                """,
                """
                    > Task :workload:kspKotlinJs
                    w: [ksp] all files: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt, jsMain:JsMainAnnotated.kt, jsMain:Main.kt]
                    w: [ksp] new files: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt, jsMain:JsMainAnnotated.kt, jsMain:Main.kt]
                    w: [ksp] option: 'a' -> 'a_commonMain'
                    w: [ksp] option: 'b' -> 'b_global'
                    w: [ksp] option: 'c' -> 'c_commonMain'
                    w: [ksp] all files: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt, jsMain:Generated.kt, jsMain:JsMainAnnotated.kt, jsMain:Main.kt]
                    w: [ksp] new files: [jsMain:Generated.kt]
                """,
                """
                    > Task :workload:kspTestKotlinJvm
                    w: [ksp] all files: [commonTest:CommonTestAnnotated.kt, jvmTest:JvmTest.kt, jvmTest:JvmTestAnnotated.kt]
                    w: [ksp] new files: [commonTest:CommonTestAnnotated.kt, jvmTest:JvmTest.kt, jvmTest:JvmTestAnnotated.kt]
                    w: [ksp] option: 'a' -> 'a_global'
                    w: [ksp] option: 'b' -> 'b_global'
                    w: [ksp] all files: [commonTest:CommonTestAnnotated.kt, jvmTest:Generated.kt, jvmTest:JvmTest.kt, jvmTest:JvmTestAnnotated.kt]
                    w: [ksp] new files: [jvmTest:Generated.kt]
                """,
            ).forEach {
                kspOutput.shouldContain(it)
            }
            listOf(
                """
                    > Task :workload:run
                    commonMain: [commonMain:CommonMainAnnotated.kt]
                    clientMain: [clientMain:ClientMainAnnotated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt]
                    jvmMain: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt, jvmMain:JvmMainAnnotated.kt, jvmMain:Main.kt]
                """,
                """
                    > Task :workload:jsNodeDevelopmentRun
                    commonMain: [commonMain:CommonMainAnnotated.kt]
                    clientMain: [clientMain:ClientMainAnnotated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt]
                    jsMain: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt, jsMain:JsMainAnnotated.kt, jsMain:Main.kt]
                """,
                """
                    > Task :workload:jvmTest
                    
                    JvmTest[jvm] > main()[jvm] STANDARD_OUT
                        commonMain: [commonMain:CommonMainAnnotated.kt]
                        clientMain: [clientMain:ClientMainAnnotated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt]
                        jvmMain: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt, jvmMain:JvmMainAnnotated.kt, jvmMain:Main.kt]
                        jvmTest: [commonTest:CommonTestAnnotated.kt, jvmTest:JvmTest.kt, jvmTest:JvmTestAnnotated.kt]
                """,
                """
                    > Task :workload:jsNodeTest
                    
                    JsTest.main STANDARD_OUT
                        commonMain: [commonMain:CommonMainAnnotated.kt]
                        clientMain: [clientMain:ClientMainAnnotated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt]
                        jsMain: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:Generated.kt, jsMain:JsMainAnnotated.kt, jsMain:Main.kt]
                        jsTest: (nothing)
                """,
            ).forEach {
                allOutput.shouldContain(it)
            }
        }

        checkBuild(
            listOf(":workload:run", ":workload:jsNodeDevelopmentRun", ":workload:jvmTest", ":workload:jsNodeTest"),
            classToAdd = "commonMain:CommonMainAnnotated2"
        ) { allOutput, _ ->
            listOf(
                """
                    > Task :workload:run
                    commonMain: [commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt]
                    clientMain: [clientMain:ClientMainAnnotated.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt]
                    jvmMain: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt, jvmMain:JvmMainAnnotated.kt, jvmMain:Main.kt]
                """,
                """
                    > Task :workload:jsNodeDevelopmentRun
                    commonMain: [commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt]
                    clientMain: [clientMain:ClientMainAnnotated.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt]
                    jsMain: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt, jsMain:JsMainAnnotated.kt, jsMain:Main.kt]
                """,
                """
                    > Task :workload:jvmTest
                    
                    JvmTest[jvm] > main()[jvm] STANDARD_OUT
                        commonMain: [commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt]
                        clientMain: [clientMain:ClientMainAnnotated.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt]
                        jvmMain: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt, jvmMain:JvmMainAnnotated.kt, jvmMain:Main.kt]
                        jvmTest: [commonTest:CommonTestAnnotated.kt, jvmTest:JvmTest.kt, jvmTest:JvmTestAnnotated.kt]
                """,
                """
                    > Task :workload:jsNodeTest
                    
                    JsTest.main STANDARD_OUT
                        commonMain: [commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt]
                        clientMain: [clientMain:ClientMainAnnotated.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt]
                        jsMain: [clientMain:ClientMainAnnotated.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt, jsMain:JsMainAnnotated.kt, jsMain:Main.kt]
                        jsTest: (nothing)
                """,
            ).forEach {
                allOutput.shouldContain(it)
            }
        }

        // TODO: Running ":workload:jsNodeDevelopmentRun" twice with configuration cache fails with:
        //     Could not load the value of field `values` of
        //     `org.gradle.api.internal.collections.SortedSetElementSource` bean found in field `store` of
        //     `org.gradle.api.internal.FactoryNamedDomainObjectContainer` bean found in field `compilations` of
        //     `org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget` bean found in field `target` of [...]
        checkBuild(
            listOf(
                ":workload:run",
                /* ":workload:jsNodeDevelopmentRun", */
                ":workload:jvmTest",
                ":workload:jsNodeTest"
            ),
            classToAdd = "clientMain:ClientMainAnnotated2"
        ) { allOutput, _ ->
            listOf(
                """
                    > Task :workload:kspCommonMainKotlinMetadata UP-TO-DATE
                """,
                """
                    > Task :workload:run
                    commonMain: [commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt]
                    clientMain: [clientMain:ClientMainAnnotated.kt, clientMain:ClientMainAnnotated2.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt]
                    jvmMain: [clientMain:ClientMainAnnotated.kt, clientMain:ClientMainAnnotated2.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt, jvmMain:JvmMainAnnotated.kt, jvmMain:Main.kt]
                """,
/*
                """
                    > Task :workload:jsNodeDevelopmentRun
                    commonMain: [commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt]
                    clientMain: [clientMain:ClientMainAnnotated.kt, clientMain:ClientMainAnnotated2.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt]
                    jsMain: [clientMain:ClientMainAnnotated.kt, clientMain:ClientMainAnnotated2.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt, jsMain:JsMainAnnotated.kt, jsMain:Main.kt]
                """,
*/
                """
                    > Task :workload:jvmTest
                    
                    JvmTest[jvm] > main()[jvm] STANDARD_OUT
                        commonMain: [commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt]
                        clientMain: [clientMain:ClientMainAnnotated.kt, clientMain:ClientMainAnnotated2.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt]
                        jvmMain: [clientMain:ClientMainAnnotated.kt, clientMain:ClientMainAnnotated2.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt, jvmMain:JvmMainAnnotated.kt, jvmMain:Main.kt]
                        jvmTest: [commonTest:CommonTestAnnotated.kt, jvmTest:JvmTest.kt, jvmTest:JvmTestAnnotated.kt]
                """,
                """
                    > Task :workload:jsNodeTest
                    
                    JsTest.main STANDARD_OUT
                        commonMain: [commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt]
                        clientMain: [clientMain:ClientMainAnnotated.kt, clientMain:ClientMainAnnotated2.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt]
                        jsMain: [clientMain:ClientMainAnnotated.kt, clientMain:ClientMainAnnotated2.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt, jsMain:JsMainAnnotated.kt, jsMain:Main.kt]
                        jsTest: (nothing)
                """,
            ).forEach {
                allOutput.shouldContain(it)
            }
        }

        checkBuild(
            listOf(
                ":workload:run",
                /* ":workload:jsNodeDevelopmentRun", */
                ":workload:jvmTest",
                ":workload:jsNodeTest"
            ),
            classToAdd = "jvmMain:JvmMainAnnotated2"
        ) { allOutput, _ ->
            listOf(
                """
                    > Task :workload:kspCommonMainKotlinMetadata UP-TO-DATE
                """,
                """
                    > Task :workload:kspClientMainKotlinMetadata UP-TO-DATE
                """,
                """
                    > Task :workload:kspKotlinJs UP-TO-DATE
                """,
                """
                    > Task :workload:run
                    commonMain: [commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt]
                    clientMain: [clientMain:ClientMainAnnotated.kt, clientMain:ClientMainAnnotated2.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt]
                    jvmMain: [clientMain:ClientMainAnnotated.kt, clientMain:ClientMainAnnotated2.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt, jvmMain:JvmMainAnnotated.kt, jvmMain:JvmMainAnnotated2.kt, jvmMain:Main.kt]
                """,
/*
                """
                    > Task :workload:jsNodeDevelopmentRun
                    commonMain: [commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt]
                    clientMain: [clientMain:ClientMainAnnotated.kt, clientMain:ClientMainAnnotated2.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt]
                    jsMain: [clientMain:ClientMainAnnotated.kt, clientMain:ClientMainAnnotated2.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt, jsMain:JsMainAnnotated.kt, jsMain:Main.kt]
                """,
*/
                """
                    > Task :workload:jvmTest
                    
                    JvmTest[jvm] > main()[jvm] STANDARD_OUT
                        commonMain: [commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt]
                        clientMain: [clientMain:ClientMainAnnotated.kt, clientMain:ClientMainAnnotated2.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt]
                        jvmMain: [clientMain:ClientMainAnnotated.kt, clientMain:ClientMainAnnotated2.kt, clientMain:Generated.kt, commonMain:CommonMainAnnotated.kt, commonMain:CommonMainAnnotated2.kt, commonMain:Generated.kt, jvmMain:JvmMainAnnotated.kt, jvmMain:JvmMainAnnotated2.kt, jvmMain:Main.kt]
                        jvmTest: [commonTest:CommonTestAnnotated.kt, jvmTest:JvmTest.kt, jvmTest:JvmTestAnnotated.kt]
                """,
                """
                    > Task :workload:jsNodeTest UP-TO-DATE
                """,
            ).forEach {
                allOutput.shouldContain(it)
            }
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
                val allOutput: String = result.output
                val kspOutput =
                    allOutput.lines()
                        .mapNotNull { if (it.startsWith("[showMe] ")) it.substringAfter("[showMe] ") else null }
                        .joinToString("\n")

                kspOutput.shouldContain(
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
                        
                        * `compileClientMainKotlinMetadata` depends on []
                        * `compileCommonMainKotlinMetadata` depends on []
                        * `compileDevelopmentExecutableKotlinJs` depends on [`compileKotlinJs`]
                        * `compileJava` depends on []
                        * `compileKotlinJs` depends on []
                        * `compileKotlinJvm` depends on []
                        * `compileKotlinMetadata` depends on []
                        * `compileProductionExecutableKotlinJs` depends on [`compileKotlinJs`]
                        * `compileTestDevelopmentExecutableKotlinJs` depends on [`compileTestKotlinJs`]
                        * `compileTestJava` depends on []
                        * `compileTestKotlinJs` depends on []
                        * `compileTestKotlinJvm` depends on []
                        * `compileTestProductionExecutableKotlinJs` depends on [`compileTestKotlinJs`]
                        * `kspClientMainKotlinMetadata` depends on [kspClientMainKotlinMetadataProcessorClasspath]
                        * `kspCommonMainKotlinMetadata` depends on [kspCommonMainKotlinMetadataProcessorClasspath]
                        * `kspKotlinJs` depends on [kspKotlinJsProcessorClasspath]
                        * `kspKotlinJvm` depends on [kspKotlinJvmProcessorClasspath]
                        * `kspTestKotlinJvm` depends on [kspTestKotlinJvmProcessorClasspath]

                    """
                )
            }
    }
}

fun String.shouldContain(expectedRawContent: String) {
    val expectedContent = expectedRawContent.trimIndent()
    assert(expectedContent in this) {
        "Missing expected content:\n$expectedContent\n\nIn output:\n$this"
    }
}
