package com.google.devtools.ksp.test.secondary

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.net.URLClassLoader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class KSPCmdLineOptionsIT(experimentalPsiResolution: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "cmd-options",
        experimentalPsiResolution = experimentalPsiResolution
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Boolean> = listOf(true, false)
    }

    private fun getKspClasspath(): List<File> {
        val repoPath = "../build/repos/test/com/google/devtools/ksp/"

        val commonDepsJar = File("$repoPath/symbol-processing-common-deps/${System.getProperty("kspVersion")}")
            .listFiles()!!.filter {
                it.name.matches(Regex(".*-\\d.jar"))
            }.maxByOrNull { it.lastModified() }!!
        val kspMainJar = File("$repoPath/symbol-processing-aa-embeddable/${System.getProperty("kspVersion")}")
            .listFiles()!!.filter {
                it.name.matches(Regex(".*-\\d.jar"))
            }.maxByOrNull { it.lastModified() }!!
        val kspApiJar = File("$repoPath/symbol-processing-api/${System.getProperty("kspVersion")}")
            .listFiles()!!.filter {
                it.name.matches(Regex(".*-\\d.jar"))
            }.maxByOrNull { it.lastModified() }!!

        val stdlibJar = File(KotlinVersion::class.java.protectionDomain.codeSource.location.toURI())
        val coroutinesJar =
            File(kotlinx.coroutines.Dispatchers::class.java.protectionDomain.codeSource.location.toURI())

        return listOf(commonDepsJar, kspMainJar, kspApiJar, stdlibJar, coroutinesJar)
    }

    private fun getKsp2Main(mainClassName: String): Method {
        val kspClasspath = getKspClasspath().map { it.toURI().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(kspClasspath)
        val kspMainClass = classLoader.loadClass(mainClassName)

        return kspMainClass.getMethod(
            "main",
            Array<String>::class.java,
        )
    }

    private fun getKsp2SharedArgs(): List<String> {
        val outDir = "${project.root.path}/build/out"
        val srcDir = "${project.root.path}/workload/src/"

        return listOf(
            "-module-name=main",
            "-project-base-dir", project.root.path,
            "-source-roots", srcDir,
            "-output-base-dir=$outDir",
            "-caches-dir=$outDir",
            "-class-output-dir=$outDir",
            "-kotlin-output-dir=$outDir",
            "-resource-output-dir", outDir,
            "-language-version=2.0",
            "-api-version=2.0",
        )
    }

    private fun buildProcessorJar(): String {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("clean", ":processors:build").build()
        return File(project.root, "processors/build/libs/processors-1.0-SNAPSHOT.jar").absolutePath
    }

    fun testKsp2(mainClassName: String, platformArgs: List<String>) {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))

        val sharedArgs = getKsp2SharedArgs()
        val kspMain = getKsp2Main(mainClassName)
        val processorJar = buildProcessorJar()

        val outDir = "${project.root.path}/build/out"
        val args = sharedArgs + platformArgs + listOf(processorJar)

        kspMain.invoke(null, args.toTypedArray())

        val status = File(outDir, "Status.log")
        Assert.assertTrue(status.exists() && status.readText() == "OK")

        val args2 = args + listOf("-processor-options", "error=true")
        Assert.assertThrows(IllegalStateException::class.java) {
            try {
                kspMain.invoke(null, args2.toTypedArray())
            } catch (e: InvocationTargetException) {
                Assert.assertTrue(e.targetException is IllegalStateException)
                Assert.assertTrue(e.targetException.message == "Error on request")
                throw e.targetException
            }
        }
    }

    @Test
    fun testKSPJvmMain() {
        val outDir = "${project.root.path}/build/out"
        testKsp2(
            "com.google.devtools.ksp.cmdline.KSPJvmMain",
            listOf(
                "-java-output-dir", outDir,
                "-jvm-target", "11",
            )
        )
    }

    @Test
    fun testKSPCommonMain() {
        testKsp2(
            "com.google.devtools.ksp.cmdline.KSPCommonMain",
            listOf(
                "-targets=common",
            )
        )
    }

    @Test
    fun testKSPJsMain() {
        testKsp2(
            "com.google.devtools.ksp.cmdline.KSPJsMain",
            listOf(
                "-backend=JS",
            )
        )
    }

    @Test
    fun testKSPNativeMain() {
        testKsp2(
            "com.google.devtools.ksp.cmdline.KSPNativeMain",
            listOf(
                "-target=LinuxX64"
            )
        )
    }

    /**
     * Parameterized test for checking that KSP exits correctly when a processor throws an
     * exception. This test spawns a new process for the invocation to reproduce the exact
     * setup where background threads are spawned.
     *
     * See GitHub issue 3120 for more information.
     */
    fun testKsp2ProcessTerminationOnCrash(mainClassName: String, platformArgs: List<String>) {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))

        val sharedArgs = getKsp2SharedArgs()
        val processorJar = buildProcessorJar()
        val args = sharedArgs + platformArgs + listOf(processorJar, "-processor-options", "error=true")

        val javaBinary = File(System.getProperty("java.home"), "bin/java").absolutePath
        val cp = getKspClasspath().joinToString(File.pathSeparator) { it.absolutePath }
        val process = ProcessBuilder(listOf(javaBinary, "-cp", cp, mainClassName) + args)
            .redirectErrorStream(true)
            .start()

        // Drain stdout/stderr asynchronously:
        // 1. Prevent deadlocks: If the child process output exceeds the OS pipe buffer capacity
        //    (typically 64 KB on Linux), the child process will block on write, causing an artificial hang.
        // 2. Avoid blocking the test thread: Reading synchronously would block forever if the process hangs.
        // 3. Diagnostics: Retain the child process output for actionable debugging if the test fails.
        val outputFuture = CompletableFuture.supplyAsync {
            process.inputStream.bufferedReader().use { it.readText() }
        }
        val finished = process.waitFor(60, TimeUnit.SECONDS)
        val output = try {
            outputFuture.get(1, TimeUnit.SECONDS)
        } catch (e: Exception) {
            "<timed out reading output: ${e.message}>"
        }
        if (!finished) {
            process.destroyForcibly()
            Assert.fail("KSP CLI process hung indefinitely when a processor crashed. Process output:\n$output")
        }
        Assert.assertEquals("Expected exit code 1. Process output:\n$output", 1, process.exitValue())
        Assert.assertTrue(
            "Process output should contain processor exception. Process output:\n$output",
            output.contains("Error on request")
        )
    }

    @Test
    fun testKSPJvmMainProcessTerminationOnCrash() {
        val outDir = "${project.root.path}/build/out"
        testKsp2ProcessTerminationOnCrash(
            "com.google.devtools.ksp.cmdline.KSPJvmMain",
            listOf(
                "-java-output-dir", outDir,
                "-jvm-target", "11",
            )
        )
    }

    @Test
    fun testKSPCommonMainProcessTerminationOnCrash() {
        testKsp2ProcessTerminationOnCrash(
            "com.google.devtools.ksp.cmdline.KSPCommonMain",
            listOf(
                "-targets=common",
            )
        )
    }

    @Test
    fun testKSPJsMainProcessTerminationOnCrash() {
        testKsp2ProcessTerminationOnCrash(
            "com.google.devtools.ksp.cmdline.KSPJsMain",
            listOf(
                "-backend=JS",
            )
        )
    }

    @Test
    fun testKSPNativeMainProcessTerminationOnCrash() {
        testKsp2ProcessTerminationOnCrash(
            "com.google.devtools.ksp.cmdline.KSPNativeMain",
            listOf(
                "-target=LinuxX64"
            )
        )
    }
}
