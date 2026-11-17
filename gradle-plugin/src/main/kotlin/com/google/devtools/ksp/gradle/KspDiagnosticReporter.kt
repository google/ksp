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

package com.google.devtools.ksp.gradle

import javax.tools.DiagnosticListener
import javax.tools.JavaFileObject
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

internal interface KspDiagnosticReporter : DiagnosticListener<JavaFileObject> {
    fun flush() {}
}

internal data class BuildToolsApiSetting(val property: String, val defaultValue: Boolean)

internal fun buildToolsApiSetting(
    platform: KotlinPlatformType,
    kgpVersion: String,
): BuildToolsApiSetting? =
    when (platform) {
        KotlinPlatformType.jvm,
        KotlinPlatformType.androidJvm ->
            BuildToolsApiSetting("kotlin.compiler.runViaBuildToolsApi", true)
        KotlinPlatformType.js,
        KotlinPlatformType.wasm,
        KotlinPlatformType.common -> {
            if (KotlinToolingVersion(kgpVersion) < KotlinToolingVersion("2.4.20")) {
                null
            } else {
                val target =
                    when (platform) {
                        KotlinPlatformType.js -> "js"
                        KotlinPlatformType.wasm -> "wasm"
                        else -> "metadata"
                    }
                BuildToolsApiSetting("kotlin.$target.runViaBuildToolsApi", false)
            }
        }
        KotlinPlatformType.native -> null
    }

internal object KspDiagnosticReporterFactory {
    fun isSupported(classLoader: ClassLoader): Boolean {
        return try {
            listOf(
                "org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer",
                "org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer\$SourceLocation",
            ).forEach { Class.forName(it, false, classLoader) }
            true
        } catch (_: ClassNotFoundException) {
            false
        } catch (_: LinkageError) {
            false
        }
    }

    fun create(
        useBuildToolsApi: Boolean,
        logLevel: Int,
    ): KspDiagnosticReporter? {
        val classLoader = javaClass.classLoader
        if (!useBuildToolsApi || !isSupported(classLoader)) return null
        // Load optional BTA types only after checking the compilation's capabilities.
        return classLoader.loadClass("com.google.devtools.ksp.gradle.BtaKspDiagnosticReporter")
            .asSubclass(KspDiagnosticReporter::class.java)
            .getConstructor(Int::class.javaPrimitiveType)
            .newInstance(logLevel)
    }
}
