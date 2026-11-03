/*
 * Copyright 2026 Google LLC
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

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.net.URLClassLoader
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Identifies a processor classloader.
 *
 * Both classpaths take part in the identity: a processor classloader delegates to a parent loader
 * built from the KSP classpath, so two modules may resolve the same processor jars while resolving
 * different KSP jars, and those must not share a loader.
 *
 * Modelled as a data class rather than a concatenated string so that the two classpaths cannot be
 * confused with one another, and so no separator has to be reserved.
 */
internal data class ProcessorClassLoaderKey(
    val kspClasspath: List<String>,
    val processorClasspath: List<String>,
)

object IsolatedClassLoaderCache {
    val cache = ConcurrentHashMap<String, URLClassLoader>()

    /**
     * Cache of processor classloaders, keyed by the KSP classpath plus the processor classpath.
     *
     * In large multi-module builds the same processor jars are used by many modules. Creating (and
     * discarding) a [URLClassLoader] per module forces those classes to be re-loaded, re-verified
     * and re-JITted for every module, which is pure overhead. Reusing the loader keeps the JIT
     * profile warm across modules.
     *
     * This is opt-in via the `ksp.classloader.cache.processors` Gradle property, because reusing a
     * loader also extends the lifetime of any static state held by processors.
     */
    internal val processorCache = ConcurrentHashMap<ProcessorClassLoaderKey, URLClassLoader>()

    fun clear() {
        val classLoaders = cache.values + processorCache.values
        classLoaders.forEach { classLoader ->
            try {
                classLoader.close()
            } catch (e: Exception) {
                // Ignore exceptions during cleanup, but we could log them if a logger was available.
            }
        }
        cache.clear()
        processorCache.clear()
    }
}

abstract class IsolatedClassLoaderCacheBuildService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    companion object {
        val KEY = "IsolatedClassLoaderCacheBuildService_" + UUID.randomUUID().toString()
    }

    override fun close() {
        IsolatedClassLoaderCache.clear()
    }
}
