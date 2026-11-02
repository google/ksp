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

package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.AnnotationClass
import com.google.devtools.ksp.symbol.ArrayValue
import com.google.devtools.ksp.symbol.EnumEntry
import com.google.devtools.ksp.symbol.ErrorValue
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSAnnotationValue
import com.google.devtools.ksp.symbol.KSBool
import com.google.devtools.ksp.symbol.KSByte
import com.google.devtools.ksp.symbol.KSChar
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSDouble
import com.google.devtools.ksp.symbol.KSFloat
import com.google.devtools.ksp.symbol.KSInt
import com.google.devtools.ksp.symbol.KSLong
import com.google.devtools.ksp.symbol.KSPrimitiveType
import com.google.devtools.ksp.symbol.KSShort
import com.google.devtools.ksp.symbol.KSString
import com.google.devtools.ksp.symbol.KSUByte
import com.google.devtools.ksp.symbol.KSUInt
import com.google.devtools.ksp.symbol.KSULong
import com.google.devtools.ksp.symbol.KSUShort
import com.google.devtools.ksp.symbol.Primitive
import com.google.devtools.ksp.symbol.ReflectionClassReference
import com.google.devtools.ksp.typedValue

class AnnotationArrayTypedValueProcessor(
    private val annotationNames: List<String>,
    override val enableNewFeatures: Boolean,
) : AbstractTestProcessor() {
    private val results = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        annotationNames.forEach { annotationName ->
            resolver.getSymbolsWithAnnotation(annotationName).forEach { annotated ->
                val name = (annotated as? KSDeclaration)?.qualifiedName?.asString() ?: annotated.toString()
                annotated.annotations.filter {
                    it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationName
                }.forEach { annotation ->
                    annotation.arguments.forEach { argument ->
                        results.add("$name @$annotationName ${argument.name?.asString()} = ${render(argument.typedValue())}")
                    }
                }
            }
        }
        return emptyList()
    }

    override fun toResult(): List<String> = results.sorted()

    private fun render(value: KSAnnotationValue?): String = when (value) {
        null -> "null"
        is Primitive -> "Primitive(${render(value.type)})"
        is ReflectionClassReference -> "ReflectionClassReference(${value.type})"
        is EnumEntry -> "EnumEntry(${value.clazz.qualifiedName?.asString()})"
        is AnnotationClass -> "AnnotationClass(${render(value.anno)})"
        is ArrayValue -> value.values.joinToString(prefix = "ArrayValue[", postfix = "]") { render(it) }
        is ErrorValue -> "ErrorValue(${value.message}, ${value.value})"
    }

    private fun render(annotation: KSAnnotation): String {
        val name = annotation.annotationType.resolve().declaration.qualifiedName?.asString()
        return annotation.arguments.joinToString(prefix = "$name(", postfix = ")") {
            "${it.name?.asString()}=${render(it.typedValue())}"
        }
    }

    private fun render(value: KSPrimitiveType): String = when (value) {
        is KSBool -> "KSBool(${value.value})"
        is KSByte -> "KSByte(${value.value})"
        is KSShort -> "KSShort(${value.value})"
        is KSInt -> "KSInt(${value.value})"
        is KSLong -> "KSLong(${value.value})"
        is KSFloat -> "KSFloat(${value.value})"
        is KSDouble -> "KSDouble(${value.value})"
        is KSChar -> "KSChar('${value.value}')"
        is KSString -> "KSString(\"${value.value}\")"
        is KSUByte -> "KSUByte(${value.value})"
        is KSUShort -> "KSUShort(${value.value})"
        is KSUInt -> "KSUInt(${value.value})"
        is KSULong -> "KSULong(${value.value})"
    }
}
