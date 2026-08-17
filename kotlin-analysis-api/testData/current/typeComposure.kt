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
// TEST PROCESSOR: TypeComposureProcessor
// EXPECTED:
// C<*> ?= C<*> : true
// C<*> ?= C<Any> : true
// C<*> ?= C<Int> : true
// C<*> ?= C<Number> : true
// C<*> ?= C<in Any> : true
// C<*> ?= C<in Int> : true
// C<*> ?= C<in Number> : true
// C<*> ?= C<out Any> : true
// C<*> ?= C<out Int> : true
// C<*> ?= C<out Number> : true
// C<Any> ?= C<*> : false
// C<Any> ?= C<Any> : true
// C<Any> ?= C<Int> : false
// C<Any> ?= C<Number> : false
// C<Any> ?= C<in Any> : false
// C<Any> ?= C<in Int> : false
// C<Any> ?= C<in Number> : false
// C<Any> ?= C<out Any> : false
// C<Any> ?= C<out Int> : false
// C<Any> ?= C<out Number> : false
// C<Int> ?= C<*> : false
// C<Int> ?= C<Any> : false
// C<Int> ?= C<Int> : true
// C<Int> ?= C<Number> : false
// C<Int> ?= C<in Any> : false
// C<Int> ?= C<in Int> : false
// C<Int> ?= C<in Number> : false
// C<Int> ?= C<out Any> : false
// C<Int> ?= C<out Int> : false
// C<Int> ?= C<out Number> : false
// C<Number> ?= C<*> : false
// C<Number> ?= C<Any> : false
// C<Number> ?= C<Int> : false
// C<Number> ?= C<Number> : true
// C<Number> ?= C<in Any> : false
// C<Number> ?= C<in Int> : false
// C<Number> ?= C<in Number> : false
// C<Number> ?= C<out Any> : false
// C<Number> ?= C<out Int> : false
// C<Number> ?= C<out Number> : false
// C<in Any> ?= C<*> : false
// C<in Any> ?= C<Any> : true
// C<in Any> ?= C<Int> : false
// C<in Any> ?= C<Number> : false
// C<in Any> ?= C<in Any> : true
// C<in Any> ?= C<in Int> : false
// C<in Any> ?= C<in Number> : false
// C<in Any> ?= C<out Any> : false
// C<in Any> ?= C<out Int> : false
// C<in Any> ?= C<out Number> : false
// C<in Int> ?= C<*> : false
// C<in Int> ?= C<Any> : true
// C<in Int> ?= C<Int> : true
// C<in Int> ?= C<Number> : true
// C<in Int> ?= C<in Any> : true
// C<in Int> ?= C<in Int> : true
// C<in Int> ?= C<in Number> : true
// C<in Int> ?= C<out Any> : false
// C<in Int> ?= C<out Int> : false
// C<in Int> ?= C<out Number> : false
// C<in Number> ?= C<*> : false
// C<in Number> ?= C<Any> : true
// C<in Number> ?= C<Int> : false
// C<in Number> ?= C<Number> : true
// C<in Number> ?= C<in Any> : true
// C<in Number> ?= C<in Int> : false
// C<in Number> ?= C<in Number> : true
// C<in Number> ?= C<out Any> : false
// C<in Number> ?= C<out Int> : false
// C<in Number> ?= C<out Number> : false
// C<out Any> ?= C<*> : false
// C<out Any> ?= C<Any> : true
// C<out Any> ?= C<Int> : true
// C<out Any> ?= C<Number> : true
// C<out Any> ?= C<in Any> : false
// C<out Any> ?= C<in Int> : false
// C<out Any> ?= C<in Number> : false
// C<out Any> ?= C<out Any> : true
// C<out Any> ?= C<out Int> : true
// C<out Any> ?= C<out Number> : true
// C<out Int> ?= C<*> : false
// C<out Int> ?= C<Any> : false
// C<out Int> ?= C<Int> : true
// C<out Int> ?= C<Number> : false
// C<out Int> ?= C<in Any> : false
// C<out Int> ?= C<in Int> : false
// C<out Int> ?= C<in Number> : false
// C<out Int> ?= C<out Any> : false
// C<out Int> ?= C<out Int> : true
// C<out Int> ?= C<out Number> : false
// C<out Number> ?= C<*> : false
// C<out Number> ?= C<Any> : false
// C<out Number> ?= C<Int> : true
// C<out Number> ?= C<Number> : true
// C<out Number> ?= C<in Any> : false
// C<out Number> ?= C<in Int> : false
// C<out Number> ?= C<in Number> : false
// C<out Number> ?= C<out Any> : false
// C<out Number> ?= C<out Int> : true
// C<out Number> ?= C<out Number> : true
// END

open class C<T>

val a: Int = 0
val b: Number = 0
val c: C<Int> = C<Int>()
