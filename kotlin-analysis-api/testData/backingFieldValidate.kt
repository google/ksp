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
// TEST PROCESSOR: BackingFieldValidateProcessor
// PROCESSOR INPUT: com.example.lib
// EXPECTED:
// EXPECT ENABLE_BACKING_FIELDS = FALSE: decl ClassWithInvalidExplicitField valid=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: decl ClassWithInvalidExplicitField valid=false
// decl LibClass valid=true
// decl libValidExplicitProp valid=true
// decl srcInvalidExplicitFieldAnno valid=false
// EXPECT ENABLE_BACKING_FIELDS = FALSE: decl srcInvalidExplicitFieldType valid=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: decl srcInvalidExplicitFieldType valid=false
// EXPECT ENABLE_BACKING_FIELDS = FALSE: decl srcInvalidExplicitFieldTypeArg valid=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: decl srcInvalidExplicitFieldTypeArg valid=false
// decl srcInvalidFieldUseSiteAnno valid=false
// decl srcValidExplicitProp valid=true
// decl srcValidImplicitProp valid=true
// predicate srcInvalidExplicitFieldType skipPropertyScope=true skipBackingFieldChild=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: field ClassWithInvalidExplicitField.memberProp.field valid=false threwWhenDisabled=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: field LibClass.memberProp.field valid=true threwWhenDisabled=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: field libValidExplicitProp.field valid=true threwWhenDisabled=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: field srcInvalidExplicitFieldAnno.field valid=false threwWhenDisabled=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: field srcInvalidExplicitFieldType.field valid=false threwWhenDisabled=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: field srcInvalidExplicitFieldTypeArg.field valid=false threwWhenDisabled=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: field srcInvalidFieldUseSiteAnno.field valid=false threwWhenDisabled=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: field srcValidExplicitProp.field valid=true threwWhenDisabled=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: field srcValidImplicitProp.field valid=true threwWhenDisabled=true
// END

// MODULE: lib
// FILE: Lib.kt
package com.example.lib

val libValidExplicitProp: List<String>
    field = mutableListOf("")

class LibClass {
    val memberProp: Int = 1
}

// MODULE: main(lib)
// FILE: Main.kt

val srcValidExplicitProp: List<String>
    field = mutableListOf("")

val srcValidImplicitProp: Int = 1

val srcInvalidExplicitFieldType: Any
    field: NonExistentType

val srcInvalidExplicitFieldTypeArg: List<Any>
    field: MutableList<NonExistentType> = mutableListOf()

@field:NonExistentAnno
val srcInvalidFieldUseSiteAnno: Int = 1

val srcInvalidExplicitFieldAnno: List<Int>
    @NonExistentAnno field = mutableListOf<Int>()

class ClassWithInvalidExplicitField {
    val memberProp: Any
        field: NonExistentType
}
