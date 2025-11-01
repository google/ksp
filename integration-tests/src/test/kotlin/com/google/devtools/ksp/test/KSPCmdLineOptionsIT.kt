package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.net.URLClassLoader

class KSPCmdLineOptionsIT() {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("cmd-options")

    private fun getKsp2Main(mainClassName: String): Method {
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

        val kspClasspath = listOf(
            commonDepsJar, kspMainJar, kspApiJar
        ).map { it.toURI().toURL() }.toTypedArray()
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

    fun testKsp2(mainClassName: String, platformArgs: List<String>) {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))

        val sharedArgs = getKsp2SharedArgs()
        val kspMain = getKsp2Main(mainClassName)

        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("clean", ":processors:build").build()
        val processorJar = File(project.root, "processors/build/libs/processors-1.0-SNAPSHOT.jar").absolutePath

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
}
