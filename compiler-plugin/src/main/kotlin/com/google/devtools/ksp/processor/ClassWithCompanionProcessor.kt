/*
 * Copyright 2010-2020 Google LLC, JetBrains s.r.o and and Kotlin Programming Language contributors.
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
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSVisitorVoid

class ClassWithCompanionProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val visitor = CompanionVisitor()

    override fun process(resolver: Resolver) {
        resolver.getAllFiles().map { it.accept(CompanionVisitor(), Unit) }
    }

    override fun toResult(): List<String> {
        return results
    }

    inner class CompanionVisitor : KSVisitorVoid() {
        override fun visitFile(file: KSFile, data: Unit) {
            file.declarations.map { it.accept(this, Unit) }
        }

        override fun visitClassDeclaration(type: KSClassDeclaration, data: Unit) {
            results.add("${type.simpleName.asString()}:${type.isCompanionObject}")
            type.declarations.map { it.accept(this, Unit) }
        }
    }
}