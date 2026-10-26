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
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSValueArgument

class VarArgAnnotationApplicationProcessor(
    val classNames: List<String>,
    override val enableNewFeatures: Boolean
) : AbstractTestProcessor() {

    private val results = mutableListOf<String>()

    override fun toResult(): List<String> = results

    override fun process(resolver: Resolver): List<KSAnnotated> {
        classNames.forEach { className ->
            val decl = resolver.getClassDeclarationByName(className)!!
            decl.annotations.forEach { anno ->
                val sb = StringBuilder()
                sb.append(decl.qualifiedName?.asString() ?: decl.simpleName.asString())
                sb.append(": ")
                visitAnnotation(anno, sb)
                results.add(sb.toString())
            }
        }
        return emptyList()
    }

    private fun visitAnnotation(annotation: KSAnnotation, sb: StringBuilder): StringBuilder {
        sb.append("@")
        sb.append(annotation.shortName.asString())
        sb.append('[')
        annotation.arguments.forEachIndexed { index, argument ->
            visitAnnotationArgument(argument, sb)
            commaSeparate(index, annotation.arguments.size, sb)

        }
        sb.append(']')
        return sb
    }

    private fun visitAnnotationArgument(annotationArgument: KSValueArgument, sb: StringBuilder): StringBuilder {
        sb.append(annotationArgument.name?.asString())
        sb.append(':')
        return when (val argumentValue = annotationArgument.value) {
            is KSAnnotation -> visitAnnotation(argumentValue, sb)
            is Collection<*> -> {
                sb.append('[')
                argumentValue.forEachIndexed { index, value ->
                    visitAny(value, sb)
                    commaSeparate(index, argumentValue.size, sb)
                }
                sb.append(']')
            }

            else -> sb.append(argumentValue)
        }
    }

    private fun visitAny(value: Any?, sb: StringBuilder): StringBuilder = when (value) {
        is KSAnnotation -> visitAnnotation(value, sb)
        else -> sb.append(value)
    }

    private fun commaSeparate(index: Int, size: Int, sb: StringBuilder): StringBuilder {
        if (index < size - 1) {
            sb.append(", ")
        }
        return sb
    }
}
