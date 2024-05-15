package com.google.devtools.ksp.standalone

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticPackageProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.packageProviders.CompositeKotlinPackageProvider
import org.jetbrains.kotlin.psi.KtFile

class IncrementalKotlinPackageProviderFactory(
    private val project: Project,
) : KotlinPackageProviderFactory() {
    private val staticFactories: MutableList<KotlinStaticPackageProviderFactory> = mutableListOf()

    override fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider {
        val providers = staticFactories.map { it.createPackageProvider(searchScope) }
        return CompositeKotlinPackageProvider.create(providers)
    }

    fun update(files: Collection<KtFile>) {
        val staticFactory = KotlinStaticPackageProviderFactory(project, files)
        staticFactories.add(staticFactory)
    }
}
