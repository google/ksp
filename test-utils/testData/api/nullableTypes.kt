/*
 * Copyright 2021 Google LLC
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
// TEST PROCESSOR: NullableTypeProcessor
// EXPECTED:
// a: [], [@TA]
// b: [SUSPEND], [@TA]
// c: [], [@TA]
// d: [SUSPEND], [@TA]
// e: [], [@TA]
// f: [], [@TA]
// g: [], [@TA]
// h: [], [@TA]
// i: [], [@TA]
// j: [], [@TA]
// k: [], [@TA]
// END

// Workaround: force file to be resolved.
// Remove after https://github.com/JetBrains/kotlin/pull/5089 is merged.
@file:Suppress()

@Target(AnnotationTarget.TYPE)
annotation class TA

val a: @TA (() -> Unit)? = {}
val b: (@TA suspend () -> Unit)? = {}
val c: @TA (() -> Unit) = {}
val d: (@TA suspend () -> Unit) = {}
val e: (@TA String)?

// Parser doesn't allow `@TA (String)`
val f: (@TA String)? = ""
val g: (@TA String?) = ""
val h: (@TA String?)? = ""
val i: @TA String = ""
val j: (@TA String) = ""
val k: ((@TA String)?) = ""
