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
// Anno1: MyClass.<init>.paramAndPropertyWithOnlyAnno1
// Anno1: MyClass.<init>.paramWithAnno1AndAnno3
// Anno1: MyClass.fieldUseSiteWithAnno1AndAnno3
// Anno1: MyClass.paramAndPropertyWithOnlyAnno1
// Anno1: MyClass.propertyUseSiteWithAnno1AndAnno2
// Anno1: MyClass.setParamUseSiteWithAnno1AndAnno3.setParamUseSiteWithAnno1AndAnno3.setter().value
// Anno1: MyClass.setterAndGetterUseSiteWithAnno1AndAnno2.setterAndGetterUseSiteWithAnno1AndAnno2.getter()
// Anno1: MyClass.setterAndGetterUseSiteWithAnno1AndAnno2.setterAndGetterUseSiteWithAnno1AndAnno2.setter()
// Anno1: MyClass.setterUseSiteWithAnno1AndAnno2.setterUseSiteWithAnno1AndAnno2.setter()
// Anno1: extFunWithAnno1AndAnno2.ReceiverClass
// Anno2: File: Main.kt
// Anno2: MyClass
// Anno2: MyClass.delegateUseSiteWithAnno2AndAnno3
// Anno2: MyClass.explicitSetParam.explicitSetParam.setter().valueWithAnno2
// Anno2: MyClass.getterUseSiteWithAnno2AndAnno3.getterUseSiteWithAnno2AndAnno3.getter()
// Anno2: MyClass.propertyUseSiteWithAnno1AndAnno2
// Anno2: MyClass.setterAndGetterUseSiteWithAnno1AndAnno2.setterAndGetterUseSiteWithAnno1AndAnno2.getter()
// Anno2: MyClass.setterAndGetterUseSiteWithAnno1AndAnno2.setterAndGetterUseSiteWithAnno1AndAnno2.setter()
// Anno2: MyClass.setterUseSiteWithAnno1AndAnno2.setterUseSiteWithAnno1AndAnno2.setter()
// Anno2: extFunWithAnno1AndAnno2.ReceiverClass
// Anno3: File: Other.kt
// Anno3: MyClass
// Anno3: MyClass.<init>.paramWithAnno1AndAnno3
// Anno3: MyClass.delegateUseSiteWithAnno2AndAnno3
// Anno3: MyClass.fieldUseSiteWithAnno1AndAnno3
// Anno3: MyClass.getterUseSiteWithAnno2AndAnno3.getterUseSiteWithAnno2AndAnno3.getter()
// Anno3: MyClass.setParamUseSiteWithAnno1AndAnno3.setParamUseSiteWithAnno1AndAnno3.setter().value
// Anno3: extPropWithAnno3.ReceiverClass
// END

// FILE: Main.kt

annotation class Anno1

annotation class Anno2

annotation class Anno3

@file:[Anno1 Anno2]
@[Anno1 Anno2 Anno3]
class MyClass(
    @param:[Anno1 Anno3] val paramWithAnno1AndAnno3: Int,
    @[Anno1] val paramAndPropertyWithOnlyAnno1: Int,
    @property:[Anno1 Anno2] val propertyUseSiteWithAnno1AndAnno2: Int,
    @field:[Anno1 Anno3] val fieldUseSiteWithAnno1AndAnno3: Int,
    @get:[Anno2 Anno3] val getterUseSiteWithAnno2AndAnno3: Int,
    @set:[Anno1 Anno2] var setterUseSiteWithAnno1AndAnno2: Int,
    @set:[Anno1 Anno2] @get:[Anno1 Anno2] var setterAndGetterUseSiteWithAnno1AndAnno2: Int,
) {
    @setparam:[Anno1 Anno3]
    var setParamUseSiteWithAnno1AndAnno3 = false

    var explicitSetParam: Int
        set(@setparam:[Anno2] valueWithAnno2) {
            field = valueWithAnno2
        }

    @delegate:[Anno2 Anno3] val delegateUseSiteWithAnno2AndAnno3: Int by lazy { 1 }
}

class ReceiverClass

@receiver:[Anno1 Anno2]
fun ReceiverClass.extFunWithAnno1AndAnno2() {}

@receiver:[Anno3]
val ReceiverClass.extPropWithAnno3: Int get() = 0

// FILE: Other.kt

@file:[Anno1 Anno3]
