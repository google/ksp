/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.ksp.test.secondary

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class IncrementalAnnotationArgumentClassReferences(experimentalPsiResolution: Boolean) {

    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "incremental-annotation-argument-class-references",
        experimentalPsiResolution = experimentalPsiResolution
    )

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Boolean> = listOf(true, false)

        private const val TARGET: String = "downstream"
        private const val KSP_KOTLIN: String = ":$TARGET:kspKotlin"
        private const val ASSEMBLE: String = "assemble"
        private const val CLEAN: String = "clean"
        private const val PROCESSOR_LABEL: String = "[TestProcessor]"
        private const val LOADER_LABEL: String = "[KSPLoaderId]"
    }

    @Test
    fun testUpToDate() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":$TARGET:assemble")?.outcome)
        }

        gradleRunner.withArguments(ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.UP_TO_DATE, result.task(KSP_KOTLIN)?.outcome)
        }
    }

    @Test
    fun testProcessorChange() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        // 1. Clean build
        val expected = mutableListOf<String>()
        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            val processedClasses = result.output.lines().filter { it.startsWith(PROCESSOR_LABEL) }
            expected.addAll(processedClasses)
            Assert.assertTrue("Expected processed classes, but found none", expected.isNotEmpty())
        }

        // 2. Apply change to upstream class
        val fileToChange = "upstream/src/main/kotlin/UpstreamChanges.kt"
        val change = "{ val prop2 = false }"
        File(project.root, fileToChange).appendText(change)

        // 3. Rebuild
        gradleRunner.withArguments(ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            val actual = result.output.lines().filter { it.startsWith(PROCESSOR_LABEL) }
            Assert.assertEquals(
                "\n${expected.joinToString("\n")}\n[SEPARATOR]\n${actual.joinToString("\n")}",
                expected,
                actual
            )
        }
    }

    @Test
    fun testTransitiveJavaClasspathChange() {
        File(project.root, "downstream/src/main/kotlin/DownstreamClass.kt").writeText(
            """
            import com.example.upstream.ExampleReferenceJava
            import com.example.upstream.UpstreamEntryPointJava

            @ExampleReference(UpstreamEntryPoint::class)
            @ExampleReferenceJava(UpstreamEntryPointJava::class)
            class DownstreamClass
            """.trimIndent()
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val downstreamProcessed = "$PROCESSOR_LABEL Processing DownstreamClass"
        // 1. Clean build
        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            Assert.assertTrue(result.output.lines().contains(downstreamProcessed))
            Assert.assertTrue(result.output.lines().contains("$PROCESSOR_LABEL Processing UpstreamChangesJava"))
        }

        // 2. Apply an ABI change to the upstream Java class that only the classpath refers to
        val fileToChange = File(project.root, "upstream/src/main/java/com/example/upstream/UpstreamChangesJava.java")
        fileToChange.writeText(
            fileToChange.readText().replace("{", "{\n    public void addedMember() {\n    }\n")
        )

        // 3. Rebuild
        gradleRunner.withArguments(ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            Assert.assertTrue(result.output.lines().contains(downstreamProcessed))
        }
    }

    /**
     * Both `:upstream` and `:downstream` apply the same processor, so they resolve an identical
     * processor classpath. Without the cache each gets its own processor classloader; with the
     * cache enabled they must share one.
     *
     * Also asserts that enabling the cache does not change what the processor does.
     *
     * Reuse and output parity are asserted from the same pair of builds rather than split across
     * two tests: each build of this project is expensive, and sharing them costs two builds
     * instead of four. Worth splitting if this fixture ever becomes cheap to build.
     */
    @Test
    fun testProcessorClassloaderCaching() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        fun build(cacheEnabled: Boolean) = gradleRunner
            .withArguments(CLEAN, ASSEMBLE, "-Pksp.classloader.cache.processors=$cacheEnabled")
            .build()

        val off = build(false)
        val offLoaders = off.output.lines().filter { it.startsWith(LOADER_LABEL) }.toSet()
        val offProcessed = off.output.lines().filter { it.startsWith(PROCESSOR_LABEL) }
        Assert.assertEquals(TaskOutcome.SUCCESS, off.task(KSP_KOTLIN)?.outcome)
        Assert.assertEquals(
            "expected one processor classloader per module, got $offLoaders",
            2,
            offLoaders.size
        )

        val on = build(true)
        val onLoaders = on.output.lines().filter { it.startsWith(LOADER_LABEL) }.toSet()
        val onProcessed = on.output.lines().filter { it.startsWith(PROCESSOR_LABEL) }
        Assert.assertEquals(TaskOutcome.SUCCESS, on.task(KSP_KOTLIN)?.outcome)
        Assert.assertEquals(
            "expected a single shared processor classloader, got $onLoaders",
            1,
            onLoaders.size
        )

        // Caching the classloader must not change processing behaviour.
        Assert.assertEquals(offProcessed, onProcessed)
    }

    @Test
    fun testRootPackageJavaOnDemandImportShadowing() {
        project.appendProperty("ksp.incremental.log=true")
        File(project.root, "downstream/src/main/java/RootJavaConsumer.java").apply {
            parentFile.mkdirs()
        }.writeText(
            """
            import com.example.upstream.*;

            @ExampleJavaReference(UpstreamExplicitTarget.class)
            public class RootJavaConsumer {
                public UpstreamChangesJava helper() {
                    return null;
                }
            }
            """.trimIndent() + "\n"
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            Assert.assertTrue(result.output.lines().contains("$PROCESSOR_LABEL Processing RootJavaConsumer"))
        }

        File(project.root, "downstream/src/main/kotlin/UpstreamChangesJava.kt").writeText(
            "class UpstreamChangesJava\n"
        )

        gradleRunner.withArguments(ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            Assert.assertTrue(
                "Expected RootJavaConsumer to be reprocessed when root-package UpstreamChangesJava shadows * import",
                result.output.lines().contains("$PROCESSOR_LABEL Processing RootJavaConsumer")
            )
        }
    }

    @Test
    fun testBacktickedDottedKotlinFunctionIncrementalInvalidation() {
        project.appendProperty("ksp.incremental.log=true")
        File(project.root, "downstream/src/main/kotlin/DottedTarget.kt").writeText(
            """
            package com.example.downstream

            @JvmName("dottedTarget")
            fun `dotted.target`(): Int = 1
            """.trimIndent() + "\n"
        )
        File(project.root, "downstream/src/main/kotlin/DownstreamClass.kt").writeText(
            """
            @ExampleReference(UpstreamExplicitTarget::class)
            class DownstreamClass {
                fun callDotted() = com.example.downstream.`dotted.target`()
            }
            """.trimIndent() + "\n"
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(CLEAN, KSP_KOTLIN).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            Assert.assertTrue(result.output.lines().contains("$PROCESSOR_LABEL Processing DownstreamClass"))
        }

        File(project.root, "downstream/src/main/kotlin/DottedTarget.kt").writeText(
            """
            package com.example.downstream

            @JvmName("dottedTarget")
            fun `dotted.target`(): Int = 2
            """.trimIndent() + "\n"
        )

        gradleRunner.withArguments(KSP_KOTLIN).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            Assert.assertTrue(
                "Expected DownstreamClass to be reprocessed after modifying backticked `dotted.target` function",
                result.output.lines().contains("$PROCESSOR_LABEL Processing DownstreamClass")
            )
        }
    }

    @Test
    fun testGetAllMembersOnJavaLibrarySupertypeDoesNotRecordClassFiles() {
        project.appendProperty("ksp.incremental.log=true")
        project.appendProperty("callGetAllMembers=true")
        File(project.root, "upstream/src/main/java/com/example/upstream/UpstreamEntryPointJava.java").writeText(
            """
            package com.example.upstream;

            public class UpstreamEntryPointJava {
                public UpstreamChangesJava field = null;
                public UpstreamChangesJava helper() {
                    return null;
                }
            }
            """.trimIndent() + "\n"
        )
        File(project.root, "downstream/src/main/kotlin/DownstreamClass.kt").writeText(
            """
            import com.example.upstream.UpstreamEntryPointJava

            @ExampleReference(UpstreamExplicitTarget::class)
            class DownstreamClass : UpstreamEntryPointJava()
            """.trimIndent() + "\n"
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            Assert.assertTrue(result.output.lines().contains("$PROCESSOR_LABEL Processing DownstreamClass"))
        }

        val lookupLogFile = File(project.root, "downstream/build/kspCaches/main/logs/kspLookupGraph.log")
        Assert.assertTrue("Expected kspLookupGraph.log to exist at ${lookupLogFile.path}", lookupLogFile.exists())
        val classFileLines = lookupLogFile.readLines().filter { it.contains(".class ->") || it.contains(".jar!") }
        Assert.assertTrue(
            "Expected kspLookupGraph.log not to record compiled .class/.jar paths, but found:\n" +
                classFileLines.joinToString("\n"),
            classFileLines.isEmpty()
        )

        File(project.root, "upstream/src/main/java/com/example/upstream/UpstreamEntryPointJava.java").writeText(
            """
            package com.example.upstream;

            public class UpstreamEntryPointJava {
                public UpstreamChangesJava field = null;
                public UpstreamChangesJava helper() {
                    return null;
                }
                public int addedMethod() {
                    return 42;
                }
            }
            """.trimIndent() + "\n"
        )

        gradleRunner.withArguments(ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            Assert.assertTrue(
                "Expected DownstreamClass to be reprocessed after Java supertype ABI change",
                result.output.lines().contains("$PROCESSOR_LABEL Processing DownstreamClass")
            )
        }
    }

    @Test
    fun testAssociateWithJavaLibraryClassInvalidatesOutput() {
        project.appendProperty("associateJavaLibClass=StandaloneAssociatedJavaLib")
        File(project.root, "upstream/src/main/java/StandaloneAssociatedJavaLib.java").writeText(
            """
            public class StandaloneAssociatedJavaLib {
                public void v1() {}
            }
            """.trimIndent() + "\n"
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
        }

        val generatedFile = File(
            project.root,
            "downstream/build/generated/ksp/main/kotlin/DownstreamClassGenerated.kt"
        )
        Assert.assertTrue("Expected generated file at ${generatedFile.path}", generatedFile.exists())
        Assert.assertTrue(
            "Expected initial generated file to contain v1, got: ${generatedFile.readText()}",
            generatedFile.readText().contains("v1")
        )

        // Modify ONLY StandaloneAssociatedJavaLib.java in :upstream (unreferenced in DownstreamClass.kt; linked solely via associateWithClasses)
        File(project.root, "upstream/src/main/java/StandaloneAssociatedJavaLib.java").writeText(
            """
            public class StandaloneAssociatedJavaLib {
                public void v1() {}
                public void v2() {}
            }
            """.trimIndent() + "\n"
        )

        gradleRunner.withArguments(ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
        }

        Assert.assertTrue(
            "Expected DownstreamClassGenerated.kt to be regenerated with v2 when StandaloneAssociatedJavaLib changed, got: ${generatedFile.readText()}",
            generatedFile.readText().contains("v2")
        )
    }

    @Test
    fun testDependencyGraphOrigin() {
        project.appendProperty("ksp.incremental.log=true")
        project.appendProperty("ksp.incremental.log.graph.origin=DownstreamClass")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
        }

        val dotFile = File(project.root, "downstream/build/kspCaches/main/logs/kspLookupGraphOrigin.dot")
        Assert.assertTrue("Expected kspLookupGraphOrigin.dot to exist at ${dotFile.path}", dotFile.exists())
        val expectedDot =
            """
            digraph {
              "DownstreamClass";
              "DownstreamClass" -> "ExampleReference";
              "DownstreamClass" -> "UpstreamEntryPoint";
              "UpstreamEntryPoint" -> "ExampleReference";
              "UpstreamEntryPoint" -> "UpstreamChanges";
            }
            """.trimIndent()
        Assert.assertEquals(expectedDot, dotFile.readText().trim())
    }

    @Test
    fun testJavaSourceAnnotationDefaultValueDependencyGraphOrigin() {
        project.appendProperty("ksp.incremental.log=true")
        project.appendProperty("ksp.incremental.log.graph.origin=DownstreamClass")
        File(project.root, "downstream/src/main/java/JavaSourceDefaultReference.java").apply {
            parentFile.mkdirs()
        }.writeText(
            """
            public @interface JavaSourceDefaultReference {
                Class<?> value() default UpstreamEntryPoint.class;
            }
            """.trimIndent() + "\n"
        )
        File(project.root, "downstream/src/main/kotlin/DownstreamClass.kt").writeText(
            """
            @ExampleReference(UpstreamExplicitTarget::class)
            @JavaSourceDefaultReference
            class DownstreamClass
            """.trimIndent() + "\n"
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
        }

        val dotFile = File(project.root, "downstream/build/kspCaches/main/logs/kspLookupGraphOrigin.dot")
        Assert.assertTrue("Expected kspLookupGraphOrigin.dot to exist at ${dotFile.path}", dotFile.exists())
        val expectedDot =
            """
            digraph {
              "DownstreamClass";
              "DownstreamClass" -> "ExampleReference";
              "DownstreamClass" -> "JavaSourceDefaultReference";
              "DownstreamClass" -> "UpstreamEntryPoint";
              "DownstreamClass" -> "UpstreamExplicitTarget";
              "UpstreamEntryPoint" -> "ExampleReference";
              "UpstreamEntryPoint" -> "UpstreamChanges";
            }
            """.trimIndent()
        Assert.assertEquals(expectedDot, dotFile.readText().trim())
    }

    @Test
    fun testJavaSourceAnnotationDefaultValueOverriddenDefect() {
        project.appendProperty("ksp.incremental.log=true")
        project.appendProperty("ksp.incremental.log.graph.origin=DownstreamClass")
        File(project.root, "downstream/src/main/java/JavaSourceDefaultReference.java").apply {
            parentFile.mkdirs()
        }.writeText(
            """
            public @interface JavaSourceDefaultReference {
                Class<?> value() default UpstreamEntryPoint.class;
            }
            """.trimIndent() + "\n"
        )
        File(project.root, "downstream/src/main/kotlin/DownstreamClass.kt").writeText(
            """
            @ExampleReference(UpstreamExplicitTarget::class)
            @JavaSourceDefaultReference(UpstreamExplicitTarget::class)
            class DownstreamClass
            """.trimIndent() + "\n"
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
        }

        val dotFile = File(project.root, "downstream/build/kspCaches/main/logs/kspLookupGraphOrigin.dot")
        Assert.assertTrue("Expected kspLookupGraphOrigin.dot to exist at ${dotFile.path}", dotFile.exists())
        val expectedDot =
            """
            digraph {
              "DownstreamClass";
              "DownstreamClass" -> "ExampleReference";
              "DownstreamClass" -> "JavaSourceDefaultReference";
              "DownstreamClass" -> "UpstreamExplicitTarget";
            }
            """.trimIndent()
        Assert.assertEquals(expectedDot, dotFile.readText().trim())
    }

    @Test
    fun testJavaSourceAnnotationPartialDefaultValueOverridden() {
        project.appendProperty("ksp.incremental.log=true")
        project.appendProperty("ksp.incremental.log.graph.origin=DownstreamClass")
        File(project.root, "downstream/src/main/java/JavaSourceMultiDefaultReference.java").apply {
            parentFile.mkdirs()
        }.writeText(
            """
            public @interface JavaSourceMultiDefaultReference {
                Class<?> value() default UpstreamEntryPoint.class;
                String tag() default "defaultTag";
            }
            """.trimIndent() + "\n"
        )
        File(project.root, "downstream/src/main/kotlin/DownstreamClass.kt").writeText(
            """
            @ExampleReference(UpstreamExplicitTarget::class)
            @JavaSourceMultiDefaultReference(value = UpstreamExplicitTarget::class)
            class DownstreamClass
            """.trimIndent() + "\n"
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
        }

        val dotFile = File(project.root, "downstream/build/kspCaches/main/logs/kspLookupGraphOrigin.dot")
        Assert.assertTrue("Expected kspLookupGraphOrigin.dot to exist at ${dotFile.path}", dotFile.exists())
        val expectedDot =
            """
            digraph {
              "DownstreamClass";
              "DownstreamClass" -> "ExampleReference";
              "DownstreamClass" -> "JavaSourceMultiDefaultReference";
              "DownstreamClass" -> "UpstreamExplicitTarget";
            }
            """.trimIndent()
        Assert.assertEquals(expectedDot, dotFile.readText().trim())
    }

    @Test
    fun testKotlinSourceAnnotationDefaultValueOverriddenDependencyGraphOrigin() {
        project.appendProperty("ksp.incremental.log=true")
        project.appendProperty("ksp.incremental.log.graph.origin=DownstreamClass")
        File(project.root, "downstream/src/main/kotlin/DownstreamClass.kt").writeText(
            """
            @ExampleReference(UpstreamExplicitTarget::class)
            @ExampleDefaultReference(UpstreamExplicitTarget::class)
            class DownstreamClass
            """.trimIndent() + "\n"
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
        }

        val dotFile = File(project.root, "downstream/build/kspCaches/main/logs/kspLookupGraphOrigin.dot")
        Assert.assertTrue("Expected kspLookupGraphOrigin.dot to exist at ${dotFile.path}", dotFile.exists())
        val expectedDot =
            """
            digraph {
              "DownstreamClass";
              "DownstreamClass" -> "ExampleDefaultReference";
              "DownstreamClass" -> "ExampleReference";
              "DownstreamClass" -> "UpstreamExplicitTarget";
            }
            """.trimIndent()
        Assert.assertEquals(expectedDot, dotFile.readText().trim())
    }

    @Test
    fun testExplicitJavaNestedAnnotationClassReference() {
        project.appendProperty("ksp.incremental.log=true")
        project.appendProperty("ksp.incremental.log.graph.origin=DownstreamJavaClass")
        File(project.root, "downstream/src/main/java/InnerJavaAnno.java").apply {
            parentFile.mkdirs()
        }.writeText(
            """
            public @interface InnerJavaAnno {
                Class<?> value();
            }
            """.trimIndent() + "\n"
        )
        File(project.root, "downstream/src/main/java/OuterJavaAnno.java").writeText(
            """
            public @interface OuterJavaAnno {
                InnerJavaAnno value();
            }
            """.trimIndent() + "\n"
        )
        File(project.root, "downstream/src/main/java/DownstreamJavaClass.java").writeText(
            """
            @ExampleJavaReference(UpstreamExplicitTarget.class)
            @OuterJavaAnno(@InnerJavaAnno(UpstreamEntryPoint.class))
            public class DownstreamJavaClass {
            }
            """.trimIndent() + "\n"
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            Assert.assertTrue(result.output.lines().contains("$PROCESSOR_LABEL Processing DownstreamJavaClass"))
            Assert.assertTrue(result.output.lines().contains("$PROCESSOR_LABEL Processing UpstreamChanges"))
        }

        val dotFile = File(project.root, "downstream/build/kspCaches/main/logs/kspLookupGraphOrigin.dot")
        Assert.assertTrue("Expected kspLookupGraphOrigin.dot to exist at ${dotFile.path}", dotFile.exists())
        val expectedDot =
            """
            digraph {
              "DownstreamJavaClass";
              "DownstreamJavaClass" -> "DownstreamJavaClass";
              "DownstreamJavaClass" -> "ExampleJavaReference";
              "DownstreamJavaClass" -> "InnerJavaAnno";
              "DownstreamJavaClass" -> "OuterJavaAnno";
              "DownstreamJavaClass" -> "UpstreamEntryPoint";
              "DownstreamJavaClass" -> "UpstreamExplicitTarget";
              "UpstreamEntryPoint" -> "ExampleReference";
              "UpstreamEntryPoint" -> "UpstreamChanges";
            }
            """.trimIndent()
        Assert.assertEquals(expectedDot, dotFile.readText().trim())

        File(project.root, "upstream/src/main/kotlin/UpstreamChanges.kt").appendText("{ val prop2 = false }")

        gradleRunner.withArguments(ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            Assert.assertTrue(
                "Expected DownstreamJavaClass to be reprocessed after UpstreamChanges modification",
                result.output.lines().contains("$PROCESSOR_LABEL Processing DownstreamJavaClass")
            )
        }
    }

    @Test
    fun testJavaSourceDefaultNestedAnnotationMultipleConsumers() {
        project.appendProperty("ksp.incremental.log=true")
        project.appendProperty("ksp.incremental.log.graph.origin=SecondDownstreamClass")
        File(project.root, "downstream/src/main/java/InnerJavaDefaultAnno.java").apply {
            parentFile.mkdirs()
        }.writeText(
            """
            public @interface InnerJavaDefaultAnno {
                Class<?> value();
            }
            """.trimIndent() + "\n"
        )
        File(project.root, "downstream/src/main/java/JavaSourceDefaultNestedAnno.java").writeText(
            """
            public @interface JavaSourceDefaultNestedAnno {
                InnerJavaDefaultAnno value() default @InnerJavaDefaultAnno(UpstreamEntryPoint.class);
            }
            """.trimIndent() + "\n"
        )
        File(project.root, "downstream/src/main/kotlin/DownstreamClass.kt").writeText(
            """
            @ExampleReference(UpstreamExplicitTarget::class)
            @JavaSourceDefaultNestedAnno
            class DownstreamClass
            """.trimIndent() + "\n"
        )
        File(project.root, "downstream/src/main/kotlin/SecondDownstreamClass.kt").writeText(
            """
            @ExampleReference(UpstreamExplicitTarget::class)
            @JavaSourceDefaultNestedAnno
            class SecondDownstreamClass
            """.trimIndent() + "\n"
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
        }

        val dotFile = File(project.root, "downstream/build/kspCaches/main/logs/kspLookupGraphOrigin.dot")
        Assert.assertTrue("Expected kspLookupGraphOrigin.dot to exist at ${dotFile.path}", dotFile.exists())
        val expectedDot =
            """
            digraph {
              "SecondDownstreamClass";
              "SecondDownstreamClass" -> "ExampleReference";
              "SecondDownstreamClass" -> "InnerJavaDefaultAnno";
              "SecondDownstreamClass" -> "JavaSourceDefaultNestedAnno";
              "SecondDownstreamClass" -> "UpstreamEntryPoint";
              "SecondDownstreamClass" -> "UpstreamExplicitTarget";
              "UpstreamEntryPoint" -> "ExampleReference";
              "UpstreamEntryPoint" -> "UpstreamChanges";
            }
            """.trimIndent()
        Assert.assertEquals(expectedDot, dotFile.readText().trim())

        File(project.root, "upstream/src/main/kotlin/UpstreamChanges.kt").appendText("{ val prop2 = false }")

        gradleRunner.withArguments(ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            val lines = result.output.lines()
            Assert.assertTrue(lines.contains("$PROCESSOR_LABEL Processing DownstreamClass"))
            Assert.assertTrue(lines.contains("$PROCESSOR_LABEL Processing SecondDownstreamClass"))
        }
    }

    @Test
    fun testJavaSourceDefaultNestedInnerAnnotationModified() {
        project.appendProperty("ksp.incremental.log=true")
        File(project.root, "downstream/src/main/java/InnerJavaDefaultAnno.java").apply {
            parentFile.mkdirs()
        }.writeText(
            """
            public @interface InnerJavaDefaultAnno {
                Class<?> value();
            }
            """.trimIndent() + "\n"
        )
        File(project.root, "downstream/src/main/java/JavaSourceDefaultNestedAnno.java").writeText(
            """
            public @interface JavaSourceDefaultNestedAnno {
                InnerJavaDefaultAnno value() default @InnerJavaDefaultAnno(UpstreamEntryPoint.class);
            }
            """.trimIndent() + "\n"
        )
        File(project.root, "downstream/src/main/kotlin/DownstreamClass.kt").writeText(
            """
            @ExampleReference(UpstreamExplicitTarget::class)
            @JavaSourceDefaultNestedAnno
            class DownstreamClass
            """.trimIndent() + "\n"
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
        }

        // Add a new class and reference it ONLY from a new defaulted method on InnerJavaDefaultAnno,
        // without touching JavaSourceDefaultNestedAnno or DownstreamClass.
        File(project.root, "downstream/src/main/kotlin/NewlyAddedInnerDefaultTarget.kt").writeText(
            "class NewlyAddedInnerDefaultTarget\n"
        )
        File(project.root, "downstream/src/main/java/InnerJavaDefaultAnno.java").writeText(
            """
            public @interface InnerJavaDefaultAnno {
                Class<?> value();
                Class<?> extra() default NewlyAddedInnerDefaultTarget.class;
            }
            """.trimIndent() + "\n"
        )

        gradleRunner.withArguments(ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            val lines = result.output.lines()
            Assert.assertTrue(
                "Expected DownstreamClass to be reprocessed when InnerJavaDefaultAnno changes",
                lines.contains("$PROCESSOR_LABEL Processing DownstreamClass")
            )
            Assert.assertTrue(
                "Expected NewlyAddedInnerDefaultTarget to be processed when InnerJavaDefaultAnno adds it as a default",
                lines.contains("$PROCESSOR_LABEL Processing NewlyAddedInnerDefaultTarget")
            )
        }
    }

    @Test
    fun testKotlinLibraryDefaultNestedInnerAnnotationModified() {
        project.appendProperty("ksp.incremental.log=true")
        project.appendProperty("ksp.incremental.log.graph.origin=DownstreamClass")
        File(project.root, "annotations/src/main/kotlin/InnerNestedDefaultAnno.kt").writeText(
            """
            import kotlin.reflect.KClass

            annotation class InnerNestedDefaultAnno(
                val value: KClass<*> = TransitiveChanges::class
            )
            """.trimIndent() + "\n"
        )
        File(project.root, "upstream/src/main/kotlin/OuterNestedDefaultAnno.kt").writeText(
            """
            annotation class OuterNestedDefaultAnno(
                val value: InnerNestedDefaultAnno = InnerNestedDefaultAnno()
            )
            """.trimIndent() + "\n"
        )
        File(project.root, "upstream/src/main/kotlin/UpstreamEntryPoint.kt").writeText(
            """
            @ExampleReference(UpstreamExplicitTarget::class)
            @OuterNestedDefaultAnno
            class UpstreamEntryPoint
            """.trimIndent() + "\n"
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
        }

        val dotFile = File(project.root, "downstream/build/kspCaches/main/logs/kspLookupGraphOrigin.dot")
        Assert.assertTrue("Expected kspLookupGraphOrigin.dot to exist at ${dotFile.path}", dotFile.exists())
        Assert.assertTrue(
            "Expected UpstreamEntryPoint -> InnerNestedDefaultAnno edge in lookup graph, got:\n${dotFile.readText()}",
            dotFile.readText().contains("\"UpstreamEntryPoint\" -> \"InnerNestedDefaultAnno\";")
        )

        // Modify ONLY InnerNestedDefaultAnno.kt in :annotations (adding a new class in :transitive)
        File(project.root, "transitive/src/main/kotlin/NewlyAddedTransitiveInnerTarget.kt").writeText(
            "class NewlyAddedTransitiveInnerTarget\n"
        )
        File(project.root, "annotations/src/main/kotlin/InnerNestedDefaultAnno.kt").writeText(
            """
            import kotlin.reflect.KClass

            annotation class InnerNestedDefaultAnno(
                val value: KClass<*> = TransitiveChanges::class,
                val extra: KClass<*> = NewlyAddedTransitiveInnerTarget::class
            )
            """.trimIndent() + "\n"
        )

        gradleRunner.withArguments(ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            val lines = result.output.lines()
            Assert.assertTrue(
                "Expected DownstreamClass to be reprocessed when library InnerNestedDefaultAnno changes",
                lines.contains("$PROCESSOR_LABEL Processing DownstreamClass")
            )
            Assert.assertTrue(
                "Expected NewlyAddedTransitiveInnerTarget to be processed when InnerNestedDefaultAnno adds it as a default",
                lines.contains("$PROCESSOR_LABEL Processing NewlyAddedTransitiveInnerTarget")
            )
        }
    }

    @Test
    fun testKotlinLibraryDefaultAnnotationModified() {
        project.appendProperty("ksp.incremental.log=true")
        project.appendProperty("ksp.incremental.log.graph.origin=DownstreamClass")
        File(project.root, "upstream/src/main/kotlin/UpstreamEntryPoint.kt").writeText(
            """
            @ExampleReference(UpstreamExplicitTarget::class)
            @ExampleTransitiveDefaultReference
            class UpstreamEntryPoint
            """.trimIndent() + "\n"
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
        }

        val dotFile = File(project.root, "downstream/build/kspCaches/main/logs/kspLookupGraphOrigin.dot")
        Assert.assertTrue("Expected kspLookupGraphOrigin.dot to exist at ${dotFile.path}", dotFile.exists())
        Assert.assertTrue(
            "Expected UpstreamEntryPoint -> ExampleTransitiveDefaultReference edge in lookup graph, got:\n${dotFile.readText()}",
            dotFile.readText().contains("\"UpstreamEntryPoint\" -> \"ExampleTransitiveDefaultReference\";")
        )

        // Modify ONLY ExampleTransitiveDefaultReference.kt in :annotations (adding a new class in :transitive)
        File(project.root, "transitive/src/main/kotlin/NewlyAddedTransitiveTarget.kt").writeText(
            "class NewlyAddedTransitiveTarget\n"
        )
        File(project.root, "annotations/src/main/kotlin/ExampleTransitiveDefaultReference.kt").writeText(
            """
            import kotlin.reflect.KClass

            annotation class ExampleTransitiveDefaultReference(
                val value: KClass<*> = TransitiveChanges::class,
                val extra: KClass<*> = NewlyAddedTransitiveTarget::class
            )

            annotation class ExampleTransitiveArrayDefaultReference(
                val value: Array<KClass<*>> = [TransitiveChanges::class]
            )
            """.trimIndent() + "\n"
        )

        gradleRunner.withArguments(ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            val lines = result.output.lines()
            Assert.assertTrue(
                "Expected DownstreamClass to be reprocessed when library ExampleTransitiveDefaultReference changes",
                lines.contains("$PROCESSOR_LABEL Processing DownstreamClass")
            )
            Assert.assertTrue(
                "Expected NewlyAddedTransitiveTarget to be processed when ExampleTransitiveDefaultReference adds it as a default",
                lines.contains("$PROCESSOR_LABEL Processing NewlyAddedTransitiveTarget")
            )
        }
    }

    @Test
    fun testJavaLibraryDefaultAnnotationModified() {
        project.appendProperty("ksp.incremental.log=true")
        project.appendProperty("ksp.incremental.log.graph.origin=DownstreamClass")
        File(project.root, "downstream/src/main/kotlin/DownstreamClass.kt").writeText(
            """
            @ExampleReference(UpstreamJavaEntryPoint::class)
            class DownstreamClass
            """.trimIndent() + "\n"
        )
        File(project.root, "upstream/src/main/java/UpstreamJavaEntryPoint.java").writeText(
            """
            @ExampleJavaReference(UpstreamExplicitTarget.class)
            @ExampleJavaTransitiveDefaultReference
            public class UpstreamJavaEntryPoint {
            }
            """.trimIndent() + "\n"
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
        }

        val dotFile = File(project.root, "downstream/build/kspCaches/main/logs/kspLookupGraphOrigin.dot")
        Assert.assertTrue("Expected kspLookupGraphOrigin.dot to exist at ${dotFile.path}", dotFile.exists())
        Assert.assertTrue(
            "Expected UpstreamJavaEntryPoint -> ExampleJavaTransitiveDefaultReference edge in lookup graph, got:\n${dotFile.readText()}",
            dotFile.readText().contains("\"UpstreamJavaEntryPoint\" -> \"ExampleJavaTransitiveDefaultReference\";")
        )

        // Modify ONLY ExampleJavaTransitiveDefaultReference.java in :annotations (adding a new class in :transitive)
        File(project.root, "transitive/src/main/kotlin/NewlyAddedJavaTransitiveTarget.kt").writeText(
            "class NewlyAddedJavaTransitiveTarget\n"
        )
        File(project.root, "annotations/src/main/java/ExampleJavaTransitiveDefaultReference.java").writeText(
            """
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            @Retention(RetentionPolicy.RUNTIME)
            public @interface ExampleJavaTransitiveDefaultReference {
                Class<?> value() default TransitiveChanges.class;
                Class<?> extra() default NewlyAddedJavaTransitiveTarget.class;
            }
            """.trimIndent() + "\n"
        )

        gradleRunner.withArguments(ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            val lines = result.output.lines()
            Assert.assertTrue(
                "Expected DownstreamClass to be reprocessed when library ExampleJavaTransitiveDefaultReference changes",
                lines.contains("$PROCESSOR_LABEL Processing DownstreamClass")
            )
            Assert.assertTrue(
                "Expected NewlyAddedJavaTransitiveTarget to be processed when ExampleJavaTransitiveDefaultReference adds it as a default",
                lines.contains("$PROCESSOR_LABEL Processing NewlyAddedJavaTransitiveTarget")
            )
        }
    }
}
