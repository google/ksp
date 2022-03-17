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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSTopDownVisitor

open class JavaWildcard2Processor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getNewFiles().forEach {
            resolver.getNewFiles().forEach {
                it.accept(RefVisitor(results, resolver), "")
            }
        }

        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }

    private class RefVisitor(
        val results: MutableList<String>,
        val resolver: Resolver
    ) : KSTopDownVisitor<String, Unit>() {
        override fun defaultHandler(node: KSNode, data: String) = Unit

        private fun KSTypeReference.pretty(): String {
            return resolve().toString()
        }

        override fun visitTypeArgument(typeArgument: KSTypeArgument, data: String) = Unit

        override fun visitAnnotation(annotation: KSAnnotation, data: String) = Unit

        @OptIn(KspExperimental::class)
        override fun visitTypeReference(typeReference: KSTypeReference, data: String) {
            val wildcard = resolver.getJavaWildcard(typeReference)
            results.add(
                data + typeReference.parent.toString() + " : " + wildcard.pretty()
            )
            super.visitTypeReference(typeReference, data + "- ")
        }
    }
}
