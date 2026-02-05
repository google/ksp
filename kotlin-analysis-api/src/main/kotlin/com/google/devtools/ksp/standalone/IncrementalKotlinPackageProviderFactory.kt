package com.google.devtools.ksp.standalone

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinCompositePackageProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.packages.KotlinStandalonePackageProviderFactory
import org.jetbrains.kotlin.psi.KtFile

@OptIn(KaPlatformInterface::class)
class IncrementalKotlinPackageProviderFactory(
    private val project: Project,
) : KotlinPackageProviderFactory, Disposable {
    private val staticFactories: MutableList<KotlinStandalonePackageProviderFactory> = mutableListOf()

    override fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider {
        val providers = staticFactories.map { it.createPackageProvider(searchScope) }
        return KotlinCompositePackageProvider.create(providers)
    }

    fun update(files: Collection<KtFile>) {
        val staticFactory = KotlinStandalonePackageProviderFactory(project, files, emptyList())
        Disposer.register(this, staticFactory)
        staticFactories.add(staticFactory)
    }

    override fun dispose() {
    }
}
