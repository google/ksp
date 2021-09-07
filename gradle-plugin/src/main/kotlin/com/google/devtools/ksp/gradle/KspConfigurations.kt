package com.google.devtools.ksp.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation

/**
 * Creates and retrieves ksp-related configurations.
 */
class KspConfigurations(private val project: Project) {
    companion object {
        private const val PREFIX = "ksp"
    }

    // "ksp" configuration, applied to the "main" source set of all targets.
    private val rootMainConfiguration = project.configurations.create(PREFIX)

    // Store all ksp configurations for quick retrieval.
    private val kotlinConfigurations = mutableMapOf<KotlinSourceSet, Configuration>()
    private val androidConfigurations = mutableMapOf<String, Configuration>()

    @OptIn(ExperimentalStdlibApi::class)
    private fun <T : Any> saveConfiguration(
        owner: T,
        parent: Configuration?,
        name: String,
        cache: MutableMap<T, Configuration>
    ): Configuration {
        val configName = PREFIX + name.replaceFirstChar { it.uppercase() }
        val config = if (configName == parent?.name) {
            // trying to add config with same parent name. Can happen, for example
            // with ksp<Target> when <Target> is "". Just use parent.
            parent
        } else {
            // maybeCreate to be future-proof, but we should never have a duplicate with current logic
            project.configurations.maybeCreate(configName).apply {
                if (parent != null) extendsFrom(parent)
            }
        }!!
        cache[owner] = config
        return config
    }

    private fun saveKotlinConfiguration(owner: KotlinSourceSet, parent: Configuration?, name: String) =
        saveConfiguration(owner, parent, name, kotlinConfigurations)

    private fun saveAndroidConfiguration(owner: String, parent: Configuration?, name: String) =
        saveConfiguration(owner, parent, name, androidConfigurations)

    init {
        project.plugins.withType(KotlinBasePluginWrapper::class.java).configureEach {
            // 1.6.0: decorateKotlinProject(project.kotlinExtension)?
            decorateKotlinProject(project.extensions.getByName("kotlin") as KotlinProjectExtension)
        }
    }

    private fun decorateKotlinProject(kotlin: KotlinProjectExtension) {
        when (kotlin) {
            is KotlinMultiplatformExtension -> kotlin.targets.configureEach(::decorateKotlinTarget)
            is KotlinSingleTargetExtension -> decorateKotlinTarget(kotlin.target)
        }
    }

    /**
     * Decorate the [KotlinSourceSet]s belonging to [target] to create one KSP configuration per source set,
     * named ksp<SourceSet>. The only exception is the main source set, for which we avoid using the
     * "main" suffix (so what would be "kspJvmMain" becomes "kspJvm").
     *
     * For Android, we prefer to use AndroidSourceSets from AGP rather than [KotlinSourceSet]s.
     * Even though the Kotlin Plugin does create [KotlinSourceSet]s out of AndroidSourceSets
     * ( https://kotlinlang.org/docs/mpp-configure-compilations.html#compilation-of-the-source-set-hierarchy ),
     * there are slight differences between the two - Kotlin creates some extra sets with unexpected word ordering,
     * and things get worse when you add product flavors. So, we use AGP sets as the source of truth.
     */
    private fun decorateKotlinTarget(target: KotlinTarget) {
        if (target.platformType == KotlinPlatformType.androidJvm) {
            AndroidPluginIntegration.findSourceSets(target.project) { setName ->
                val isMain = setName.endsWith("main", ignoreCase = true)
                val nameWithoutMain = when {
                    isMain -> setName.substring(0, setName.length - 4)
                    else -> setName
                }
                val nameWithTargetPrefix = when {
                    target.name.isEmpty() -> nameWithoutMain
                    else -> target.name + nameWithoutMain.replaceFirstChar { it.uppercase() }
                }
                val parent = if (isMain) rootMainConfiguration else null
                saveAndroidConfiguration(setName, parent, nameWithTargetPrefix)
            }
        } else {
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
            saveKotlinConfiguration(sourceSet, parent, compilation.target.name)
        } else {
            saveKotlinConfiguration(sourceSet, parent, sourceSet.name)
        }
    }

    /**
     * Returns the user-facing configurations involved in the given compilation.
     * We use [KotlinCompilation.kotlinSourceSets], not [KotlinCompilation.allKotlinSourceSets] for a few reasons:
     * 1) consistency with how we created the configurations. For example, all* can return user-defined sets
     *    that don't belong to any compilation, like user-defined intermediate source sets (e.g. iosMain).
     *    These do not currently have their own ksp configuration.
     * 2) all* can return sets belonging to other [KotlinCompilation]s
     */
    fun find(compilation: KotlinCompilation<*>): Set<Configuration> {
        val kotlinConfigurations = compilation.kotlinSourceSets.mapNotNull { kotlinConfigurations[it] }
        val androidConfigurations = if (compilation.platformType == KotlinPlatformType.androidJvm) {
            compilation as KotlinJvmAndroidCompilation
            val androidSourceSets = AndroidPluginIntegration.getCompilationSourceSets(compilation)
            androidSourceSets.mapNotNull { androidConfigurations[it] }
        } else emptyList()
        return (kotlinConfigurations + androidConfigurations).toSet()
    }
}
