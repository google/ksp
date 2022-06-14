/*
 * Copyright 2020 Google LLC
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

package com.google.devtools.ksp.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * A Gradle extension to configure KSP.
 */
open class KspExtension {
    private val apOptions = mutableMapOf<String, String>()
    internal val commandLineArgumentProviders = mutableListOf<CommandLineArgumentProvider>()

    private val sourceSetOptions = mutableMapOf<String, SourceSetOptions>()

    // Some options have a global and a source set-specific variant. The latter, if specified, overrides the former.
    // The following is necessary due to KotlinSourceSet not being extension-aware:
    // - A KotlinSourceSet receiver addresses the source set-specific variant if the `ksp { ... }` block is invoked
    //   inside a source set block.
    // - A Project receiver addresses the corresponding global variant. (This is required as the compiler's name
    //   resolution would always prefer a receiver-less member and never invoke an extension receiver variant.)
    // - A corresponding private property provides the global variant's backing field.

    /** Options passed to the processor (global). */
    private val arguments: Map<String, String> get() = this@KspExtension.apOptions.toMap()

    /** Options passed to the processor (global). */
    open val Project.arguments: Map<String, String> get() = this@KspExtension.arguments

    /** Options passed to the processor (source set-specific). */
    open val KotlinSourceSet.arguments: Map<String, String>
        get() = sourceSetOptions(this).apOptions.toMap()

    /** Specifies an option passed to the processor (global). */
    open fun Project.arg(k: String, v: String) {
        if ('=' in k) {
            throw GradleException("'=' is not allowed in custom option's name.")
        }
        apOptions[k] = v
    }

    /** Specifies an option passed to the processor (source set-specific). */
    open fun KotlinSourceSet.arg(k: String, v: String) = with(sourceSetOptions(this)) {
        if ('=' in k) {
            throw GradleException("'=' is not allowed in custom option's name.")
        }
        apOptions[k] = v
    }

    /** Specifies a command line arguments provider (global). */
    open fun arg(arg: CommandLineArgumentProvider) {
        commandLineArgumentProviders.add(arg)
    }

    /** Block other compiler plugins by removing them from the classpath (global option). */
    open var blockOtherCompilerPlugins: Boolean = false

    /**
     * Instruct KSP to pickup sources from compile tasks, instead of source sets (global option).
     * Note that it depends on behaviors of other Gradle plugins, that may bring surprises and can be hard to debug.
     * Use your discretion.
     */
    open var allowSourcesFromOtherPlugins: Boolean = false

    /** Treat all warnings as errors (global option). */
    private var allWarningsAsErrors: Boolean = false

    /** Treat all warnings as errors (global option). */
    open var Project.allWarningsAsErrors: Boolean
        get() = this@KspExtension.allWarningsAsErrors
        set(value) {
            this@KspExtension.allWarningsAsErrors = value
        }

    /** Treat all warnings as errors (source set-specific option). */
    open var KotlinSourceSet.allWarningsAsErrors: Boolean
        get() = sourceSetOptions(this).allWarningsAsErrors ?: this@KspExtension.allWarningsAsErrors
        set(value) = with(sourceSetOptions(this)) { allWarningsAsErrors = value }

    /** Specify if this set of source set options is inheritable for dependent source sets (true by default). */
    open var KotlinSourceSet.inheritable: Boolean
        get() = sourceSetOptions(this).inheritable
        set(value) = with(sourceSetOptions(this)) { inheritable = value }

    /** Specify if KSP processing is enabled (source set-specific option). */
    open var KotlinSourceSet.enabled: Boolean
        get() = sourceSetOptions(this).enabled ?: false
        set(value) = with(sourceSetOptions(this)) { enabled = value }

    /** Specify the source set's KSP processor (enables KSP processing, if set). */
    open fun KotlinSourceSet.processor(dependencyNotation: Any) {
        sourceSetOptions(this).processor = dependencyNotation
        sourceSetOptions(this).enabled = true
    }

    internal fun sourceSetOptions(sourceSet: KotlinSourceSet): SourceSetOptions =
        sourceSetOptions.computeIfAbsent(sourceSet.name) { SourceSetOptions() }

    internal fun globalSourceSetOptions(): SourceSetOptions = SourceSetOptions().also {
        it.inheritable = true
        it.enabled = false
        it.apOptions = apOptions
        it.allWarningsAsErrors = allWarningsAsErrors
    }
}

/**
 * Source set-specific options.
 *
 * If [inheritable] is true (the default), a lower-level source set's option with a null value will inherit its
 * values from its inheritable parent, while [apOptions] / [arguments] will inherit all key/value pairs for keys
 * that are not already present. The [inheritable] property is not inheritable ;-), its purpose is to optionally
 * disable inheritance for one level only.
 */
internal data class SourceSetOptions(
    /** Specify if this set of source set options is inheritable for dependent source sets. */
    internal var inheritable: Boolean = true,

    /** Specify if KSP processing is enabled. */
    internal var enabled: Boolean? = null,

    /** Specify the source set's KSP processor. */
    internal var processor: Any? = null,

    /** Options passed to the processor. */
    internal var apOptions: MutableMap<String, String> = mutableMapOf(),

    /** Treat all warnings as errors. */
    internal var allWarningsAsErrors: Boolean? = null,
) {
    /** Options passed to the processor. */
    internal val arguments: Map<String, String> get() = apOptions.toMap()

    /** Inherits options from [other]. */
    internal fun inheritFrom(other: SourceSetOptions, initializationMode: Boolean = false): SourceSetOptions {
        require(initializationMode || other.inheritable)

        enabled = enabled ?: other.enabled
        processor = processor ?: other.processor
        apOptions = (other.apOptions + apOptions).toMutableMap()
        allWarningsAsErrors = allWarningsAsErrors ?: other.allWarningsAsErrors

        return this
    }
}
