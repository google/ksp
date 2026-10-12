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

// TEST PROCESSOR: NestedAnnotationDeclarationOrderProcessor
// EXPECTED:
// MyAnnotation direct: [a, b, c, <init>]
// MyAnnotation source: [a, b, c, <init>]
// MyNestedAnnotation direct: [a, b, c, <init>]
// MyNestedAnnotation source: [a, b, c, <init>]
// END

@MyAnnotation(a = "a", b = "b", c = "c")
@MyObject.MyNestedAnnotation(a = "a", b = "b", c = "c")
class MyClass

annotation class MyAnnotation(
    val a: String,
    val b: String,
    val c: String,
)

object MyObject {
    annotation class MyNestedAnnotation(
        val a: String,
        val b: String,
        val c: String,
    )
}
