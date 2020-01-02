/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processing

import org.jetbrains.kotlin.ksp.symbol.KSType

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