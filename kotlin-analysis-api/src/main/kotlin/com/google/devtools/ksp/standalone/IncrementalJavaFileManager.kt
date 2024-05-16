package com.google.devtools.ksp.standalone

import com.intellij.core.CorePackageIndex
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.roots.PackageIndex
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCliJavaFileManagerImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.computeDefaultRootModules
import org.jetbrains.kotlin.cli.jvm.compiler.getJavaModuleRoots
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesDynamicCompoundIndex
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import org.jetbrains.kotlin.cli.jvm.index.SingleJavaFileRootsIndex
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.JavaModuleGraph
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl

class IncrementalJavaFileManager(val environment: KotlinCoreProjectEnvironment) {
    lateinit var rootsIndex: JvmDependenciesDynamicCompoundIndex
    lateinit var packagePartProviders: List<JvmPackagePartProvider>
    val singleJavaFileRoots = mutableListOf<JavaRoot>()

    fun initialize(
        modules: List<KtModule>,
        sourceFiles: List<PsiJavaFile>,
    ) {
        val project = environment.project
        val javaFileManager = project.getService(JavaFileManager::class.java) as KotlinCliJavaFileManagerImpl
        val javaModuleFinder = CliJavaModuleFinder(null, null, javaFileManager, project, null)
        val javaModuleGraph = JavaModuleGraph(javaModuleFinder)
        val allSourceFileRoots = sourceFiles.map { JavaRoot(it.virtualFile, JavaRoot.RootType.SOURCE) }
        val jdkRoots = getDefaultJdkModuleRoots(javaModuleFinder, javaModuleGraph)
        val libraryRoots = StandaloneProjectFactory.getAllBinaryRoots(modules, environment)

        val rootsWithSingleJavaFileRoots = buildList {
            addAll(libraryRoots)
            addAll(allSourceFileRoots)
            addAll(jdkRoots)
        }

        val (roots, newSingleJavaFileRoots) = rootsWithSingleJavaFileRoots.partition { (file) ->
            file.isDirectory || file.extension != JavaFileType.DEFAULT_EXTENSION
        }

        singleJavaFileRoots.addAll(newSingleJavaFileRoots)

        rootsIndex = JvmDependenciesDynamicCompoundIndex().apply {
            addIndex(JvmDependenciesIndexImpl(roots))
        }

        val corePackageIndex = project.getService(PackageIndex::class.java) as CorePackageIndex
        roots.forEach { javaRoot ->
            if (javaRoot.file.isDirectory) {
                if (javaRoot.type == JavaRoot.RootType.SOURCE) {
                    // NB: [JavaCoreProjectEnvironment#addSourcesToClasspath] calls:
                    //   1) [CoreJavaFileManager#addToClasspath], which is used to look up Java roots;
                    //   2) [CorePackageIndex#addToClasspath], which populates [PackageIndex]; and
                    //   3) [FileIndexFacade#addLibraryRoot], which conflicts with this SOURCE root when generating a library scope.
                    // Thus, here we manually call first two, which are used to:
                    //   1) create [PsiPackage] as a package resolution result; and
                    //   2) find directories by package name.
                    // With both supports, annotations defined in package-info.java can be properly propagated.
                    javaFileManager.addToClasspath(javaRoot.file)
                    corePackageIndex.addToClasspath(javaRoot.file)
                } else {
                    environment.addSourcesToClasspath(javaRoot.file)
                }
            }
        }

        packagePartProviders = listOf(
            StandaloneProjectFactory.createPackagePartsProvider(
                libraryRoots + jdkRoots,
                LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST)
            ).invoke(ProjectScope.getLibrariesScope(project))
        )

        javaFileManager.initialize(
            rootsIndex,
            packagePartProviders,
            SingleJavaFileRootsIndex(singleJavaFileRoots),
            true
        )
    }

    fun add(sourceFiles: List<PsiJavaFile>) {
        val project = environment.project
        val javaFileManager = project.getService(JavaFileManager::class.java) as KotlinCliJavaFileManagerImpl
        val allSourceFileRoots = sourceFiles.map { JavaRoot(it.virtualFile, JavaRoot.RootType.SOURCE) }

        val (roots, newSingleJavaFileRoots) = allSourceFileRoots.partition { (file) ->
            file.isDirectory || file.extension != JavaFileType.DEFAULT_EXTENSION
        }

        singleJavaFileRoots.addAll(newSingleJavaFileRoots)

        rootsIndex.apply {
            addIndex(JvmDependenciesIndexImpl(roots))
        }

        val corePackageIndex = project.getService(PackageIndex::class.java) as CorePackageIndex
        roots.forEach { javaRoot ->
            if (javaRoot.file.isDirectory) {
                if (javaRoot.type == JavaRoot.RootType.SOURCE) {
                    // NB: [JavaCoreProjectEnvironment#addSourcesToClasspath] calls:
                    //   1) [CoreJavaFileManager#addToClasspath], which is used to look up Java roots;
                    //   2) [CorePackageIndex#addToClasspath], which populates [PackageIndex]; and
                    //   3) [FileIndexFacade#addLibraryRoot], which conflicts with this SOURCE root when generating a library scope.
                    // Thus, here we manually call first two, which are used to:
                    //   1) create [PsiPackage] as a package resolution result; and
                    //   2) find directories by package name.
                    // With both supports, annotations defined in package-info.java can be properly propagated.
                    javaFileManager.addToClasspath(javaRoot.file)
                    corePackageIndex.addToClasspath(javaRoot.file)
                } else {
                    environment.addSourcesToClasspath(javaRoot.file)
                }
            }
        }

        javaFileManager.initialize(
            rootsIndex,
            packagePartProviders,
            SingleJavaFileRootsIndex(singleJavaFileRoots),
            true
        )
    }
}

private fun getDefaultJdkModuleRoots(
    javaModuleFinder: CliJavaModuleFinder,
    javaModuleGraph: JavaModuleGraph
): List<JavaRoot> {
    // In contrast to `ClasspathRootsResolver.addModularRoots`, we do not need to handle automatic Java modules because JDK modules
    // aren't automatic.
    return javaModuleGraph.getAllDependencies(javaModuleFinder.computeDefaultRootModules()).flatMap { moduleName ->
        val module = javaModuleFinder.findModule(moduleName) ?: return@flatMap emptyList<JavaRoot>()
        val result = module.getJavaModuleRoots()
        result
    }
}
