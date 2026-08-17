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
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSBackingField
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.NonExistLocation
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class AnnotationsInDependenciesProcessor(override val enableNewFeatures: Boolean): AbstractTestProcessor() {
    private val results = mutableListOf<String>()
    override fun toResult() = results

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // NOTE: There are two cases this test ignores.
        // a) For property annotations with target, they get added to the property getter/setter whereas it would show
        //    on the property as well if it was in kotlin source. This test expects it in both for kotlin source
        //    whereas it expects it only in the getter/setter for compiled kotlin source
        // b) When an annotation without a target is used in a constructor (with field), that annotation is not copied
        //    to the backing field for .class files. The assertion line in test ignores it (see the NoTargetAnnotation
        //    output difference for the DataClass)
        addToResults(resolver, "main.KotlinClass")
        addToResults(resolver, "lib.KotlinClass")
        addToResults(resolver, "main.DataClass")
        addToResults(resolver, "lib.DataClass")
        return emptyList()
    }

    private fun addToResults(resolver: Resolver, qName: String) {
        results.add("$qName ->")
        val collected = collectAnnotations(resolver, qName)
        val signatures = collected.flatMap { (annotated, annotations) ->
            val annotatedSignature = annotated.toSignature()
            annotations.map {
                "$annotatedSignature : ${it.toSignature()}"
            }
        }.sorted()
        results.addAll(signatures)
    }

    private fun collectAnnotations(resolver: Resolver, qName: String): Map<KSAnnotated, List<KSAnnotation>> {
        val output = mutableMapOf<KSAnnotated, List<KSAnnotation>>()
        resolver.getClassDeclarationByName(qName)?.accept(
            AnnotationVisitor(enableNewFeatures),
            output
        )
        return output
    }

    private fun KSAnnotated.toSignature(): String {
        return when (this) {
            is KSClassDeclaration -> "class ${(qualifiedName ?: simpleName).asString()} ${this.location.lineNumber}"
            is KSPropertyDeclaration -> "property ${simpleName.asString()} ${this.location.lineNumber}"
            is KSFunctionDeclaration -> "function ${simpleName.asString()} ${this.location.lineNumber}"
            is KSValueParameter -> name?.let {
                "parameter ${it.asString()} ${this.location.lineNumber}"
            } ?: "no-name-value-parameter ${this.location.lineNumber}"
            is KSPropertyGetter -> "getter of ${receiver.toSignature()}" // lineNumber handled by recursive call
            is KSPropertySetter -> "setter of ${receiver.toSignature()}" // lineNumber handled by recursive call
            is KSBackingField -> "field of ${property.toSignature()}" // lineNumber handled by recursive call
            else -> {
                error("unexpected annotated")
            }
        }
    }

    private fun KSAnnotation.toSignature(): String {
        val type = this.annotationType.resolve().declaration.let {
            (it.qualifiedName ?: it.simpleName).asString()
        }
        val args = this.arguments.map {
            "[${it.name?.asString()} = ${it.value} : ${it.location.lineNumber}]"
        }.joinToString(",")
        return "$type{$args} : ${this.location.lineNumber}"
    }

    private val Location.lineNumber: String
        get() = when (this) {
            is FileLocation -> this.lineNumber.toString()
            is NonExistLocation -> "<no line>"
        }

    inner class AnnotationVisitor(enableNewFeatures: Boolean)  : KSTopDownVisitor<MutableMap<KSAnnotated, List<KSAnnotation>>, Unit>(enableNewFeatures) {
        override fun defaultHandler(node: KSNode, data: MutableMap<KSAnnotated, List<KSAnnotation>>) {
        }

        override fun visitAnnotated(annotated: KSAnnotated, data: MutableMap<KSAnnotated, List<KSAnnotation>>) {
            val annotations = annotated.annotations.toList()
            if (annotations.isNotEmpty()) {
                data[annotated] = annotations
            }
            super.visitAnnotated(annotated, data)
        }

        override fun visitTypeReference(
            typeReference: KSTypeReference,
            data: MutableMap<KSAnnotated, List<KSAnnotation>>
        ) {
            // don't traverse type references
        }
    }
}
