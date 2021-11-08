package com.google.devtools.ksp.impl

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.formatting.Formatter
import com.intellij.formatting.FormatterImpl
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.AsyncExecutionService
import com.intellij.openapi.application.impl.AsyncExecutionServiceImpl
import com.intellij.openapi.editor.impl.DocumentWriteAccessGuard
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileEditor.impl.FileEditorPsiTreeChangeListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.pom.PomModel
import com.intellij.pom.tree.TreeAspect
import com.intellij.psi.PsiTreeChangeListener
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.DocumentCommitProcessor
import com.intellij.psi.impl.DocumentCommitThread
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.impl.source.codeStyle.CodeStyleManagerImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.analysis.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.analyseWithReadAction
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.FirSealedClassInheritorsProcessorFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.PackagePartProviderFactory
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProvider
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.providers.*
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.deserialization.ClassData
import java.io.File

@OptIn(InvalidWayOfUsingAnalysisSession::class)
fun registerComponents(project: MockProject, environment: KotlinCoreEnvironment, ktFiles: List<KtFile>) {

    project.picoContainer.registerComponentInstance(
        KtAnalysisSessionProvider::class.qualifiedName,
        KtFirAnalysisSessionProvider(project)
    )

    project.picoContainer.registerComponentInstance(
        ProjectStructureProvider::class.qualifiedName,
        ProjectStructureProviderImpl()
    )

    project.picoContainer.registerComponentInstance(
        KotlinModificationTrackerFactory::class.qualifiedName,
        KotlinOutOfBlockModificationTrackerFactoryImpl()
    )

    RegisterComponentService.registerFirIdeResolveStateService(project)

    project.picoContainer.registerComponentInstance(
        KotlinDeclarationProviderFactory::class.qualifiedName,
        object : KotlinDeclarationProviderFactory() {
            override fun createDeclarationProvider(searchScope: GlobalSearchScope): KotlinDeclarationProvider {
                return DeclarationProviderImpl(searchScope, ktFiles.filter { searchScope.contains(it.virtualFile) })
            }
        }
    )

    project.picoContainer.registerComponentInstance(
        KotlinPackageProviderFactory::class.qualifiedName,
        object : KotlinPackageProviderFactory() {
            override fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider {
                return KotlinPackageProviderImpl(searchScope, ktFiles.filter { searchScope.contains(it.virtualFile) })
            }
        }
    )

    project.picoContainer.registerComponentInstance(
        FirSealedClassInheritorsProcessorFactory::class.qualifiedName,
        FirSealedClassInheritorsProcessorFactoryImpl()
    )
    project.picoContainer.registerComponentInstance(
        PackagePartProviderFactory::class.qualifiedName,
        object : PackagePartProviderFactory() {
            override fun createPackagePartProviderForLibrary(scope: GlobalSearchScope): PackagePartProvider {
                return object : PackagePartProvider {
                    override fun findPackageParts(packageFqName: String): List<String> {
                        return emptyList()
                    }

                    override fun getAllOptionalAnnotationClasses(): List<ClassData> {
                        return emptyList()
                    }

                    override fun getAnnotationsOnBinaryModule(moduleName: String): List<ClassId> {
                        return emptyList()
                    }
                }
            }
        }
    )

    val application = ApplicationManager.getApplication() as MockApplication
    KotlinCoreEnvironment.underApplicationLock {
        application.registerService(
            KotlinReferenceProviderContributor::class.java, KotlinFirReferenceContributor::class.java
        )
        application.registerService(DocumentCommitProcessor::class.java, DocumentCommitThread::class.java)
        application.registerService(AsyncExecutionService::class.java, AsyncExecutionServiceImpl::class.java)
        application.registerService(Formatter::class.java, FormatterImpl::class.java)
    }

    project.picoContainer.registerComponentInstance(
        CodeStyleManager::class.qualifiedName,
        CodeStyleManagerImpl(project)
    )
    project.picoContainer.registerComponentInstance(
        TreeAspect::class.qualifiedName,
        TreeAspect()
    )
    project.picoContainer.registerComponentInstance(
        PomModel::class.qualifiedName,
        HackPomModel(project)
    )

    project.picoContainer.registerComponentInstance(
        KtModuleScopeProvider::class.qualifiedName,
        KtModuleScopeProviderImpl()
    )

    CoreApplicationEnvironment.registerExtensionPoint(
        Extensions.getRootArea(),
        DocumentWriteAccessGuard.EP_NAME,
        DocumentWriteAccessGuard::class.java
    )
    CoreApplicationEnvironment.registerExtensionPoint(
        project.extensionArea,
        PsiTreeChangeListener.EP,
        FileEditorPsiTreeChangeListener::class.java
    )
}
fun findSomeReference(ktFile: KtFile): KtReference? {
    for (i in 1..300) {
        val reference = ktFile.findReferenceAt(i)
        if (reference != null && reference is KtReference)
            return reference
    }

    return null
}

fun main() {
    val compilerConfiguration = CompilerConfiguration()
    compilerConfiguration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    val env = KotlinCoreEnvironment.createForProduction(
        Disposer.newDisposable(), compilerConfiguration,
        EnvironmentConfigFiles.JVM_CONFIG_FILES
    )

    val project = env.project as MockProject
    val factory = PsiFileFactoryImpl(project)

    val file = File("testData/api/hello.kt")
    val text = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim { it <= ' ' }

    val virtualFile = LightVirtualFile("a.kt", KotlinLanguage.INSTANCE, StringUtilRt.convertLineSeparators(text))
    val ktFile = factory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile
    registerComponents(project, env, listOf(ktFile))
    analyseWithReadAction(ktFile) {
        val fileSymbol = ktFile.getFileSymbol()
        val members = fileSymbol.getFileScope().getAllSymbols()
        members.filterIsInstance<KtFunctionSymbol>()
    }
}
