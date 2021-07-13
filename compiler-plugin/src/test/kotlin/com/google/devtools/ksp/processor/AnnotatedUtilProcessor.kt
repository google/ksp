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


package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.getAnnotation
import com.google.devtools.ksp.symbol.impl.getAnnotations
import com.google.devtools.ksp.symbol.impl.getAnnotationsByType
import com.google.devtools.ksp.symbol.impl.isAnnotationPresent
import com.google.devtools.ksp.visitor.KSTopDownVisitor
import kotlin.jvm.Throws
import kotlin.reflect.KClass

class AnnotatedUtilProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    private val visitors = listOf(GetAnnotationVisitor(), GetAnnotationsVisitor(), GetAnnotationsByTypeVisitor())

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation("Test", true).forEach {
            val annotation = it.annotations.single()
            annotation.arguments.forEach { visitors.forEach { visitor -> it.accept(visitor, results) } }
        }
        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }

    inner class GetAnnotationVisitor : KSTopDownVisitor<MutableCollection<String>, Unit>() {
        override fun visitAnnotated(annotated: KSAnnotated, data: MutableCollection<String>) {
            if (annotated.isAnnotationPresent(ParametersTestAnnotation::class)) {
                annotated.getAnnotation(ParametersTestAnnotation::class)?.let { data.add(it.toString()) }
            }
            if (annotated.isAnnotationPresent(ParameterArraysTestAnnotation::class)) {
                annotated.getAnnotation(ParameterArraysTestAnnotation::class)?.let { data.add(it.toString()) }
            }
            if (annotated.isAnnotationPresent(ParametersTestWithNegativeDefaultsAnnotation::class)) {
                annotated.getAnnotation(ParametersTestWithNegativeDefaultsAnnotation::class)
                    ?.let { data.add(it.toString()) }
            }
        }

        override fun defaultHandler(node: KSNode, data: MutableCollection<String>) {
        }
    }

    inner class GetAnnotationsVisitor : KSTopDownVisitor<MutableCollection<String>, Unit>() {
        override fun visitAnnotated(annotated: KSAnnotated, data: MutableCollection<String>) {
            annotated.getAnnotations().forEach { data.add(it.toString()) }
        }

        override fun defaultHandler(node: KSNode, data: MutableCollection<String>) {
        }
    }

    inner class GetAnnotationsByTypeVisitor : KSTopDownVisitor<MutableCollection<String>, Unit>() {
        override fun visitAnnotated(annotated: KSAnnotated, data: MutableCollection<String>) {
            annotated.getAnnotationsByType(ParametersTestAnnotation::class).let { data.add(it.toString()) }
            annotated.getAnnotationsByType(ParameterArraysTestAnnotation::class).let { data.add(it.toString()) }
            annotated.getAnnotationsByType(ParametersTestWithNegativeDefaultsAnnotation::class)
                .let { data.add(it.toString()) }
            annotated.getAnnotationsByType(Throws::class).let { data.add(it.toString()) }
        }

        override fun defaultHandler(node: KSNode, data: MutableCollection<String>) {
        }
    }

    @Suppress("LongParameterList")
    annotation class ParametersTestAnnotation(
        val booleanValue: Boolean = false,
        val byteValue: Byte = 2,
        val charValue: Char = 'b',
        val doubleValue: Double = 3.0,
        val floatValue: Float = 4.0f,
        val intValue: Int = 5,
        val longValue: Long = 6L,
        val stringValue: String = "emptystring",
        // fails on getting the arguments from the KSAnnotation when no value is set for the kClassValue in
        // the declaration. Throws an NPE with using a default value
        val kClassValue: KClass<*> = ParametersTestAnnotation::class,
        val enumValue: TestEnum = TestEnum.NONE,
    )

    @Suppress("LongParameterList")
    annotation class ParameterArraysTestAnnotation(
        val booleanArrayValue: BooleanArray = booleanArrayOf(),
        val byteArrayValue: ByteArray = byteArrayOf(),
        val charArrayValue: CharArray = charArrayOf(),
        val doubleArrayValue: DoubleArray = doubleArrayOf(),
        val floatArrayValue: FloatArray = floatArrayOf(),
        val intArrayValue: IntArray = intArrayOf(),
        val longArrayValue: LongArray = longArrayOf(),
        val stringArrayValue: Array<String> = emptyArray(),
        val kClassArrayValue: Array<KClass<*>> = emptyArray(),
        val enumArrayValue: Array<TestEnum> = emptyArray(),
    )

    annotation class ParametersTestWithNegativeDefaultsAnnotation(
        val byteValue: Byte = -2,
        val doubleValue: Double = -3.0,
        val floatValue: Float = -4.0f,
        val intValue: Int = -5,
        val longValue: Long = -6L,
    )

    enum class TestEnum {
        NONE, VALUE1, VALUE2
    }

}
