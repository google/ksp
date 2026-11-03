package com.google.devtools.ksp.standalone

import com.intellij.core.CorePackageIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.CollectionQuery
import com.intellij.util.Query
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.name.FqName

/**
 * A [CorePackageIndex] implementation that delegates package directory lookups to
 * [JvmDependenciesIndex].
 *
 * By default, [CorePackageIndex] linearly scans all classpath roots on every package lookup.
 * Delegating to [index] avoids duplicate directory traversal and reuses the package caches already
 * maintained by [IncrementalJavaFileManager]'s dependencies index.
 */
class JvmDependenciesPackageIndex(
    private val index: JvmDependenciesIndex,
) : CorePackageIndex() {
    override fun getDirectoriesByPackageName(
        packageName: String,
        includeLibrarySources: Boolean,
    ): Array<VirtualFile> = findDirectoriesByPackageName(packageName).toTypedArray()

    override fun getDirsByPackageName(
        packageName: String,
        includeLibrarySources: Boolean,
    ): Query<VirtualFile> = CollectionQuery(findDirectoriesByPackageName(packageName))

    private fun findDirectoriesByPackageName(packageName: String): List<VirtualFile> =
        buildList {
            index.traverseDirectoriesInPackage(FqName(packageName)) { dir, _ ->
                add(dir)
                true
            }
        }
}
