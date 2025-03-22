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

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleBuilderDsl
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleProviderBuilder
import org.jetbrains.kotlin.analysis.project.structure.impl.KaSourceModuleImpl
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KtModuleBuilderDsl
class KspModuleBuilder(
    private val coreApplicationEnvironment: CoreApplicationEnvironment,
    private val project: Project
) : KtModuleBuilder() {
    public lateinit var moduleName: String
    public var languageVersionSettings: LanguageVersionSettings =
        LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST)

    private val sourceRoots: MutableSet<Path> = mutableSetOf()

    fun addSourceRoot(path: Path) {
        sourceRoots.add(path)
    }

    fun addSourceRoots(paths: Collection<Path>) {
        sourceRoots.addAll(paths)
    }

    override fun build(): KaSourceModule {
        val virtualFiles = collectVirtualFilesByRoots()
        val psiManager = PsiManager.getInstance(project)
        val psiFiles = virtualFiles.mapNotNull { psiManager.findFile(it) }
        val contentScope = IncrementalGlobalSearchScope(project, virtualFiles)
        return KaSourceModuleImpl(
            directRegularDependencies,
            directDependsOnDependencies,
            directFriendDependencies,
            contentScope,
            platform,
            project,
            moduleName,
            languageVersionSettings,
            psiFiles,
        )
    }

    val analyzableExtensions = setOf("kt", "java", "kts")

    private fun collectVirtualFilesByRoots(): Set<VirtualFile> {
        val localFileSystem = coreApplicationEnvironment.localFileSystem
        return buildSet {
            for (root in sourceRoots) {
                val files = root.toFile().walk().filter {
                    it.isDirectory || it.extension.lowercase() in analyzableExtensions
                }
                for (file in files) {
                    val virtualFile = localFileSystem.findFileByIoFile(file) ?: continue
                    add(virtualFile)
                }
            }
        }
    }
}

class IncrementalGlobalSearchScope(
    project: Project,
    initialSet: Collection<VirtualFile> = emptyList(),
) : GlobalSearchScope(project) {
    // TODO: optimize space with trie.
    val files = mutableSetOf<VirtualFile>().apply { addAll(initialSet) }

    fun addAll(files: Collection<VirtualFile>) = this.files.addAll(files)

    override fun contains(file: VirtualFile): Boolean = file in files

    override fun isSearchInLibraries(): Boolean = false

    override fun isSearchInModuleContent(aModule: Module): Boolean = true
}

@OptIn(ExperimentalContracts::class)
public inline fun KtModuleProviderBuilder.buildKspSourceModule(
    init: KspModuleBuilder.() -> Unit
): KaSourceModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KspModuleBuilder(coreApplicationEnvironment, project).apply(init).build()
}
