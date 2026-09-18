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

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.AnnotationClass
import com.google.devtools.ksp.symbol.ArrayValue
import com.google.devtools.ksp.symbol.EnumClass
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSInt
import com.google.devtools.ksp.symbol.KSString
import com.google.devtools.ksp.symbol.Primitive
import com.google.devtools.ksp.symbol.ReflectionClassReference
import com.google.devtools.ksp.typedValue

class AnnotationArrayValueTypeProcessor(override val enableNewFeatures: Boolean): AbstractTestProcessor() {
    private val results = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logAnnotationArrayValues(resolver, "JavaAnnotated")
        logAnnotationArrayValues(resolver, "KotlinAnnotated")
        logScalarValues(resolver)
        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }

    private fun logAnnotationArrayValues(resolver: Resolver, className: String) {
        val annotated = resolver.getClassDeclarationByName(className)!!
        listOf("JavaAnnotation", "KotlinAnnotation").forEach { annotationName ->
            val annotation = annotated.annotations.single { it.shortName.asString() == annotationName }
            val argument = annotation.arguments.single()
            val value = argument.value
            val argumentName = argument.name?.asString()
            check(value is Array<*> || value is Collection<*>) {
                "Unexpected array-valued annotation argument type: ${value?.javaClass?.name ?: "null"}"
            }
            results.add("$className $annotationName $argumentName is Array<*> or Collection<*>: true")
            results.add("$className $annotationName $argumentName size: ${value.sizeOrNull()}")

            val typedValue = argument.typedValue()
            check(typedValue is ArrayValue) { "Unexpected typed annotation argument: $typedValue" }
            check(typedValue.values.size == 2)
            val nestedValues = typedValue.values.map { element ->
                val nested = element as? AnnotationClass ?: error("Unexpected array element: $element")
                val primitive = nested.anno.arguments.single().typedValue() as? Primitive
                (primitive?.type as? KSString)?.v
            }
            check(nestedValues == listOf("one", "two")) { "Unexpected nested annotation values: $nestedValues" }
            results.add("$className $annotationName $argumentName typedValue: ArrayValue of two AnnotationClass values")
        }
    }

    private fun logScalarValues(resolver: Resolver) {
        val annotated = resolver.getClassDeclarationByName("KotlinAnnotated")!!
        val annotation = annotated.annotations.single { it.shortName.asString() == "TypedAnnotation" }
        val arguments = annotation.arguments.associateBy { it.name?.asString() }

        val number = (arguments.getValue("number").typedValue() as? Primitive)?.type as? KSInt
        check(number?.v == 7) { "Unexpected typed number: $number" }
        results.add("KotlinAnnotated TypedAnnotation number: Primitive(KSInt(7))")

        val mode = arguments.getValue("mode").typedValue() as? EnumClass
        check(mode?.clazz?.simpleName?.asString() == "FIRST") { "Unexpected typed enum: $mode" }
        results.add("KotlinAnnotated TypedAnnotation mode: EnumClass(FIRST)")

        val klass = arguments.getValue("klass").typedValue() as? ReflectionClassReference
        check(klass?.type?.declaration?.simpleName?.asString() == "String") {
            "Unexpected typed class reference: $klass"
        }
        results.add("KotlinAnnotated TypedAnnotation klass: ReflectionClassReference(String)")
    }

    private fun Any?.sizeOrNull(): Int? {
        return when (this) {
            is Array<*> -> size
            is Collection<*> -> size
            else -> null
        }
    }
}
