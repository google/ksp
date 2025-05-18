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
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class LibOriginsProcessor : AbstractTestProcessor() {
    private val result = mutableListOf<String>()
    private val visited = mutableSetOf<KSNode>()

    override fun toResult(): List<String> {
        return result
    }

    inner class MyCollector : KSTopDownVisitor<Origin, Unit>() {
        private fun KSNode.pretty(): String {
            val parents: MutableList<KSNode> = mutableListOf(this)
            var curr: KSNode = this
            while (curr.parent != null) {
                curr = curr.parent!!
                parents.add(curr)
            }
            parents.reverse()
            return parents.toString()
        }

        override fun defaultHandler(node: KSNode, data: Origin) {
            if (node.origin != data && !visited.contains(node)) {
                visited.add(node)
                result.add("Exception: ${node.pretty()}: ${node.origin}")
            }
        }
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val visitor = MyCollector()

        // FIXME: workaround for https://github.com/google/ksp/issues/418
        resolver.getDeclarationsFromPackage("foo.bar").sortedBy { it.simpleName.asString() }.forEach {
            if (it.containingFile == null || it.containingFile.toString().endsWith(".class")) {
                result.add("Validating $it")
                it.accept(visitor, it.origin)
            }
        }

        resolver.getNewFiles().sortedBy { it.fileName }.forEach {
            result.add("Validating $it")
            it.accept(visitor, it.origin)
        }

        return emptyList()
    }
}
