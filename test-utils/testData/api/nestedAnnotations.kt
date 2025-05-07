/*
 * Copyright 2023 Google LLC
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// TEST PROCESSOR: NestedAnnotationProcessor
// EXPECTED:
// @param: @MyNestedAnnotation: MyNestedAnnotation
// @field @MyNestedAnnotation: MyNestedAnnotation
// @property: @MyNestedAnnotation: MyNestedAnnotation
// @setparam: @MyNestedAnnotation: MyNestedAnnotation
// END

class MyClass(@param:MyNestedAnnotation param: String) {
    @field:MyNestedAnnotation
    val field: String = TODO()

    @property:MyNestedAnnotation
    @setparam:MyNestedAnnotation
    var property: String = TODO()
    annotation class MyNestedAnnotation
}

