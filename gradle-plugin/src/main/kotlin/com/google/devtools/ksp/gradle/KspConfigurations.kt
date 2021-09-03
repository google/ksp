package com.google.devtools.ksp.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import java.util.*

/**
 * Creates and retrieves ksp-related configurations.
 */
class KspConfigurations(private val project: Project) {
    companion object {
        const val ROOT = "ksp"
    }

    // "ksp" configuration. In single-platform projects, it is applied to the "main" sourceSet.
    // In multi-platform projects, it is applied to the "main" sourceSet of all targets.
    private val rootMainConfiguration = project.configurations.create(ROOT)

    // Stores all saved configurations for quick access.
    private val configurations = mutableMapOf<KotlinSourceSet, Configuration>()

    @OptIn(ExperimentalStdlibApi::class)
    private fun addConfiguration(owner: KotlinSourceSet, parent: Configuration?, name: String): Configuration {
        val configName = ROOT + name.replaceFirstChar { it.uppercase() }
        val existingConfig = project.configurations.findByName(configName)
        if (existingConfig != null && configName != ROOT) {
            error("Unexpected duplicate configuration ($configName).")
        }

        val config = existingConfig ?: project.configurations.create(configName)
        if (parent != null) config.extendsFrom(parent)
        configurations[owner] = config
        return config
    }

    init {
        project.plugins.withType(KotlinBasePluginWrapper::class.java).configureEach {
            // 1.6.0: decorateKotlinProject(project.kotlinExtension)
            decorateKotlinProject(project.extensions.getByName("kotlin") as KotlinProjectExtension)
        }
    }

    private fun decorateKotlinProject(kotlin: KotlinProjectExtension) {
        when (kotlin) {
            is KotlinMultiplatformExtension -> kotlin.targets.configureEach(::decorateKotlinTarget)
            is KotlinSingleTargetExtension -> decorateKotlinTarget(kotlin.target)
        }
    }

    private fun decorateKotlinTarget(target: KotlinTarget) {
        if (target.platformType == KotlinPlatformType.androidJvm) {
            /**
             * TODO: Android might need special handling. Discuss. Tricky points:
             * 1) KotlinSourceSets are defined in terms of AGP Variants - a resolved, compilable entity.
             *    Using them would be consistent with other targets and simple.
             * 2) AGP AndroidSourceSets represent a hierarchy: we have "test", "debug", but also "testDebug"
             *    which depends on the other two. Not clear if this dependency should be reflected in the
             *    configurations.
             * 3) Need to find a way to retrieve the correct configurations in applyToCompilation
             */
            Unit
        } else {
            // We could add target-specific configurations here (kspJvm, parent of kspJvmMain & kspJvmTest)
            // but we decided that kspJvm should actually mean kspJvmMain, which in turn is not created.
            target.compilations.configureEach { compilation ->
                val isMain = compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME
                compilation.kotlinSourceSets.forEach { sourceSet ->
                    val isDefault = sourceSet.name == compilation.defaultSourceSetName
                    decorateKotlinSourceSet(compilation, isMain, sourceSet, isDefault)
                }
            }
        }
    }

    private fun decorateKotlinSourceSet(
        compilation: KotlinCompilation<*>,
        isMainCompilation: Boolean,
        sourceSet: KotlinSourceSet,
        isDefaultSourceSet: Boolean
    ) {
        val parent = if (isMainCompilation) rootMainConfiguration else null
        if (isMainCompilation && isDefaultSourceSet) {
            // Use target name instead of sourceSet name, to avoid creating "kspMain" or "kspJvmMain".
            // Note: on single-platform, target name is conveniently set to "" so this resolves to "ksp".
            addConfiguration(sourceSet, parent, compilation.target.name)
        } else {
            addConfiguration(sourceSet, parent, sourceSet.name)
        }
    }

    /**
     * Returns the user-facing configurations involved in the given compilation.
     * We use [KotlinCompilation.kotlinSourceSets], not [KotlinCompilation.allKotlinSourceSets] for a few reasons:
     * 1) consistency with how we created the configurations
     * 2) all* can return sets belonging to other compilations. In this case the dependency should be tracked
     *    by Gradle at the task level, not by us through configurations.
     * 3) all* can return user-defined sets belonging to no compilation, like intermediate source sets defined
     *    to share code between targets. They do not currently have their own ksp configuration.
     */
    fun find(compilation: KotlinCompilation<*>): Set<Configuration> {
        val sourceSets = compilation.kotlinSourceSets
        return sourceSets.mapNotNull { configurations[it] }.toSet()
    }
}
