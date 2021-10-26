package com.google.devtools.ksp.impl

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProvider
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule

class KtModuleScopeProviderImpl: KtModuleScopeProvider() {
    override fun getModuleLibrariesScope(sourceModule: KtSourceModule): GlobalSearchScope {
        return GlobalSearchScope.filesScope(sourceModule.project, emptyList())
    }

}
