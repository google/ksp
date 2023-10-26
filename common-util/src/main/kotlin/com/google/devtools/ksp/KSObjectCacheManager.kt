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

package com.google.devtools.ksp

class KSObjectCacheManager {
    companion object {
        private val caches_prop = object : ThreadLocal<ArrayList<KSObjectCache<*, *>>>() {
            override fun initialValue(): ArrayList<KSObjectCache<*, *>> {
                return ArrayList()
            }
        }
        val caches
            get() = caches_prop.get()

        fun register(cache: KSObjectCache<*, *>) = caches.add(cache)
        fun clear() = caches.forEach { it.clear() }
    }
}

abstract class KSObjectCache<K, V> {
    private val cache_prop = ThreadLocal<MutableMap<K, V>>()

    val cache: MutableMap<K, V>
        get() {
            if (cache_prop.get() == null) {
                KSObjectCacheManager.register(this)
                cache_prop.set(mutableMapOf())
            }
            return cache_prop.get()
        }

    open fun clear() = cache.clear()
}
