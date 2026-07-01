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

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType

class TestProcessor(val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val createdFiles = mutableSetOf<String>()
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation("ExampleReference")
            .filterIsInstance<KSClassDeclaration>().forEach { sym ->
                // Follow dependencies to let KSP capture dependency graph
                followSym(sym)

                val fileName = "${sym.simpleName.asString()}Generated"

                if (!createdFiles.contains(fileName)) {
                    createdFiles.add(fileName)
                    val file = environment.codeGenerator.createNewFile(
                        Dependencies(false, sym.containingFile!!),
                        "",
                        fileName
                    )
                    file.write("class ${sym.simpleName.asString()}Generated".toByteArray())
                }
            }
        // Do not keep KSNodes across rounds
        seen.clear()
        return emptyList()
    }

    private val seen = mutableListOf<KSNode>()
    private val queue = mutableListOf<KSNode>()

    private fun followSym(sym: KSAnnotated) {
        if (!seen.contains(sym)) {
            queue.add(sym)
        }
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            log("Processing $node")
            seen.add(node)
            when (node) {
                is KSDeclaration -> {
                    followAnnotations(node)
                    followDeclaration(node)
                }

                is KSAnnotated -> {
                    followAnnotations(node)
                }

                else -> Unit
            }
        }
    }

    private fun followAnnotations(sym: KSAnnotated) {
        sym.annotations.forEach { anno ->
            anno.arguments.forEach { annoArg ->
                annoArg.value?.let { value ->
                    when (value) {
                        is List<*> -> {
                            value.filterIsInstance<KSType>()
                                .map { ksType -> ksType.declaration }
                                .filterNot(seen::contains)
                                .forEach(queue::add)
                        }

                        is KSType -> {
                            val decl = value.declaration
                            if (!seen.contains(decl)) {
                                queue.add(decl)
                            }
                        }

                        else -> {
                            return@forEach
                        }
                    }
                }
            }
        }
    }

    private fun followDeclaration(ksDecl: KSDeclaration): Unit = when (ksDecl) {
        is KSClassDeclaration -> {
            ksDecl.declarations
                .filterNot(seen::contains)
                .filter { it !is KSPropertyDeclaration }
                .forEach(queue::add)
        }

        is KSFunctionDeclaration -> {
            ksDecl.returnType?.resolve()?.declaration?.let {
                if (!seen.contains(it)) {
                    queue.add(it)
                }
            }
            ksDecl.parameters
                .map { it.type.resolve().declaration }
                .filterNot(seen::contains)
                .forEach(queue::add)
        }

        else -> Unit
    }

    private fun log(msg: Any) {
        println("[TestProcessor] $msg")
    }
}
