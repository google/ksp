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
// PROCESSOR INPUT: Anno1, Anno2, Anno3
// EXPECTED:
// Anno1: File: Main.kt
// Anno1: File: Other.kt
// Anno1: MyClass
// Anno1: MyClass.<init>.mixedStyleWithAnno1AndAnno2AndAnno3
// Anno1: MyClass.<init>.paramAndPropertyWithOnlyAnno1
// Anno1: MyClass.<init>.paramWithAnno1AndAnno3
// Anno1: MyClass.mixedStyleWithAnno1AndAnno2AndAnno3
// Anno1: MyClass.paramAndPropertyWithOnlyAnno1
// Anno2: File: Main.kt
// Anno2: MyClass
// Anno2: MyClass.<init>.mixedStyleWithAnno1AndAnno2AndAnno3
// Anno2: MyClass.mixedStyleWithAnno1AndAnno2AndAnno3
// Anno3: File: Other.kt
// Anno3: MyClass
// Anno3: MyClass.<init>.mixedStyleWithAnno1AndAnno2AndAnno3
// Anno3: MyClass.<init>.paramWithAnno1AndAnno3
// Anno3: MyClass.mixedStyleWithAnno1AndAnno2AndAnno3
// END

// FILE: Main.kt

annotation class Anno1

annotation class Anno2

annotation class Anno3

@file:[Anno1 Anno2]
@[Anno1 Anno2 Anno3]
class MyClass(
    @param:[Anno1 Anno3] val paramWithAnno1AndAnno3,
    @[Anno1] val paramAndPropertyWithOnlyAnno1,
    @[Anno1] @[Anno2] @Anno3 val mixedStyleWithAnno1AndAnno2AndAnno3
)

// FILE: Other.kt

@file:[Anno1 Anno3]
