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

// WITH_RUNTIME
// TEST PROCESSOR: AnnotationTypeArgumentsInLibraryProcessor
// EXPECTED:
// BinaryRetentionTarget.annotationType: BinaryRetentionAnno
// BinaryRetentionTarget.typeArgCount: 0
// RuntimeRetentionTarget.annotationType: RuntimeRetentionAnno
// RuntimeRetentionTarget.typeArgCount: 0
// END

// MODULE: lib
// FILE: LibraryAnnotations.kt
package com.example

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceRetentionAnno<A : Any>

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class BinaryRetentionAnno<A : Any>

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RuntimeRetentionAnno<A : Any>

class SourceRetentionValue
class BinaryRetentionValue
class RuntimeRetentionValue

@SourceRetentionAnno<SourceRetentionValue>
class SourceRetentionTarget

@BinaryRetentionAnno<BinaryRetentionValue>
class BinaryRetentionTarget

@RuntimeRetentionAnno<RuntimeRetentionValue>
class RuntimeRetentionTarget

// MODULE: main(lib)
// FILE: Main.kt
package com.example.main

class Main
