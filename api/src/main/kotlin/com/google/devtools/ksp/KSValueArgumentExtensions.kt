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
package com.google.devtools.ksp

import com.google.devtools.ksp.symbol.AnnotationClass
import com.google.devtools.ksp.symbol.ArrayValue
import com.google.devtools.ksp.symbol.EnumClass
import com.google.devtools.ksp.symbol.ErrorValue
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSAnnotationValue
import com.google.devtools.ksp.symbol.KSBool
import com.google.devtools.ksp.symbol.KSByte
import com.google.devtools.ksp.symbol.KSChar
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDouble
import com.google.devtools.ksp.symbol.KSFloat
import com.google.devtools.ksp.symbol.KSInt
import com.google.devtools.ksp.symbol.KSLong
import com.google.devtools.ksp.symbol.KSShort
import com.google.devtools.ksp.symbol.KSString
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSUByte
import com.google.devtools.ksp.symbol.KSUInt
import com.google.devtools.ksp.symbol.KSULong
import com.google.devtools.ksp.symbol.KSUShort
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.Primitive
import com.google.devtools.ksp.symbol.ReflectionClassReference

fun KSValueArgument.typedValue(): KSAnnotationValue = value.toKSAnnotationValue()

private fun Any?.toKSAnnotationValue(): KSAnnotationValue =
    when (this) {
        is Boolean -> Primitive(KSBool(this))
        is Byte -> Primitive(KSByte(this))
        is Short -> Primitive(KSShort(this))
        is Int -> Primitive(KSInt(this))
        is Long -> Primitive(KSLong(this))
        is Float -> Primitive(KSFloat(this))
        is Double -> Primitive(KSDouble(this))
        is Char -> Primitive(KSChar(this))
        is String -> Primitive(KSString(this))
        is UByte -> Primitive(KSUByte(this))
        is UShort -> Primitive(KSUShort(this))
        is UInt -> Primitive(KSUInt(this))
        is ULong -> Primitive(KSULong(this))
        is KSType -> ReflectionClassReference(this)
        is KSClassDeclaration -> EnumClass(this)
        is KSAnnotation -> AnnotationClass(this)
        is Array<*> -> ArrayValue(map { it.toKSAnnotationValue() }.toTypedArray())
        is Collection<*> -> ArrayValue(map { it.toKSAnnotationValue() }.toTypedArray())
        else -> ErrorValue("Unexpected annotation value type: ${this?.javaClass?.name ?: "null"}", this)
    }
