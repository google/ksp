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

import com.google.devtools.ksp.KSTypeNotPresentException
import com.google.devtools.ksp.KSTypesNotPresentException
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getKSAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import kotlin.reflect.KClass

@KspExperimental
class AnnotationArbitraryClassValueProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols =
            resolver.getSymbolsWithAnnotation(
                "com.google.devtools.ksp.processor.ClassValueAnnotation"
            )
        symbols.forEach {
            it.getKSAnnotationsByType(ClassValueAnnotation::class).forEach(::logKSAnnotationValues)
            result.add(it.isAnnotationPresent(ClassValueAnnotation::class).toString())
            result.add(
                it.getKSAnnotationsByType(OtherClassValueAnnotations.ClassValueAnnotation::class)
                    .count()
                    .toString()
            )
            result.add(
                it.isAnnotationPresent(OtherClassValueAnnotations.ClassValueAnnotation::class)
                    .toString()
            )
            it.getAnnotationsByType(ClassValueAnnotation::class).forEach(::logAnnotationValues)
        }
        return emptyList()
    }

    private fun logKSAnnotationValues(annotation: KSAnnotation) {
        val classValue = annotation.argument("classValue") as KSType
        result.add(classValue.renderQualifiedName())

        val classValues = annotation.argument("classValues") as List<*>
        result.add(classValues.joinToString { (it as KSType).renderQualifiedName() })
    }

    private fun KSAnnotation.argument(name: String): Any? =
        arguments.single { it.name?.asString() == name }.value

    private fun KSType.renderQualifiedName(): String {
        val qualifiedName =
            declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()
        if (arguments.isEmpty()) {
            return qualifiedName
        }
        return arguments.joinToString(prefix = "$qualifiedName<", postfix = ">") {
            it.type?.resolve()?.renderQualifiedName() ?: "*"
        }
    }

    private fun logAnnotationValues(classValueAnnotation: ClassValueAnnotation) {
        try {
            classValueAnnotation.classValue
        } catch (e: Exception) {
            assert(e is KSTypeNotPresentException)
            e as KSTypeNotPresentException
            result.add(e.ksType.toString())
        }

        try {
            classValueAnnotation.classValues
        } catch (e: Exception) {
            assert(e is KSTypesNotPresentException)
            e as KSTypesNotPresentException
            result.add(e.ksTypes.joinToString())
        }
    }
}

annotation class ClassValueAnnotation(
    val classValue: KClass<*>,
    val classValues: Array<KClass<*>>,
)

private object OtherClassValueAnnotations {
    annotation class ClassValueAnnotation
}
