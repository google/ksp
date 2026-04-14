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

package com.google.devtools.ksp.common

/**
 * Applies [transform] to each key-value pair, where the value is a collection,
 * and merges the resulting collections of values.
 *
 * It will exclude any key-value pair where `transform(key)` is `null`.
 *
 * In other words, if `k1 -> [a, b, c], k2 -> [c, d, e]` and
 * `transform(k1) = transform(k2) = k3`, then the result is
 * `k3 -> [a, b, c, d, e]`.
 * Note the list is deduplicated.
 */
internal fun <K, V, R> Map<K, Lazy<Collection<V>>>.mergeMapNotNullKeys(
    transform: (K) -> R?
): Map<R, Lazy<Collection<V>>> =
    mutableMapOf<R, Lazy<Collection<V>>>().apply {
        this@mergeMapNotNullKeys.forEach {
            transform(it.key)?.let { newKey ->
                if (newKey in this) {
                    // N.B.: Store the old value in a variable so the value is captured in the lazy
                    // lambda, instead of capturing the indexing expression `this[newKey]`, which
                    // by recursion results in stack overflow.
                    val old = this[newKey]!!
                    this[newKey] = lazy { old.value.plus(it.value.value).toSet() }
                } else {
                    this[newKey] = it.value
                }
            }
        }
    }

/**
 * Merges the key-value pairs of `this` and `other`, deduplicating values.
 *
 * In other words, if `this = k -> [a, b, c]` and `other = k -> [c, d, e]`,
 * then the result is `k -> [a, b, c, d, e]`.
 */
internal fun <K, V> Map<K, Collection<V>>.merge(
    other: Map<K, Collection<V>>
): Map<K, Collection<V>> =
    mutableMapOf<K, MutableSet<V>>().apply {
        this@merge.forEach { (k, vs) ->
            this@apply.getOrPut(k, ::mutableSetOf).addAll(vs)
        }
        other.forEach { (k, vs) ->
            this@apply.getOrPut(k, ::mutableSetOf).addAll(vs)
        }
    }
