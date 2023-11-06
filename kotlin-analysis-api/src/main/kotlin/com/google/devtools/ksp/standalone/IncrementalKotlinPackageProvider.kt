package com.google.devtools.ksp.standalone

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticPackageProviderFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile

class IncrementalKotlinPackageProvider(var del: KotlinPackageProvider) : KotlinPackageProvider() {
    override fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean {
        return del.doesKotlinOnlyPackageExist(packageFqName)
    }

    override fun doesPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean {
        return del.doesPackageExist(packageFqName, platform)
    }

    override fun doesPlatformSpecificPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean {
        return del.doesPlatformSpecificPackageExist(packageFqName, platform)
    }

    override fun getKotlinOnlySubPackagesFqNames(packageFqName: FqName, nameFilter: (Name) -> Boolean): Set<Name> {
        return del.getKotlinOnlySubPackagesFqNames(packageFqName, nameFilter)
    }

    override fun getPlatformSpecificSubPackagesFqNames(
        packageFqName: FqName,
        platform: TargetPlatform,
        nameFilter: (Name) -> Boolean
    ): Set<Name> {
        return del.getPlatformSpecificSubPackagesFqNames(packageFqName, platform, nameFilter)
    }

    override fun getSubPackageFqNames(
        packageFqName: FqName,
        platform: TargetPlatform,
        nameFilter: (Name) -> Boolean
    ): Set<Name> {
        return del.getSubPackageFqNames(packageFqName, platform, nameFilter)
    }
}

class IncrementalKotlinPackageProviderFactory(
    private val project: Project,
) : KotlinPackageProviderFactory() {
    private var provider: IncrementalKotlinPackageProvider? = null
    private lateinit var scope: GlobalSearchScope
    private var files: Collection<KtFile> = emptyList()
    private lateinit var staticFactory: KotlinPackageProviderFactory

    override fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider {
        this.scope = searchScope
        return IncrementalKotlinPackageProvider(createDelegateProvider()).also {
            provider = it
        }
    }

    fun update(files: Collection<KtFile>) {
        this.files = files
        this.staticFactory = KotlinStaticPackageProviderFactory(project, files)
        provider?.let {
            it.del = createDelegateProvider()
        }
    }

    private fun createDelegateProvider(): KotlinPackageProvider {
        return staticFactory.createPackageProvider(scope)
    }
}
