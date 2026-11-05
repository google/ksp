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
}
