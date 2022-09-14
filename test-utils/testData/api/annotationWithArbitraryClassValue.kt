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
// TEST PROCESSOR: AnnotationArbitraryClassValueProcessor
// EXPECTED:
// User
// String, Company, IntArray, Array<User>
// END
// FILE: a.kt
package com.google.devtools.ksp.processor

import kotlin.reflect.KClass

annotation class ClassValueAnnotation(
    val classValue: KClass<*>,
    val classValues: Array<KClass<*>>)

data class User(val id: Long, val name: String)
data class Company(val id: Long, val name: String, val location: String)

@ClassValueAnnotation(
    classValue = User::class,
    classValues = [String::class, Company::class, IntArray::class, Array<User>::class]
)
class ClassValueAnnotated
