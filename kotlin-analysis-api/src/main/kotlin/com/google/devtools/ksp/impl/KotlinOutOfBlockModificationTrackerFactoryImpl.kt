package com.google.devtools.ksp.impl

import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analyzer.ModuleSourceInfoBase

class KotlinOutOfBlockModificationTrackerFactoryImpl : KotlinModificationTrackerFactory() {
    private val projectWide = SimpleModificationTracker()
    private val library = SimpleModificationTracker()
    private val forModule = mutableMapOf<KtSourceModule, SimpleModificationTracker>()

    override fun createProjectWideOutOfBlockModificationTracker(): ModificationTracker {
        return projectWide
    }

    override fun createLibrariesModificationTracker(): ModificationTracker {
        return library
    }

    override fun createModuleWithoutDependenciesOutOfBlockModificationTracker(module: KtSourceModule): ModificationTracker {
        return forModule.getOrPut(module) { SimpleModificationTracker() }
    }

    override fun incrementModificationsCount() {
        projectWide.incModificationCount()
        library.incModificationCount()
        forModule.values.forEach { it.incModificationCount() }
    }
}
