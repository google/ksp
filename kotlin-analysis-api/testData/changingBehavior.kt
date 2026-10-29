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

// TEST PROCESSOR: ChangingBehaviorProcessor
// PROCESSOR INPUT: Anno, FieldAnno, PropAnno
// EXPECTED:
// enableNewFeatures = false
// Resolver.getSymbolsWithAnnotation("Anno") = MyClass.x
// Resolver.getSymbolsWithAnnotation("FieldAnno") = MyClass.x
// Resolver.getSymbolsWithAnnotation("PropAnno") = MyClass.x
// MyClass.x.annotations = Anno, FieldAnno, PropAnno
// MyClass.x.modifiers = FINAL, PUBLIC
// enableNewFeatures = true
// Resolver.getSymbolsWithAnnotation("Anno") = MyClass.x
// Resolver.getSymbolsWithAnnotation("FieldAnno") = MyClass.x.field
// Resolver.getSymbolsWithAnnotation("PropAnno") = MyClass.x
// MyClass.x.annotations = Anno, PropAnno
// MyClass.x.modifiers = FINAL, PUBLIC
// MyClass.x.field.annotations = FieldAnno
// MyClass.x.field.modifiers = FINAL, PRIVATE
// END

// FILE: Anno.kt

annotation class Anno

// FILE: FieldAnno.kt

annotation class FieldAnno

// FILE: PropAnno.kt

annotation class PropAnno

// FILE: MyClass.kt

class MyClass {
    @Anno
    @field:FieldAnno
    @property:PropAnno
    val x = 42
}
