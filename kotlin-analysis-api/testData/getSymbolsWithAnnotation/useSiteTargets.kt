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
// PROCESSOR INPUT: com.example.Anno, com.example.FileAnno
// EXPECTED:
// com.example.Anno: TargetClass
// com.example.Anno: TargetClass.<init>.allUseSite
// com.example.Anno: TargetClass.<init>.noUseSite
// com.example.Anno: TargetClass.<init>.paramAndPropertyUseSite
// com.example.Anno: TargetClass.<init>.paramUseSite
// com.example.Anno: TargetClass.allUseSite
// com.example.Anno: TargetClass.allUseSite.allUseSite.getter()
// com.example.Anno: TargetClass.allUseSite.allUseSite.setter()
// com.example.Anno: TargetClass.allUseSite.allUseSite.setter().value
// com.example.Anno: TargetClass.delegateUseSite
// com.example.Anno: TargetClass.fieldUseSite
// com.example.Anno: TargetClass.getAndSetUseSite.getAndSetUseSite.getter()
// com.example.Anno: TargetClass.getAndSetUseSite.getAndSetUseSite.setter()
// com.example.Anno: TargetClass.getterUseSite.getterUseSite.getter()
// com.example.Anno: TargetClass.noUseSite
// com.example.Anno: TargetClass.nonParamValAllUseSite
// com.example.Anno: TargetClass.nonParamValAllUseSite.nonParamValAllUseSite.getter()
// com.example.Anno: TargetClass.nonParamVarAllUseSite
// com.example.Anno: TargetClass.nonParamVarAllUseSite.nonParamVarAllUseSite.getter()
// com.example.Anno: TargetClass.nonParamVarAllUseSite.nonParamVarAllUseSite.setter()
// com.example.Anno: TargetClass.nonParamVarAllUseSite.nonParamVarAllUseSite.setter().value
// com.example.Anno: TargetClass.paramAndPropertyUseSite
// com.example.Anno: TargetClass.propertyUseSite
// com.example.Anno: TargetClass.setParamUseSiteOnSetParam.setParamUseSiteOnSetParam.setter().boolSetterParameter
// com.example.Anno: TargetClass.setParamUseSiteOnVar.setParamUseSiteOnVar.setter().value
// com.example.Anno: TargetClass.setterUseSite.setterUseSite.setter()
// com.example.Anno: nothingFun.ReceiverFunClass
// com.example.Anno: nothingProp.ReceiverPropClass
// com.example.FileAnno: File: TargetFile.kt
// END

// FILE: Anno.kt

package com.example

annotation class Anno
annotation class FileAnno

// FILE: TargetFile.kt

import com.example.Anno
import com.example.FileAnno

@file:FileAnno

@Anno
class TargetClass(
    @Anno val noUseSite: Int,
    @param:Anno val paramUseSite: String,
    @property:Anno val propertyUseSite: Boolean,
    @get:Anno val getterUseSite: Boolean,
    @set:Anno var setterUseSite: Boolean,
    @get:Anno @set:Anno var getAndSetUseSite: Boolean,
    @field:Anno val fieldUseSite: Float,
    @all:Anno var allUseSite: Char,
    @param:Anno @property:Anno val paramAndPropertyUseSite: Long,
) {

    @all:Anno
    var nonParamVarAllUseSite: String

    @all:Anno
    val nonParamValAllUseSite: String

    @setparam:Anno
    var setParamUseSiteOnVar = 42

    var setParamUseSiteOnSetParam: Boolean
        set(@setparam:Anno boolSetterParameter) {
            field = boolSetterParameter
        }

    @delegate:Anno val delegateUseSite: Boolean by lazy { true }
}

class ReceiverFunClass

class ReceiverPropClass

@receiver:Anno
fun ReceiverFunClass.nothingFun() {}

@receiver:Anno
val ReceiverPropClass.nothingProp: Unit get() = Unit
