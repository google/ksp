package com.google.devtools.ksp.standalone

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinCompositeDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeAlias

class IncrementalKotlinDeclarationProviderFactory(
    private val project: Project,
) : KotlinDeclarationProviderFactory() {
    private val staticFactories: MutableList<KotlinStandaloneDeclarationProviderFactory> = mutableListOf()

    override fun createDeclarationProvider(
        scope: GlobalSearchScope,
        contextualModule: KtModule?
    ): KotlinDeclarationProvider {
        val providers = staticFactories.map { it.createDeclarationProvider(scope, contextualModule) }
        return KotlinCompositeDeclarationProvider.create(providers)
    }

    fun update(files: Collection<KtFile>) {
        val skipBuiltIns = staticFactories.isNotEmpty()
        val staticFactory = KotlinStandaloneDeclarationProviderFactory(project, files, skipBuiltins = skipBuiltIns)
        staticFactories.add(staticFactory)
    }

    fun getDirectInheritorCandidates(baseClassName: Name): Set<KtClassOrObject> =
        staticFactories.flatMapTo(mutableSetOf()) {
            it.getDirectInheritorCandidates(baseClassName)
        }

    fun getInheritableTypeAliases(aliasedName: Name): Set<KtTypeAlias> =
        staticFactories.flatMapTo(mutableSetOf()) {
            it.getInheritableTypeAliases(aliasedName)
        }
}
