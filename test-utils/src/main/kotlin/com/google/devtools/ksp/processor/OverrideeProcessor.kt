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

import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*

@Suppress("unused") // used by tests
class OverrideeProcessor : AbstractTestProcessor() {
    private val results = mutableListOf<String>()

    override fun toResult() = results

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logSubject(resolver, "NoOverride")
        logSubject(resolver, "Subject")
        logSubject(resolver, "JavaSubject.Subject")
        logSubject(resolver, "lib.Subject")
        logSubject(resolver, "ConflictingSubject1")
        logSubject(resolver, "ConflictingSubject2")
        logSubject(resolver, "ConflictingSubject3")
        logSubject(resolver, "ConflictingSubject4")
        logSubject(resolver, "OverrideOrder1")
        logSubject(resolver, "OverrideOrder2")
        logSubject(resolver, "JavaAccessorImpl")
        return emptyList()
    }

    private fun logSubject(resolver: Resolver, qName: String) {
        results.add("$qName:")
        val subject = resolver.getClassDeclarationByName(qName)!!
        subject.declarations.filterIsInstance<KSClassDeclaration>().forEach {
            logClass(it)
        }
        logClass(subject)
    }

    private fun logClass(subject: KSClassDeclaration) {
        subject.declarations.filterIsInstance<KSPropertyDeclaration>()
            .forEach {
                checkOverridee(it)
            }
        subject.declarations.filterIsInstance<KSFunctionDeclaration>()
            .filterNot { it.simpleName.asString() in IGNORED_METHOD_NAMES }
            .forEach {
                checkOverridee(it)
            }
    }

    private fun checkOverridee(declaration: KSDeclaration) {
        val signature = if (declaration is KSPropertyDeclaration) declaration.toSignature() else
            (declaration as KSFunctionDeclaration).toSignature()
        val overrideeSignature = if (declaration is KSPropertyDeclaration) declaration.findOverridee()?.toSignature()
        else (declaration as KSFunctionDeclaration).findOverridee()?.toSignature()
        results.add("$signature -> $overrideeSignature")
    }

    private fun KSDeclaration.toSignature(): String {
        return when (this) {
            is KSFunctionDeclaration -> this.toSignature()
            is KSPropertyDeclaration -> this.toSignature()
            else -> throw IllegalStateException()
        }
    }

    private fun KSFunctionDeclaration.toSignature(): String {
        val self = this
        return buildString {
            append(self.closestClassDeclaration()?.simpleName?.asString())
            append(".")
            append(self.simpleName.asString())
            append(
                self.parameters.joinToString(", ", prefix = "(", postfix = ")") {
                    "${it.name?.asString()}:${it.type.resolve().declaration.simpleName.asString()}"
                }
            )
        }
    }

    private fun KSPropertyDeclaration.toSignature(): String {
        val self = this
        return buildString {
            append(self.closestClassDeclaration()?.simpleName?.asString())
            append(".")
            append(self.simpleName.asString())
        }
    }

    companion object {
        // ignore these methods as we receive syntetics of it from compiled code
        private val IGNORED_METHOD_NAMES = listOf("equals", "hashCode", "toString", "<init>")
    }
}
