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

package com.google.devtools.ksp.common

// TODO: garbage collect underlying sequence after exhaust.
class MemoizedSequence<T>(sequence: Sequence<T>) : Sequence<T> {

    private val cache = arrayListOf<T>()

    private val iter: Iterator<T> by lazy {
        sequence.iterator()
    }

    private inner class CachedIterator() : Iterator<T> {
        var idx = 0
        override fun hasNext(): Boolean {
            return idx < cache.size || iter.hasNext()
        }

        override fun next(): T {
            if (idx == cache.size) {
                cache.add(iter.next())
            }
            val value = cache[idx]
            idx += 1
            return value
        }
    }

    override fun iterator(): Iterator<T> {
        return CachedIterator()
    }
}
