/*
 * Copyright 2021 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
package com.google.devtools.ksp.gradle.testing

import java.io.File

/**
 * Simpler wrapper that represents a module in a test project.
 *
 * It has helper methods to add plugins, dependencies or sources to the project.
 *
 * It is loosely structured, which means it won't check its integrity. (e.g. you can add android
 * sources w/o adding android plugin). It is meant to serve as a convenient way to configure
 * modules.
 */
class TestModule(
    val moduleRoot: File,
    plugins: List<PluginDeclaration> = emptyList(),
    dependencies: List<DependencyDeclaration> = emptyList()
) {
    val plugins = LinkedHashSet(plugins)
    val dependencies = LinkedHashSet(dependencies)
    val buildFileAdditions = LinkedHashSet<String>()
    val name = moduleRoot.name

    init {
        moduleRoot.mkdirs()
    }

    /**
     * Adds the given source file to the main source set.
     */
    fun addSource(name: String, contents: String) {
        val srcDir = when {
            name.endsWith(".kt") -> kotlinSourceDir
            name.endsWith(".java") -> javaSourceDir
            else -> error("must provide java or kotlin file")
        }
        srcDir.resolve(name).writeText(contents)
    }

    /**
     * Adds the given source file to the test source set.
     */
    fun addTestSource(name: String, contents: String) {
        val srcDir = when {
            name.endsWith(".kt") -> kotlinTestSourceDir
            name.endsWith(".java") -> javaTestSourceDir
            else -> error("must provide java or kotlin file")
        }
        srcDir.resolve(name).writeText(contents)
    }

    /**
     * Adds the given source file to the AndroidTest source set.
     */
    fun addAndroidTestSource(name: String, contents: String) {
        val srcDir = when {
            name.endsWith(".kt") -> kotlinAndroidTestSourceDir
            name.endsWith(".java") -> javaAndroidTestSourceDir
            else -> error("must provide java or kotlin file")
        }
        srcDir.resolve(name).writeText(contents)
    }

    /**
     * Adds the given source file to the given KotlinSourceSet in a multiplatform project.
     */
    fun addMultiplatformSource(sourceSet: String, name: String, contents: String) {
        require(name.endsWith(".kt")) { "multiplatform source extension needs to be *.kt." }
        val srcDir = multiplatformKotlinSourceDir(sourceSet)
        srcDir.resolve(name).writeText(contents)
    }

    private fun multiplatformKotlinSourceDir(sourceSet: String) = moduleRoot.resolve("src/$sourceSet/kotlin").also {
        it.mkdirs()
    }

    private val kotlinSourceDir
        get() = moduleRoot.resolve("src/main/kotlin").also {
            it.mkdirs()
        }

    private val javaSourceDir
        get() = moduleRoot.resolve("src/main/java").also {
            it.mkdirs()
        }

    private val kotlinTestSourceDir
        get() = moduleRoot.resolve("src/test/kotlin").also {
            it.mkdirs()
        }

    private val javaTestSourceDir
        get() = moduleRoot.resolve("src/test/java").also {
            it.mkdirs()
        }

    private val kotlinAndroidTestSourceDir
        get() = moduleRoot.resolve("src/androidTest/kotlin").also {
            it.mkdirs()
        }

    private val javaAndroidTestSourceDir
        get() = moduleRoot.resolve("src/androidTest/java").also {
            it.mkdirs()
        }

    private val servicesDir
        get() = moduleRoot.resolve("src/main/resources/META-INF/services/").also {
            it.mkdirs()
        }

    val kspServicesFile
        get() = servicesDir.resolve("com.google.devtools.ksp.processing.SymbolProcessorProvider")

    private val buildFile
        get() = moduleRoot.resolve("build.gradle.kts")

    /**
     * Writes the build file.
     */
    fun writeBuildFile() {
        val contents = buildString {
            appendln("plugins {")
            plugins.forEach { plugin ->
                appendln(plugin.toCode().prependIndent("    "))
            }
            appendln("}")
            appendln("dependencies {")
            dependencies.forEach { dependency ->
                appendln(dependency.toCode().prependIndent("    "))
            }
            appendln("}")
            buildFileAdditions.forEach {
                appendln(it)
            }
        }
        buildFile.writeText(contents)
    }
}
