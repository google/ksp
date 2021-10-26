package com.google.devtools.ksp.impl

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

class KotlinPackageProviderImpl(
    scope: GlobalSearchScope,
    files: Collection<KtFile>
) : KotlinPackageProvider() {
    private val filesInScope = files.filter { scope.contains(it.virtualFile) }


    override fun doKotlinPackageExists(packageFqName: FqName): Boolean {
        return filesInScope.any { it.packageFqName == packageFqName }
    }

    override fun getKotlinSubPackageFqNames(packageFqName: FqName): Set<Name> {
        TODO("Not yet implemented")
    }
}
