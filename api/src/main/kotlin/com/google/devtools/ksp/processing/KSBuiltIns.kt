/*
 * Copyright 2010-2020 Google LLC, JetBrains s.r.o and and Kotlin Programming Language contributors.
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


package com.google.devtools.ksp.processing

import com.google.devtools.ksp.symbol.KSType

interface KSBuiltIns {
    /**
     * Common Standard Library types. Use [Resolver.getClassDeclarationByName] for other types.
     */
    val anyType: KSType
    val nothingType: KSType
    val unitType: KSType
    val numberType: KSType
    val byteType: KSType
    val shortType: KSType
    val intType: KSType
    val longType: KSType
    val floatType: KSType
    val doubleType: KSType
    val charType: KSType
    val booleanType: KSType
    val stringType: KSType
    val iterableType: KSType
    val annotationType: KSType
    val arrayType: KSType
}