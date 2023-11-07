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

import com.google.devtools.ksp.impl.DirectoriesScope
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleBuilderDsl
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleProviderBuilder
import org.jetbrains.kotlin.analysis.project.structure.impl.KtSourceModuleImpl
import org.jetbrains.kotlin.analysis.project.structure.impl.collectSourceFilePaths
import org.jetbrains.kotlin.analysis.project.structure.impl.hasSuitableExtensionToAnalyse
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString

@KtModuleBuilderDsl
class KspModuleBuilder(
    private val kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
) : KtModuleBuilder() {
    public lateinit var moduleName: String
    public var languageVersionSettings: LanguageVersionSettings =
        LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST)

    private val sourceRoots: MutableList<Path> = mutableListOf()

    fun addSourceRoot(path: Path) {
        sourceRoots.add(path)
    }

    fun addSourceRoots(paths: Collection<Path>) {
        sourceRoots.addAll(paths)
    }

    override fun build(): KtSourceModule {
        val virtualFiles = collectVirtualFilesByRoots()
        val psiManager = PsiManager.getInstance(kotlinCoreProjectEnvironment.project)
        val psiFiles = virtualFiles.mapNotNull { psiManager.findFile(it) }
        val project = kotlinCoreProjectEnvironment.project
        val fs = kotlinCoreProjectEnvironment.environment.localFileSystem
        val contentScope =
            DirectoriesScope(project, sourceRoots.mapNotNull { fs.findFileByPath(it.pathString) }.toSet())
        return KtSourceModuleImpl(
            directRegularDependencies,
            directDependsOnDependencies,
            directFriendDependencies,
            contentScope,
            platform,
            kotlinCoreProjectEnvironment.project,
            moduleName,
            languageVersionSettings,
            psiFiles,
        )
    }

    private fun collectVirtualFilesByRoots(): List<VirtualFile> {
        val localFileSystem = kotlinCoreProjectEnvironment.environment.localFileSystem
        return buildList {
            for (root in sourceRoots) {
                val files = when {
                    root.isDirectory() -> collectSourceFilePaths(root)
                    root.hasSuitableExtensionToAnalyse() -> listOf(root)
                    else -> emptyList()
                }
                for (file in files) {
                    val virtualFile = localFileSystem.findFileByIoFile(file.toFile()) ?: continue
                    add(virtualFile)
                }
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun KtModuleProviderBuilder.buildKspSourceModule(
    init: KspModuleBuilder.() -> Unit
): KtSourceModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KspModuleBuilder(kotlinCoreProjectEnvironment).apply(init).build()
}
