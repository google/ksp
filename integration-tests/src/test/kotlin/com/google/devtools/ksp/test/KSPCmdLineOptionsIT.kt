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
import java.net.URLClassLoader

data class CompileResult(val exitCode: ExitCode, val output: String)

@RunWith(Parameterized::class)
class KSPCmdLineOptionsIT(useKSP2: Boolean) {
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

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
