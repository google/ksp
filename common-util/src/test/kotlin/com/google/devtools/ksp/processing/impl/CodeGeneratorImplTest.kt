package com.google.devtools.ksp.processing.impl

import com.google.devtools.ksp.AnyChanges
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.FileType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CodeGeneratorImplTest {

    lateinit var codeGenerator: CodeGenerator
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
            javaDir,
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
    }

    @Test
    fun testCreatingAFileWithPath() {
        codeGenerator.createNewFile(Dependencies.ALL_FILES, "a/b/c/Test.java", FileType.JAVA_SOURCE)
        codeGenerator.createNewFile(Dependencies.ALL_FILES, "a/b/c/Test.kt", FileType.KOTLIN_SOURCE)
        codeGenerator.createNewFile(Dependencies.ALL_FILES, "a/b/c/Test.class", FileType.CLASS)
        codeGenerator.createNewFile(Dependencies.ALL_FILES, "a/b/c/Test", FileType.RESOURCE)

        val files = codeGenerator.generatedFile.toList()
        Assert.assertEquals(File(baseDir, "java/a/b/c/Test.java"), files[0])
        Assert.assertEquals(File(baseDir, "kotlin/a/b/c/Test.kt"), files[1])
        Assert.assertEquals(File(baseDir, "classes/a/b/c/Test.class"), files[2])
        Assert.assertEquals(File(baseDir, "resources/a/b/c/Test"), files[3])
    }

    @Test
    fun testCreatingAFileWithPathAndDots() {
        codeGenerator.createNewFile(Dependencies.ALL_FILES, "a/b/c/dir.with.dot/Test.java", FileType.JAVA_SOURCE)
        codeGenerator.createNewFile(Dependencies.ALL_FILES, "a/b/c/dir.with.dot/Test.kt", FileType.KOTLIN_SOURCE)
        codeGenerator.createNewFile(Dependencies.ALL_FILES, "a/b/c/dir.with.dot/Test.class", FileType.CLASS)
        codeGenerator.createNewFile(Dependencies.ALL_FILES, "a/b/c/dir.with.dot/Test", FileType.RESOURCE)

        val files = codeGenerator.generatedFile.toList()
        Assert.assertEquals(File(baseDir, "java/a/b/c/dir.with.dot/Test.java"), files[0])
        Assert.assertEquals(File(baseDir, "kotlin/a/b/c/dir.with.dot/Test.kt"), files[1])
        Assert.assertEquals(File(baseDir, "classes/a/b/c/dir.with.dot/Test.class"), files[2])
        Assert.assertEquals(File(baseDir, "resources/a/b/c/dir.with.dot/Test"), files[3])
    }
}
