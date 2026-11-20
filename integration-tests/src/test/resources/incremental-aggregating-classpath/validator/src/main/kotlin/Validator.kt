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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*

/**
 * Lists the declarations of package `p6`, which is on the classpath, in an aggregating output, `p6.log`. Nothing is
 * generated while the package is empty.
 */
class Validator(private val codeGenerator: CodeGenerator) : SymbolProcessor {
    private var processed = false

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (processed) {
            return emptyList()
        }
        processed = true
        val names = resolver.getDeclarationsFromPackage("p6").mapNotNull { it.qualifiedName?.asString() }.toList()
        if (names.isEmpty()) {
            return emptyList()
        }
        codeGenerator.createNewFile(Dependencies(aggregating = true), "", "p6", "log").writer().use { writer ->
            names.sorted().forEach { writer.write("$it\n") }
        }
        return emptyList()
    }
}

class TestProcessorProvider : SymbolProcessorProvider {
    override fun create(env: SymbolProcessorEnvironment): SymbolProcessor = Validator(env.codeGenerator)
}
