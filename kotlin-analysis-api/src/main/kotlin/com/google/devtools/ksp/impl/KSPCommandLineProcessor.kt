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

package com.google.devtools.ksp.impl

import com.google.devtools.ksp.KspOptions
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File
import java.nio.file.Files

class KSPCommandLineProcessor(val args: Array<String>) {
    val compilerConfiguration = CompilerConfiguration()
    // TODO: support KSP options
    val sources = args.toList()
    lateinit var kspOptions: KspOptions

    val ktFiles = sources
        .map { File(it) }
        .sortedBy { Files.isSymbolicLink(it.toPath()) } // Get non-symbolic paths first
        .flatMap { root -> root.walk().filter { it.isFile && it.extension == "kt" }.toList() }
        .sortedBy { Files.isSymbolicLink(it.toPath()) }
        .distinctBy { it.canonicalPath }

    val javaFiles = sources
        .map { File(it) }
        .sortedBy { Files.isSymbolicLink(it.toPath()) } // Get non-symbolic paths first
        .flatMap { root -> root.walk().filter { it.isFile && it.extension == "java" }.toList() }
        .sortedBy { Files.isSymbolicLink(it.toPath()) }
        .distinctBy { it.canonicalPath }

    init {
        compilerConfiguration.addKotlinSourceRoots(ktFiles.map { it.absolutePath })
        compilerConfiguration.addJavaSourceRoots(javaFiles)
        compilerConfiguration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

        val kspOptionsBuilder = KspOptions.Builder()
        kspOptions = kspOptionsBuilder.build()
    }
}
