@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
package com.google.devtools.ksp.standalone

import com.intellij.core.CorePackageIndex
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.roots.PackageIndex
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.impl.file.impl.JavaFileManager
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneJvmDependenciesIndex
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCliJavaFileManagerImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesDynamicCompoundIndex
import org.jetbrains.kotlin.cli.jvm.index.SingleJavaFileRootsIndex

class IncrementalJavaFileManager(
    val environment: KotlinCoreProjectEnvironment,
    val rootsIndex: JvmDependenciesDynamicCompoundIndex,
    val packagePartProviders: List<JvmPackagePartProvider>,
    libraryRoots: List<JavaRoot>,
) {
    val singleJavaFileRoots = mutableListOf<JavaRoot>()

    init {
        addRoots(libraryRoots)
    }

    fun add(sourceFiles: Set<PsiJavaFile>) {
        addRoots(sourceFiles.map { JavaRoot(it.virtualFile, JavaRoot.RootType.SOURCE) })
    }

    private fun addRoots(newRoots: List<JavaRoot>) {
        val project = environment.project
        val javaFileManager = project.getService(JavaFileManager::class.java) as KotlinCliJavaFileManagerImpl
        val (roots, newSingleJavaFileRoots) = newRoots.partition { (file) ->
            file.isDirectory || file.extension != JavaFileType.DEFAULT_EXTENSION
        }

        singleJavaFileRoots.addAll(newSingleJavaFileRoots)

        if (roots.isNotEmpty()) {
            rootsIndex.addIndex(KotlinStandaloneJvmDependenciesIndex(roots))
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
            true, null
        )
    }
}
