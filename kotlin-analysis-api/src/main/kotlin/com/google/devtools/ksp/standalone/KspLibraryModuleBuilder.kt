/*
 * Copyright 2023 Google LLC
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
package com.google.devtools.ksp.standalone

import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.project.structure.builder.KtBinaryModuleBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleBuilderDsl
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleProviderBuilder
import org.jetbrains.kotlin.analysis.project.structure.impl.KaLibraryModuleImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KtModuleBuilderDsl
class KspLibraryModuleBuilder(
    private val kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment
) : KtBinaryModuleBuilder() {
    public lateinit var libraryName: String
    public var librarySources: KaLibrarySourceModule? = null

    override fun build(): KaLibraryModule {
        val binaryRoots = getBinaryRoots()
        val contentScope = ProjectScope.getLibrariesScope(kotlinCoreProjectEnvironment.project)
        return KaLibraryModuleImpl(
            directRegularDependencies,
            directDependsOnDependencies,
            directFriendDependencies,
            contentScope,
            platform,
            kotlinCoreProjectEnvironment.project,
            binaryRoots,
            libraryName,
            librarySources,
            false
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun KtModuleProviderBuilder.buildKspLibraryModule(init: KspLibraryModuleBuilder.() -> Unit): KaLibraryModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KspLibraryModuleBuilder(kotlinCoreProjectEnvironment).apply(init).build()
}
