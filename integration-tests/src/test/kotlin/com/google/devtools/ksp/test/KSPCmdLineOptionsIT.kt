package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.junit.Assert
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.net.URLClassLoader

data class CompileResult(val exitCode: ExitCode, val output: String)

@RunWith(Parameterized::class)
class KSPCmdLineOptionsIT(val useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("cmd-options", useKSP2 = useKSP2)

    private fun runCmdCompiler(pluginOptions: List<String>): CompileResult {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("clean", ":processors:build").build()
        val processorJar = File(project.root, "processors/build/libs/processors-1.0-SNAPSHOT.jar")
        val classLoader = URLClassLoader(arrayOf(processorJar.toURI().toURL()), javaClass.classLoader)
        val compiler = classLoader.loadClass(K2JVMCompiler::class.java.name).newInstance() as K2JVMCompiler
        val repoPath = "../build/repos/test/com/google/devtools/ksp/"
        val kspPluginId = "com.google.devtools.ksp.symbol-processing"
        val kspPluginJar = File("$repoPath/symbol-processing-cmdline/2.0.255-SNAPSHOT").listFiles()!!.filter {
            it.name.matches(Regex(".*-\\d.jar"))
        }.maxByOrNull { it.lastModified() }!!
        val kspApiJar = File("$repoPath/symbol-processing-api/2.0.255-SNAPSHOT").listFiles()!!.filter {
            it.name.matches(Regex(".*-\\d.jar"))
        }.maxByOrNull { it.lastModified() }!!
        val compilerArgs = mutableListOf(
            "-no-stdlib",
            "-language-version", "1.9",
            "-Xplugin=${kspPluginJar.absolutePath}",
            "-Xplugin=${kspApiJar.absolutePath}",
            "-P", "plugin:$kspPluginId:apclasspath=${processorJar.absolutePath}",
            "-P", "plugin:$kspPluginId:projectBaseDir=${project.root}/build",
            "-P", "plugin:$kspPluginId:classOutputDir=${project.root}/build",
            "-P", "plugin:$kspPluginId:javaOutputDir=${project.root}/build/out",
            "-P", "plugin:$kspPluginId:kotlinOutputDir=${project.root}/build/out",
            "-P", "plugin:$kspPluginId:resourceOutputDir=${project.root}/build/out",
            "-P", "plugin:$kspPluginId:kspOutputDir=${project.root}/build/out",
            "-P", "plugin:$kspPluginId:cachesDir=${project.root}/build/out",
            "-P", "plugin:$kspPluginId:incremental=false",
            "-d", "${project.root}/build/out"
        )
        pluginOptions.forEach {
            compilerArgs.add("-P")
            compilerArgs.add("plugin:$kspPluginId:$it")
        }
        compilerArgs.add(File(project.root, "workload/src/main/kotlin/com/example/A.kt").absolutePath)
        val outStream = ByteArrayOutputStream()
        val exitCode = compiler.exec(PrintStream(outStream), *compilerArgs.toTypedArray())
        return CompileResult(exitCode, outStream.toString())
    }

    @Test
    fun testWithCompilationOnError() {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        Assume.assumeFalse(useKSP2)
        val result = runCmdCompiler(listOf("apoption=error=true", "withCompilation=true"))
        val errors = result.output.lines().filter { it.startsWith("error: [ksp]") }
        val exitCode = result.exitCode
        Assert.assertTrue(exitCode == ExitCode.COMPILATION_ERROR)
        Assert.assertTrue(
            errors.any {
                it.startsWith("error: [ksp] java.lang.IllegalStateException: Error on request")
            }
        )
    }

    @Test
    fun testWithCompilationOnErrorOk() {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        Assume.assumeFalse(useKSP2)
        val result = runCmdCompiler(listOf("apoption=error=true", "returnOkOnError=true", "withCompilation=true"))
        val errors = result.output.lines().filter { it.startsWith("error: [ksp]") }
        val exitCode = result.exitCode
        Assert.assertTrue(exitCode == ExitCode.OK)
        Assert.assertTrue(
            errors.any {
                it.startsWith("error: [ksp] java.lang.IllegalStateException: Error on request")
            }
        )
    }

    private fun getKsp2Main(mainClassName: String): Method {
        val repoPath = "../build/repos/test/com/google/devtools/ksp/"

        val commonDepsJar = File("$repoPath/symbol-processing-common-deps/2.0.255-SNAPSHOT").listFiles()!!.filter {
            it.name.matches(Regex(".*-\\d.jar"))
        }.maxByOrNull { it.lastModified() }!!
        val kspMainJar = File("$repoPath/symbol-processing-aa-embeddable/2.0.255-SNAPSHOT").listFiles()!!.filter {
            it.name.matches(Regex(".*-\\d.jar"))
        }.maxByOrNull { it.lastModified() }!!
        val kspApiJar = File("$repoPath/symbol-processing-api/2.0.255-SNAPSHOT").listFiles()!!.filter {
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
        Assume.assumeTrue(useKSP2)

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

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
