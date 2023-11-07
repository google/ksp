package com.google.devtools.ksp.standalone

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticDeclarationProviderFactory
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.KtTypeAlias

class IncrementalKotlinDeclarationProvider(var del: KotlinDeclarationProvider) : KotlinDeclarationProvider() {
    override fun computePackageSetWithTopLevelCallableDeclarations(): Set<String>? {
        return del.computePackageSetWithTopLevelCallableDeclarations()
    }

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        return del.findFilesForFacade(facadeFqName)
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile> {
        return del.findFilesForFacadeByPackage(packageFqName)
    }

    override fun findFilesForScript(scriptFqName: FqName): Collection<KtScript> {
        return del.findFilesForScript(scriptFqName)
    }

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        return del.findInternalFilesForFacade(facadeFqName)
    }

    override fun getAllClassesByClassId(classId: ClassId): Collection<KtClassOrObject> {
        return del.getAllClassesByClassId(classId)
    }

    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> {
        return del.getAllTypeAliasesByClassId(classId)
    }

    override fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration? {
        return del.getClassLikeDeclarationByClassId(classId)
    }

    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<KtFile> {
        return del.getTopLevelCallableFiles(callableId)
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        return del.getTopLevelCallableNamesInPackage(packageFqName)
    }

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> {
        return del.getTopLevelFunctions(callableId)
    }

    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> {
        return del.getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName)
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> {
        return del.getTopLevelProperties(callableId)
    }
}

class IncrementalKotlinDeclarationProviderFactory(
    private val project: Project,
) : KotlinDeclarationProviderFactory() {
    private var provider: IncrementalKotlinDeclarationProvider? = null
    private lateinit var scope: GlobalSearchScope
    private var contextualModule: KtModule? = null
    private var files: Collection<KtFile> = emptyList()
    private lateinit var staticFactory: KotlinDeclarationProviderFactory

    override fun createDeclarationProvider(
        scope: GlobalSearchScope,
        contextualModule: KtModule?
    ): KotlinDeclarationProvider {
        this.scope = scope
        this.contextualModule = contextualModule
        return IncrementalKotlinDeclarationProvider(createDelegateProvider()).also {
            provider = it
        }
    }

    fun update(files: Collection<KtFile>) {
        this.files = files
        this.staticFactory = KotlinStaticDeclarationProviderFactory(project, files)
        provider?.let {
            it.del = createDelegateProvider()
        }
    }

    private fun createDelegateProvider(): KotlinDeclarationProvider {
        return staticFactory.createDeclarationProvider(scope, contextualModule)
    }
}
