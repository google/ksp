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

package com.google.devtools.ksp.standalone

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleProviderBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.KtSourceModuleBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import java.nio.file.Path

private val analyzableExtensions = setOf("kt", "java", "kts")

/**
 * Builds a source module from the analyzable files under [sourceRoots].
 *
 * The content scope of the module is an [IncrementalGlobalSearchScope], so that the files generated in later rounds
 * can be added to it.
 */
fun KtModuleProviderBuilder.buildKspSourceModule(
    sourceRoots: Collection<Path>,
    init: KtSourceModuleBuilder.() -> Unit,
): KaSourceModule {
    val project = project
    val virtualFiles = collectVirtualFilesByRoots(coreApplicationEnvironment, sourceRoots)
    return buildKtSourceModule {
        init()
        addSourceVirtualFiles(virtualFiles.filterNot { it.isDirectory })
        contentScope = IncrementalGlobalSearchScope(project, virtualFiles)
    }
}

private fun collectVirtualFilesByRoots(
    coreApplicationEnvironment: CoreApplicationEnvironment,
    sourceRoots: Collection<Path>,
): Set<VirtualFile> {
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
