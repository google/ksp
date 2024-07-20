package com.google.devtools.ksp

import com.google.devtools.ksp.processing.kspJvmArgParser
import com.google.devtools.ksp.processing.kspJvmArgParserHelp
import org.junit.Assert
import org.junit.Test
import java.io.File

class CommandLineArgParserTest {
    @Test
    fun testJvm() {
        val args = arrayListOf<String>(
            "-module-name=MyModule",
            "-source-roots", "/path/to/A:/path/to/B",
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
            "/path/to/processorB.jar:rel/to/processorC.jar",
        ).toTypedArray()
        val (config, classpath) = kspJvmArgParser(args)
        Assert.assertEquals(
            listOf("/path/to/A", "/path/to/B").map(::File),
            config.sourceRoots
        )
        Assert.assertEquals(
            "MyModule",
            config.moduleName
        )
        Assert.assertEquals(
            listOf("/path/to/processorA.jar", "/path/to/processorB.jar", "rel/to/processorC.jar"),
            classpath
        )
    }

    @Test
    fun testJvmHelp() {
        val helpMsg = kspJvmArgParserHelp()
        Assert.assertTrue("*   -java-output-dir=File" in helpMsg)
        Assert.assertTrue("    -libraries=List<File>" in helpMsg)
        Assert.assertTrue("*   <processor classpath>" in helpMsg)
        println(helpMsg)
    }
}
