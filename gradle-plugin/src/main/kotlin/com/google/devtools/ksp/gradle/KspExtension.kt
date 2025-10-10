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

import com.google.devtools.ksp.KspExperimental
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.process.CommandLineArgumentProvider
import javax.inject.Inject

abstract class KspExtension @Inject constructor(project: Project) {
    /**
     * KSP1 is removed now so KSP2 is the only option
     * This is kept for backwards compatibility in the meantime
     * Setting this value to `false` will have no impact
     */
    @KspExperimental
    abstract val useKsp2: Property<Boolean>

    internal val apOptions = project.objects.mapProperty(String::class.java, String::class.java)
    internal val commandLineArgumentProviders = project.objects.listProperty(CommandLineArgumentProvider::class.java)
        .also { it.finalizeValueOnRead() }
    internal val excludedProcessors = project.objects.setProperty(String::class.java)
        .also { it.finalizeValueOnRead() }

    /**
     * Sources that should be excluded from processing by Kotlin Symbol Processors.
     *
     * If you have a task that generates sources, you can call `ksp.excludedSources.from(task)` for those sources
     * to be added to the file collection.
     */
    abstract val excludedSources: ConfigurableFileCollection

    /**
     * Returns a map with key/value arguments passed to Kotlin Symbol Processors.
     */
    open val arguments: Map<String, String> get() = apOptions.get()

    /**
     * Add a key/value pair to pass an argument to the processors.
     */
    open fun arg(k: String, v: String) {
        if ('=' in k) {
            throw GradleException("'=' is not allowed in custom option's name.")
        }
        apOptions.put(k, v)
    }

    /**
     * Add a key/value pair to pass an argument to the processors.
     */
    open fun arg(k: String, v: Provider<String>) {
        if ('=' in k) {
            throw GradleException("'=' is not allowed in custom option's name.")
        }
        apOptions.put(k, v)
    }

    /**
     * Add an argument in a form of "key=value" to pass to the processors.
     */
    open fun arg(arg: CommandLineArgumentProvider) {
        commandLineArgumentProviders.add(arg)
    }

    @Deprecated("No-op. KSP no longer reads this property")
    open var blockOtherCompilerPlugins: Boolean = true

    // Instruct KSP to pickup sources from compile tasks, instead of source sets.
    // Note that it depends on behaviors of other Gradle plugins, that may bring surprises and can be hard to debug.
    // Use your discretion.
    @Deprecated("This feature is broken in recent versions of Gradle and is no longer supported in KSP2.")
    open var allowSourcesFromOtherPlugins: Boolean = false

    /**
     * Treat all warnings as errors.
     */
    open var allWarningsAsErrors: Boolean = false

    /**
     * Exclude calling the processor provider with given class with [fullyQualifiedName]. By default, all
     * `com.google.devtools.ksp.processing.SymbolProcessorProvider` on the processor classpath are called.
     *
     * Note, the excluded providers will still be loaded, but not called.
     */
    open fun excludeProcessor(fullyQualifiedName: String) {
        excludedProcessors.add(fullyQualifiedName)
    }
}
