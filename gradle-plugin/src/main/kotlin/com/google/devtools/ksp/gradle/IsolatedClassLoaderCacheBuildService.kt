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
import java.util.concurrent.ConcurrentHashMap

abstract class IsolatedClassLoaderCacheBuildService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    companion object {
        const val KEY = "IsolatedClassLoaderCacheBuildService"
    }
    val isolatedClassLoaderCache = ConcurrentHashMap<String, URLClassLoader>()

    override fun close() = isolatedClassLoaderCache.clear()
}
