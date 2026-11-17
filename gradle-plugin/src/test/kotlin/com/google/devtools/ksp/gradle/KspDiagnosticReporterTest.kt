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

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URI
import javax.tools.Diagnostic
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.junit.Test

class KspDiagnosticReporterTest {
    @Test
    fun btaCapabilitiesSelectTheAdapterWithoutProblems() {
        assertThat(KspDiagnosticReporterFactory.isSupported(javaClass.classLoader)).isTrue()
        for (hidden in listOf("org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer",
            "org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer$")) {
            val missingApi = hiding(hidden)
            assertThat(KspDiagnosticReporterFactory.isSupported(missingApi)).isFalse()
        }
        assertThat(KspDiagnosticReporterFactory.isSupported(hiding("org.gradle.api.problems."))).isTrue()
    }

    @Test
    fun factoryCreatesOnlyEnabledReporters() {
        assertThat(KspDiagnosticReporterFactory.create(true, LogLevel.WARN.ordinal)).isNotNull()
        assertThat(KspDiagnosticReporterFactory.create(false, LogLevel.WARN.ordinal)).isNull()
    }

    @Test
    fun renderingPreservesDecoratedBodiesAndFiltersOnlyConsoleOutput() {
        val output = ByteArrayOutputStream()
        val original = System.out
        try {
            System.setOut(PrintStream(output))
            val adapter = BtaKspDiagnosticReporter(LogLevel.WARN.ordinal)
            adapter.report(diagnostic(Diagnostic.Kind.WARNING, File("/tmp/Foo.kt").toURI(), 3, 11,
                "[ksp] /tmp/Foo.kt:3: warning body"))
            adapter.report(diagnostic(Diagnostic.Kind.MANDATORY_WARNING, message = "[ksp] mandatory"))
            adapter.report(diagnostic(Diagnostic.Kind.ERROR, message = "[ksp] source-free error"))
            adapter.report(diagnostic(Diagnostic.Kind.NOTE))
            adapter.flush()
            assertThat(output.toString().replace("\r\n", "\n")).isEqualTo(
                "w: [ksp] /tmp/Foo.kt:3: warning body\nw: [ksp] mandatory\ne: [ksp] source-free error\n")
            output.reset()
            val errorsOnly = BtaKspDiagnosticReporter(LogLevel.ERROR.ordinal)
            errorsOnly.report(diagnostic(Diagnostic.Kind.WARNING))
            errorsOnly.report(diagnostic(Diagnostic.Kind.ERROR))
            assertThat(output.toString().replace("\r\n", "\n")).isEqualTo("e: [ksp] diagnostic message\n")
        } finally {
            System.setOut(original)
        }
    }

    private fun hiding(prefix: String) = object : ClassLoader(javaClass.classLoader) {
        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            if (name.startsWith(prefix)) throw ClassNotFoundException(name)
            return super.loadClass(name, resolve)
        }
    }

    @Test
    fun targetSpecificActivationMatchesKgp() {
        for (platform in listOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm)) {
            assertThat(usesBta(platform, "2.4.0")).isTrue()
            assertThat(usesBta(platform, "2.4.20", "false")).isFalse()
        }
        for (platform in
            listOf(KotlinPlatformType.js, KotlinPlatformType.wasm, KotlinPlatformType.common)) {
            assertThat(usesBta(platform, "2.4.0", "true")).isFalse()
            assertThat(usesBta(platform, "2.4.10", "true")).isFalse()
            assertThat(usesBta(platform, "2.4.20")).isFalse()
            assertThat(usesBta(platform, "2.4.20", "true")).isTrue()
            assertThat(usesBta(platform, "2.4.20", "false")).isFalse()
        }
        assertThat(usesBta(KotlinPlatformType.native, "2.4.20", "true")).isFalse()
        assertThat(buildToolsApiSetting(KotlinPlatformType.js, "2.4.20")!!.property)
            .isEqualTo("kotlin.js.runViaBuildToolsApi")
        assertThat(buildToolsApiSetting(KotlinPlatformType.wasm, "2.4.20")!!.property)
            .isEqualTo("kotlin.wasm.runViaBuildToolsApi")
        assertThat(buildToolsApiSetting(KotlinPlatformType.common, "2.4.20")!!.property)
            .isEqualTo("kotlin.metadata.runViaBuildToolsApi")
    }

    @Test
    fun sourceConversionPreservesPositionsWithoutInventingRangesOrContents() {
        val file = File("/tmp/Ksp source.kt").absoluteFile
        val source =
            diagnostic(Diagnostic.Kind.WARNING, file.toURI(), 3, 11).toBtaSourceLocation()!!
        assertThat(source.path).isEqualTo(file.path)
        assertThat(source.line).isEqualTo(3)
        assertThat(source.column).isEqualTo(11)
        assertThat(source.lineEnd).isEqualTo(-1)
        assertThat(source.columnEnd).isEqualTo(-1)
        assertThat(source.lineContent).isNull()
        assertThat(diagnostic(Diagnostic.Kind.ERROR).toBtaSourceLocation()).isNull()
        assertThat(
                diagnostic(Diagnostic.Kind.ERROR, File("/tmp/Old.java").toURI(), 2)
                    .toBtaSourceLocation()!!
                    .column
            )
            .isEqualTo(-1)
    }

    private fun usesBta(
        platform: KotlinPlatformType,
        kgp: String,
        property: String? = null,
    ): Boolean {
        val setting = buildToolsApiSetting(platform, kgp) ?: return false
        return property?.toBoolean() ?: setting.defaultValue
    }

    private fun diagnostic(
        kind: Diagnostic.Kind,
        uri: URI? = null,
        line: Long = Diagnostic.NOPOS,
        column: Long = Diagnostic.NOPOS,
        message: String = "[ksp] diagnostic message",
    ): Diagnostic<JavaFileObject> =
        object : Diagnostic<JavaFileObject> {
            override fun getKind() = kind

            override fun getSource(): JavaFileObject? = uri?.let {
                object : SimpleJavaFileObject(it, JavaFileObject.Kind.SOURCE) {}
            }

            override fun getLineNumber() = line

            override fun getColumnNumber() = column

            override fun getPosition() = Diagnostic.NOPOS

            override fun getStartPosition() = Diagnostic.NOPOS

            override fun getEndPosition() = Diagnostic.NOPOS

            override fun getCode(): String? = null

            override fun getMessage(locale: java.util.Locale?) = message
        }
}
