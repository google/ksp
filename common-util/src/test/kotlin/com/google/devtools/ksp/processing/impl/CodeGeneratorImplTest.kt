package com.google.devtools.ksp.processing.impl

import com.google.devtools.ksp.common.AnyChanges
import com.google.devtools.ksp.common.impl.CodeGeneratorImpl
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CodeGeneratorImplTest {

    lateinit var codeGenerator: CodeGeneratorImpl
    lateinit var baseDir: File

    @Before
    fun setup() {
        baseDir = Files.createTempDirectory("project").toFile()
        val classesDir = File(baseDir, "classes")
        classesDir.mkdir()
        val javaDir = File(baseDir, "java")
        javaDir.mkdir()
        val kotlinDir = File(baseDir, "kotlin")
        kotlinDir.mkdir()
        val resourcesDir = File(baseDir, "resources")
        resourcesDir.mkdir()
        codeGenerator = CodeGeneratorImpl(
            classesDir,
            { javaDir },
            kotlinDir,
            resourcesDir,
            baseDir,
            AnyChanges(baseDir),
            emptyList(),
            true
        )
    }

    @Test
    fun testCreatingAFile() {
        codeGenerator.createNewFile(Dependencies.ALL_FILES, "a.b.c", "Test", "java")
        codeGenerator.createNewFile(Dependencies.ALL_FILES, "a.b.c", "Test", "kt")
        codeGenerator.createNewFile(Dependencies.ALL_FILES, "a.b.c", "Test", "class")
        codeGenerator.createNewFile(Dependencies.ALL_FILES, "a.b.c", "Test", "")

        val files = codeGenerator.generatedFile.toList()
        Assert.assertEquals(File(baseDir, "java/a/b/c/Test.java"), files[0])
        Assert.assertEquals(File(baseDir, "kotlin/a/b/c/Test.kt"), files[1])
        Assert.assertEquals(File(baseDir, "classes/a/b/c/Test.class"), files[2])
        Assert.assertEquals(File(baseDir, "resources/a/b/c/Test"), files[3])

        try {
            codeGenerator.outputs
        } catch (e: Exception) {
            Assert.fail("Failed to get outputs: ${e.message}")
        }
    }

    @Test
    fun testCreatingAFileWithSlash() {
        codeGenerator.createNewFile(Dependencies.ALL_FILES, "a/b/c", "Test", "java")
        codeGenerator.createNewFile(Dependencies.ALL_FILES, "a/b/c", "Test", "kt")
        codeGenerator.createNewFile(Dependencies.ALL_FILES, "a/b/c", "Test", "class")
        codeGenerator.createNewFile(Dependencies.ALL_FILES, "a/b/c", "Test", "")

        val files = codeGenerator.generatedFile.toList()
        Assert.assertEquals(File(baseDir, "java/a/b/c/Test.java"), files[0])
        Assert.assertEquals(File(baseDir, "kotlin/a/b/c/Test.kt"), files[1])
        Assert.assertEquals(File(baseDir, "classes/a/b/c/Test.class"), files[2])
        Assert.assertEquals(File(baseDir, "resources/a/b/c/Test"), files[3])

        try {
            codeGenerator.outputs
        } catch (e: Exception) {
            Assert.fail("Failed to get outputs: ${e.message}")
        }
    }

    @Test
    fun testCreatingAFileWithPath() {
        codeGenerator.createNewFileByPath(Dependencies.ALL_FILES, "a/b/c/Test", "java")
        codeGenerator.createNewFileByPath(Dependencies.ALL_FILES, "a/b/c/Test")
        codeGenerator.createNewFileByPath(Dependencies.ALL_FILES, "a/b/c/Test", "class")
        codeGenerator.createNewFileByPath(Dependencies.ALL_FILES, "a/b/c/Test", "")

        val files = codeGenerator.generatedFile.toList()
        Assert.assertEquals(File(baseDir, "java/a/b/c/Test.java"), files[0])
        Assert.assertEquals(File(baseDir, "kotlin/a/b/c/Test.kt"), files[1])
        Assert.assertEquals(File(baseDir, "classes/a/b/c/Test.class"), files[2])
        Assert.assertEquals(File(baseDir, "resources/a/b/c/Test"), files[3])

        try {
            codeGenerator.outputs
        } catch (e: Exception) {
            Assert.fail("Failed to get outputs: ${e.message}")
        }
    }

    @Test
    fun testCreatingAFileWithPathAndDots() {
        codeGenerator.createNewFileByPath(Dependencies.ALL_FILES, "a/b/c/dir.with.dot/Test", "java")
        codeGenerator.createNewFileByPath(Dependencies.ALL_FILES, "a/b/c/dir.with.dot/Test")
        codeGenerator.createNewFileByPath(Dependencies.ALL_FILES, "a/b/c/dir.with.dot/Test", "class")
        codeGenerator.createNewFileByPath(Dependencies.ALL_FILES, "a/b/c/dir.with.dot/Test", "")

        val files = codeGenerator.generatedFile.toList()
        Assert.assertEquals(File(baseDir, "java/a/b/c/dir.with.dot/Test.java"), files[0])
        Assert.assertEquals(File(baseDir, "kotlin/a/b/c/dir.with.dot/Test.kt"), files[1])
        Assert.assertEquals(File(baseDir, "classes/a/b/c/dir.with.dot/Test.class"), files[2])
        Assert.assertEquals(File(baseDir, "resources/a/b/c/dir.with.dot/Test"), files[3])

        try {
            codeGenerator.outputs
        } catch (e: Exception) {
            Assert.fail("Failed to get outputs: ${e.message}")
        }
    }

    @Test
    fun testCreatingAFileByPathWithInvalidPath() {
        try {
            codeGenerator.createNewFileByPath(Dependencies.ALL_FILES, "../../b/c/Test", "java")
            Assert.fail()
        } catch (e: java.lang.IllegalStateException) {
            Assert.assertEquals(e.message, "requested path is outside the bounds of the required directory")
        }
    }

    @Test
    fun testWarnOnEmptyNonAggregatingDependencies() {
        val warnings = mutableListOf<String>()
        val testLogger = object : KSPLogger {
            override fun logging(message: String, symbol: KSNode?) {}
            override fun info(message: String, symbol: KSNode?) {}
            override fun warn(message: String, symbol: KSNode?) {
                warnings.add(message)
            }
            override fun error(message: String, symbol: KSNode?) {}
            override fun exception(e: Throwable) {}
        }
        val classesDir = File(baseDir, "classes")
        val javaDir = File(baseDir, "java")
        val kotlinDir = File(baseDir, "kotlin")
        val resourcesDir = File(baseDir, "resources")
        val generatorWithLogger = CodeGeneratorImpl(
            classesDir,
            { javaDir },
            kotlinDir,
            resourcesDir,
            baseDir,
            AnyChanges(baseDir),
            emptyList(),
            true,
            testLogger
        )
        generatorWithLogger.createNewFile(Dependencies(false), "a.b.c", "WarnTest", "kt")
        Assert.assertEquals(1, warnings.size)
        Assert.assertTrue(warnings[0].contains("aggregating = false"))
    }
}
