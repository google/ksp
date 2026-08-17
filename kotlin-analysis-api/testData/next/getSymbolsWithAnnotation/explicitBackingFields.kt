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

// TEST PROCESSOR: GetSymbolsWithAnnotationProcessor
// PROCESSOR INPUT: Anno
// EXPECTED:
// Anno: MyClass.annotatedProperty
// Anno: MyClass.annotatedPropertyAndFieldViaUseSiteTargets
// Anno: MyClass.annotatedPropertyAndFieldViaUseSiteTargets.field
// Anno: MyClass.annotatedPropertyWithFieldTarget.field
// Anno: MyClass.propertyWhereFieldIsDirectlyAnnotated.field
// Anno: MyClass.propertyWithAllTarget
// Anno: MyClass.propertyWithAllTarget.field
// Anno: MyClass.propertyWithAllTarget.propertyWithAllTarget.getter()
// END

// FILE: Main.kt

annotation class Anno

class MyClass {
    @Anno // Should just pick the property
    val annotatedProperty: List<String>
        field = MutableList<String>

    val propertyWhereFieldIsDirectlyAnnotated: List<String>
        @Anno field = MutableList<String>

    @field:Anno
    val annotatedPropertyWithFieldTarget: List<String>
        field = MutableList<String>

    @field:Anno
    @property:Anno
    val annotatedPropertyAndFieldViaUseSiteTargets: List<String>
        field = MutableList<String>

    @all:Anno
    val propertyWithAllTarget: List<Int>
        field = MutableList<Int>
}
