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

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Origin

open class TestProcessor(
    val environment: SymbolProcessorEnvironment,
    protected open val followDefaultArguments: Boolean = false
) : SymbolProcessor {
    init {
        // Identifies the classloader this processor was loaded from. Modules that share a cached
        // processor classloader report the same id; modules with their own loader report different
        // ids. Deliberately uses a prefix distinct from [log]'s so that tests asserting on
        // "[TestProcessor]" lines are unaffected.
        println("[KSPLoaderId] ${System.identityHashCode(TestProcessor::class.java.classLoader)}")
    }

    protected open val label: String = "[TestProcessor]"
    private val createdFiles = mutableSetOf<String>()
    override fun process(resolver: Resolver): List<KSAnnotated> {
        (
            sequenceOf("ExampleReference", "ExampleJavaReference")
                .flatMap { resolver.getSymbolsWithAnnotation(it) } +
                resolver.getSymbolsWithAnnotation("com.example.upstream.ExampleReferenceJava")
                    .filter { it.origin == Origin.KOTLIN }
            )
            .filterIsInstance<KSClassDeclaration>().forEach { sym ->
                // Follow dependencies to let KSP capture dependency graph
                followSym(sym)
                if (environment.options["callGetAllMembers"] == "true") {
                    sym.getAllFunctions().toList()
                    sym.getAllProperties().toList()
                }

                val fileName = "${sym.simpleName.asString()}Generated"

                if (!createdFiles.contains(fileName)) {
                    createdFiles.add(fileName)
                    val associatedJavaLib = environment.options["associateJavaLibClass"]
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { resolver.getClassDeclarationByName(it) }
                    val file = environment.codeGenerator.createNewFile(
                        Dependencies(false, sym.containingFile!!),
                        "",
                        fileName
                    )
                    val suffix = associatedJavaLib?.let { javaLibDecl ->
                        val members = javaLibDecl.declarations
                            .map { it.simpleName.asString() }
                            .sorted()
                            .joinToString(",")
                        " { val members = \"$members\" }"
                    } ?: ""
                    file.write("class ${sym.simpleName.asString()}Generated$suffix".toByteArray())
                    if (associatedJavaLib != null) {
                        environment.codeGenerator.associateWithClasses(
                            listOf(associatedJavaLib),
                            "",
                            fileName
                        )
                    }
                }
            }
        // Do not keep KSNodes across rounds
        seen.clear()
        return emptyList()
    }

    private val seen = mutableListOf<KSNode>()
    private val queue = mutableListOf<KSNode>()

    private fun followSym(sym: KSAnnotated) {
        if (!seen.contains(sym) && !queue.contains(sym)) {
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

    private fun followValue(value: Any?) {
        when (value) {
            is List<*> -> {
                value.forEach(::followValue)
            }

            is Array<*> -> {
                value.forEach(::followValue)
            }

            is KSType -> {
                val decl = value.declaration
                if (!seen.contains(decl) && !queue.contains(decl)) {
                    queue.add(decl)
                }
                value.arguments.forEach { typeArg ->
                    followValue(typeArg.type?.resolve())
                }
            }

            is KSClassDeclaration -> {
                if (!seen.contains(value) && !queue.contains(value)) {
                    queue.add(value)
                }
            }

            is KSAnnotation -> {
                followAnnotation(value)
            }

            else -> Unit
        }
    }

    private fun followAnnotation(anno: KSAnnotation) {
        anno.arguments.forEach { annoArg ->
            followValue(annoArg.value)
        }
        if (followDefaultArguments) {
            anno.defaultArguments.forEach { defaultArg ->
                followValue(defaultArg.value)
            }
        }
    }

    private fun followAnnotations(sym: KSAnnotated) {
        sym.annotations.forEach(::followAnnotation)
    }

    private fun followDeclaration(ksDecl: KSDeclaration): Unit = when (ksDecl) {
        is KSClassDeclaration -> {
            ksDecl.declarations
                .filterNot { seen.contains(it) || queue.contains(it) }
                .filter { it !is KSPropertyDeclaration }
                .forEach(queue::add)
        }

        is KSFunctionDeclaration -> {
            ksDecl.returnType?.resolve()?.declaration?.let {
                if (!seen.contains(it) && !queue.contains(it)) {
                    queue.add(it)
                }
            }
            ksDecl.parameters.forEach(::followAnnotations)
            ksDecl.parameters
                .map { it.type.resolve().declaration }
                .filterNot { seen.contains(it) || queue.contains(it) }
                .forEach(queue::add)
        }

        else -> Unit
    }

    private fun log(msg: Any) {
        println("$label $msg")
    }
}
