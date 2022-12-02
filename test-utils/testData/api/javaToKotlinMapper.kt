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

// WITH_RUNTIME
// TEST PROCESSOR: JavaToKotlinMapProcessor
// EXPECTED:
// java.lang.String -> kotlin.String
// java.lang.Integer -> kotlin.Int
// java.util.List -> kotlin.collections.List, kotlin.collections.MutableList
// java.util.Map -> kotlin.collections.Map, kotlin.collections.MutableMap
// java.util.Set -> kotlin.collections.Set, kotlin.collections.MutableSet
// java.util.Map.Entry -> kotlin.collections.Map.Entry, kotlin.collections.MutableMap.MutableEntry
// java.util.ListIterator -> kotlin.collections.ListIterator, kotlin.collections.MutableListIterator
// java.util.Iterator -> kotlin.collections.Iterator, kotlin.collections.MutableIterator
// java.lang.Iterable -> kotlin.collections.Iterable, kotlin.collections.MutableIterable
// java.lang.Void -> null
// kotlin.Throwable -> java.lang.Throwable
// kotlin.Int -> java.lang.Integer
// kotlin.Nothing -> java.lang.Void
// kotlin.IntArray -> null
// END

val unused = Unit
