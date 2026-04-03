package com.google.devtools.ksp

import com.google.devtools.ksp.processing.kspJvmArgParser
import com.google.devtools.ksp.processing.kspJvmArgParserHelp
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

@ParameterizedClass
@ValueSource(booleans = [true, false])
class CommandLineArgParserTest(private val isExperimentalPsiResolution: Boolean) {

    @Test
    fun testJvm() {
        val sep = File.pathSeparator
        val args = arrayListOf(
            "-module-name=MyModule",
            "-source-roots", "/path/to/A$sep/path/to/B",
            "/path/to/processorA.jar",
            "-kotlin-output-dir=/path/to/output/kotlin",
            "-java-output-dir=/path/to/output/java",
            "-class-output-dir=/path/to/output/class",
            "-resource-output-dir=/path/to/output/resource",
            "-language-version=2.0",
            "-api-version=2.0",
            "-jvm-target", "21",
            "-project-base-dir", "/path/to/base",
            "-output-base-dir", "/path/to/output",
            "-caches-dir", "/path/to/caches",
            "-experimental-psi-resolution=$isExperimentalPsiResolution",
            "/path/to/processorB.jar${sep}rel/to/processorC.jar",
        ).toTypedArray()
        val (config, classpath) = kspJvmArgParser(args)
        Assertions.assertEquals(
            listOf("/path/to/A", "/path/to/B").map(::File),
            config.sourceRoots
        )
        Assertions.assertEquals(
            "MyModule",
            config.moduleName
        )
        Assertions.assertEquals(
            isExperimentalPsiResolution,
            config.experimentalPsiResolution
        )
        Assertions.assertEquals(
            listOf("/path/to/processorA.jar", "/path/to/processorB.jar", "rel/to/processorC.jar"),
            classpath
        )
    }

    @Test
    fun testJvmHelp() {
        val helpMsg = kspJvmArgParserHelp()
        Assertions.assertTrue("*   -java-output-dir=File" in helpMsg)
        Assertions.assertTrue("    -libraries=List<File>" in helpMsg)
        Assertions.assertTrue("    -experimental-psi-resolution=Boolean" in helpMsg)
        Assertions.assertTrue("*   <processor classpath>" in helpMsg)
        println(helpMsg)
    }
}
