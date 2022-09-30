/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package com.google.devtools.ksp.gradle

import java.io.File

/**
 * A simplified version of CommonComilerArguments.
 */
open class KotlinCompilerArguments {
    open var freeArgs: List<String> = emptyList()
    open var verbose: Boolean = false
    open var allWarningsAsErrors: Boolean = false
    open var languageVersion: String? = null
    open var apiVersion: String? = null
    open var useK2: Boolean = false
    open var incrementalCompilation: Boolean = false
    open var pluginOptions: List<String> = emptyList()
    open var pluginClasspaths: List<File> = emptyList()
    open var multiPlatform: Boolean = false
    open var expectActualLinker: Boolean = false

    // A.K.A. friend modules
    open var friendPaths: List<File> = emptyList()

    // A.K.A. classpath
    open var libraries: List<File> = emptyList()

    // A.K.A. output, output file
    open var destination: File? = null
}

/**
 * A simplified version of K2JVMComilerArguments.
 */
class KotlinJvmCompilerArguments : KotlinCompilerArguments() {
    var noJdk: Boolean = false
    var noStdlib: Boolean = false
    var noReflect: Boolean = false
    var moduleName: String? = null
    var jvmTarget: String? = null
    var jdkRelease: String? = null
    var allowNoSourceFiles: Boolean = false

    var javaSourceRoots: List<File> = emptyList()
}

/**
 * A simplified version of K2JSComilerArguments.
 */
class KotlinJsCompilerArguments : KotlinCompilerArguments() {
    var noStdlib: Boolean = false
    var irOnly: Boolean = false
    var irProduceJs: Boolean = false
    var irProduceKlibDir: Boolean = false
    var irProduceKlibFile: Boolean = false
    var irBuildCache: Boolean = false
    var wasm: Boolean = false
    var target: String = "v5"

    override var multiPlatform = true
}

/**
 * A simplified version of K2MetadataComilerArguments.
 */
class KotlinMetadataCompilerArguments : KotlinCompilerArguments() {
    override var multiPlatform = true
    override var expectActualLinker = true
}

/**
 * A simplified version of K2NativeComilerArguments.
 */
class KotlinNativeCompilerArguments : KotlinCompilerArguments() {
    var target: String? = null
    override var multiPlatform = true
}
