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

package com.google.devtools.ksp.common.impl

import com.google.devtools.ksp.common.AnyChanges
import com.google.devtools.ksp.common.NoSourceFile
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Origin
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.lang.reflect.Proxy

class CodeGeneratorImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun associateWithClassesTreatsJavaLibClassAsNoSourceFile() {
        val projectBase = tempFolder.newFolder("project")
        val codeGenerator = createCodeGenerator(projectBase)
        val clsFile = fakeFile(
            fileName = "JavaLib.class",
            filePath = "/external/gradle/cache/upstream.jar!/com/example/JavaLib.class"
        )
        val javaLibClass = fakeDeclaration<KSClassDeclaration>(
            origin = Origin.JAVA_LIB,
            qualifiedName = "com.example.JavaLib",
            containingFile = clsFile
        )
        val kotlinLibClass = fakeDeclaration<KSClassDeclaration>(
            origin = Origin.KOTLIN_LIB,
            qualifiedName = "com.example.KotlinLib",
            containingFile = null
        )
        val sourceFile = fakeFile(
            fileName = "JavaSrc.java",
            filePath = File(projectBase, "src/main/java/com/example/JavaSrc.java").path
        )
        val javaSrcClass = fakeDeclaration<KSClassDeclaration>(
            origin = Origin.JAVA,
            qualifiedName = "com.example.JavaSrc",
            containingFile = sourceFile
        )

        codeGenerator.associateWithClasses(
            listOf(javaLibClass, kotlinLibClass, javaSrcClass),
            "com.example",
            "Generated",
            "kt"
        )

        val expectedOutput = setOf(File("out/kotlin/com/example/Generated.kt"))
        val expectedJavaLibKey = File(NoSourceFile(projectBase, "com.example.JavaLib").filePath).relativeTo(projectBase)
        val expectedKotlinLibKey =
            File(NoSourceFile(projectBase, "com.example.KotlinLib").filePath).relativeTo(projectBase)
        val expectedSrcKey = File("src/main/java/com/example/JavaSrc.java")

        assertEquals(
            mapOf(
                expectedJavaLibKey to expectedOutput,
                expectedKotlinLibKey to expectedOutput,
                expectedSrcKey to expectedOutput,
            ),
            codeGenerator.sourceToOutputs
        )
    }

    @Test
    fun associateWithClassesUnwrapsLibraryEnumEntryToEnclosingEnumClass() {
        val projectBase = tempFolder.newFolder("project")
        val codeGenerator = createCodeGenerator(projectBase)
        val clsFile = fakeFile(
            fileName = "JavaLibEnum.class",
            filePath = "/external/gradle/cache/upstream.jar!/com/example/JavaLibEnum.class"
        )
        val javaLibEnum = fakeDeclaration<KSClassDeclaration>(
            origin = Origin.JAVA_LIB,
            qualifiedName = "com.example.JavaLibEnum",
            containingFile = clsFile,
            classKind = ClassKind.ENUM_CLASS,
        )
        val javaLibEnumEntry = fakeDeclaration<KSClassDeclaration>(
            origin = Origin.JAVA_LIB,
            qualifiedName = "com.example.JavaLibEnum.ENTRY_A",
            containingFile = clsFile,
            classKind = ClassKind.ENUM_ENTRY,
            parentDeclaration = javaLibEnum,
        )
        val kotlinLibEnum = fakeDeclaration<KSClassDeclaration>(
            origin = Origin.KOTLIN_LIB,
            qualifiedName = "com.example.KotlinLibEnum",
            containingFile = null,
            classKind = ClassKind.ENUM_CLASS,
        )
        val kotlinLibEnumEntry = fakeDeclaration<KSClassDeclaration>(
            origin = Origin.KOTLIN_LIB,
            qualifiedName = "com.example.KotlinLibEnum.ENTRY_B",
            containingFile = null,
            classKind = ClassKind.ENUM_ENTRY,
            parentDeclaration = kotlinLibEnum,
        )

        codeGenerator.associateWithClasses(
            listOf(javaLibEnumEntry, kotlinLibEnumEntry),
            "com.example",
            "GeneratedFromEnumEntries",
            "kt"
        )

        val expectedOutput = setOf(File("out/kotlin/com/example/GeneratedFromEnumEntries.kt"))
        val expectedJavaEnumKey =
            File(NoSourceFile(projectBase, "com.example.JavaLibEnum").filePath).relativeTo(projectBase)
        val expectedKotlinEnumKey =
            File(NoSourceFile(projectBase, "com.example.KotlinLibEnum").filePath).relativeTo(projectBase)

        assertEquals(
            mapOf(
                expectedJavaEnumKey to expectedOutput,
                expectedKotlinEnumKey to expectedOutput,
            ),
            codeGenerator.sourceToOutputs
        )
    }

    @Test
    fun associateWithFunctionsAndPropertiesTreatJavaLibAndCompiledSyntheticMembersAsLibrarySymbols() {
        val projectBase = tempFolder.newFolder("project")
        val wildcard = AnyChanges(projectBase)
        val codeGenerator = createCodeGenerator(projectBase, wildcard)
        val clsFile = fakeFile(
            fileName = "JavaLib.class",
            filePath = "/external/gradle/cache/upstream.jar!/com/example/JavaLib.class"
        )
        val javaLibFun = fakeDeclaration<KSFunctionDeclaration>(
            origin = Origin.JAVA_LIB,
            qualifiedName = "com.example.JavaLib.foo",
            containingFile = clsFile
        )
        val syntheticCompiledFun = fakeDeclaration<KSFunctionDeclaration>(
            origin = Origin.SYNTHETIC,
            qualifiedName = "com.example.JavaLib.getBar",
            containingFile = clsFile
        )
        val javaLibProp = fakeDeclaration<KSPropertyDeclaration>(
            origin = Origin.JAVA_LIB,
            qualifiedName = "com.example.JavaLib.field",
            containingFile = clsFile
        )

        codeGenerator.associateWithFunctions(
            listOf(javaLibFun, syntheticCompiledFun),
            "com.example",
            "GeneratedFun",
            "kt"
        )
        codeGenerator.associateWithProperties(
            listOf(javaLibProp),
            "com.example",
            "GeneratedProp",
            "kt"
        )

        val expectedWildcardKey = File(wildcard.filePath).relativeTo(projectBase)
        assertEquals(
            mapOf(
                expectedWildcardKey to setOf(
                    File("out/kotlin/com/example/GeneratedFun.kt"),
                    File("out/kotlin/com/example/GeneratedProp.kt"),
                )
            ),
            codeGenerator.sourceToOutputs
        )
    }

    private fun createCodeGenerator(
        projectBase: File,
        wildcard: KSFile = AnyChanges(projectBase),
    ): CodeGeneratorImpl {
        val outDir = File(projectBase, "out")
        return CodeGeneratorImpl(
            classDir = File(outDir, "classes"),
            javaDir = { File(outDir, "java") },
            kotlinDir = File(outDir, "kotlin"),
            resourcesDir = File(outDir, "resources"),
            projectBase = projectBase,
            anyChangesWildcard = wildcard,
            allSources = emptyList(),
            isIncremental = true,
        )
    }

    private fun fakeFile(fileName: String, filePath: String): KSFile {
        return Proxy.newProxyInstance(
            KSFile::class.java.classLoader,
            arrayOf(KSFile::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getFileName" -> fileName
                "getFilePath" -> filePath
                "toString" -> "File: $fileName"
                else -> throw UnsupportedOperationException("Unexpected call: ${method.name}")
            }
        } as KSFile
    }

    private inline fun <reified T : Any> fakeDeclaration(
        origin: Origin,
        qualifiedName: String,
        containingFile: KSFile?,
        classKind: ClassKind = ClassKind.CLASS,
        parentDeclaration: KSDeclaration? = null,
    ): T {
        val ksName = KSNameImpl.getCached(qualifiedName)
        return Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getOrigin" -> origin
                "getQualifiedName" -> ksName
                "getContainingFile" -> containingFile
                "getClassKind" -> classKind
                "getParentDeclaration" -> parentDeclaration
                "toString" -> qualifiedName
                else -> throw UnsupportedOperationException("Unexpected call: ${method.name}")
            }
        } as T
    }
}
