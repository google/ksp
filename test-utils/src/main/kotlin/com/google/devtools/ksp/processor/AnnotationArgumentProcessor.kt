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

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*

class AnnotationArgumentProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val visitor = ArgumentVisitor()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        listOf("MyClass", "MyClassInLib").forEach { clsName ->
            resolver.getClassDeclarationByName(clsName)?.let { cls ->
                cls.annotations.forEach() { annotation ->
                    results.add(
                        "$clsName: ${annotation.annotationType.resolve().declaration.qualifiedName?.asString()}"
                    )
                    annotation.arguments.forEach {
                        results.add(
                            "$clsName: ${annotation.shortName.asString()}: ${it.name!!.asString()} = ${it.value}"
                        )
                    }
                }
            }
        }

        resolver.getClassDeclarationByName("DataClass")?.let { cls ->
            cls.declarations.filterIsInstance<KSFunctionDeclaration>().single {
                it.simpleName.asString() == "copy"
            }.annotations.forEach {
                it.arguments
            }
        }

        resolver.getSymbolsWithAnnotation("Bar", true).forEach {
            it.annotations.forEach { it.arguments.forEach { it.accept(visitor, Unit) } }
        }

        val C = resolver.getClassDeclarationByName("C")
        C?.annotations?.first()?.arguments?.forEach { results.add(it.value.toString()) }
        val ThrowsClass = resolver.getClassDeclarationByName("ThrowsClass")
        ThrowsClass?.declarations?.filter {
            it.simpleName.asString() == "throwsException"
        }?.forEach {
            it.annotations.single().annotationType.resolve().declaration.let {
                results.add(it.toString())
            }
        }

        resolver.getClassDeclarationByName("Sub")?.let { cls ->
            cls.superTypes.single().annotations.single().let { typeAnnotation ->
                val a = typeAnnotation.arguments.single().value as KSAnnotation
                results.add("Sub: ${a.arguments}")
            }
        }

        resolver.getClassDeclarationByName("Cls")?.let { cls ->
            cls.annotations.single().arguments.forEach { argToA ->
                results.add("Cls: argToA: ${argToA.name!!.asString()}")
                argToA.value.let { argBVal ->
                    if (argBVal is KSAnnotation) {
                        argBVal.arguments.forEach { argToB ->
                            results.add("Cls: argToB: " + argToB.value)
                        }
                    } else {
                        results.add("Cls: argBVal unknown: $argBVal")
                    }
                }
            }
        }

        resolver.getClassDeclarationByName("TestJavaLib")?.let { cls ->
            cls.annotations.single().arguments.single().let { ksValueArg ->
                results.add("TestJavaLib: " + (ksValueArg.value as KSAnnotation).shortName.asString())
            }
        }

        resolver.getClassDeclarationByName("TestNestedAnnotationDefaults")?.let { cls ->
            cls.annotations.forEach { annotation ->
                val annotationArg = annotation.arguments.single().value as KSAnnotation
                results.add("${cls.simpleName.asString()}: ${annotationArg.arguments.single().value}")
            }
        }

        resolver.getClassDeclarationByName("TestValueArgEquals")?.let { cls ->
            cls.annotations.forEach { anno1 ->
                val arg1 = (anno1.arguments.single().value as KSAnnotation).arguments.single()
                cls.annotations.forEach { anno2 ->
                    val arg2 = (anno2.arguments.single().value as KSAnnotation).arguments.single()
                    val eq = arg1 == arg2
                    results.add("$anno1($arg1) == $anno2($arg2): $eq")
                }
            }
        }

        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }

    inner class ArgumentVisitor : KSVisitorVoid() {
        override fun visitValueArgument(valueArgument: KSValueArgument, data: Unit) {
            if (valueArgument.value is KSType) {
                results.add((valueArgument.value as KSType).declaration.toString())
            } else {
                results.add(valueArgument.value.toString())
            }
        }
    }
}
