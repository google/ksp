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
// TEST PROCESSOR: ErrorTypeProcessor
// EXPECTED:
// ERROR TYPE
// kotlin.collections.Map
// kotlin.String
// ERROR TYPE
// errorInComponent is assignable from errorAtTop: false
// errorInComponent is assignable from class C: false
// Any is assignable from errorInComponent: true
// class C is assignable from errorInComponent: false
// Any is assignable from class C: true
// Cls's super type is Error type: true
// Cls's annotation is Error type: true
// END
// FILE: a.kt
class C {
    val errorAtTop = mutableMapOf<String, NonExistType, Int>()
    val errorInComponent: Map<String, NonExistType>
}

// FILE: Cls.java

@NonExistingAnnotation
public class Cls extends NonExistType {

}
