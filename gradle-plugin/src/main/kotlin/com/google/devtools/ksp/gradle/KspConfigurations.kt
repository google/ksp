package com.google.devtools.ksp.gradle

import org.gradle.api.InvalidUserCodeException
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

    // Store all ksp configurations for quick retrieval.
    private val kotlinConfigurations = mutableMapOf<KotlinSourceSet, Configuration>()
    private val androidConfigurations = mutableMapOf<String, Configuration>()

    @OptIn(ExperimentalStdlibApi::class)
    private fun <T : Any> saveConfiguration(
        ownerSet: T,
        ownerName: String,
        name: String,
        cache: MutableMap<T, Configuration>
    ): Configuration {
        val configName = PREFIX + name.replaceFirstChar { it.uppercase() }
        // maybeCreate to be future-proof, but we should never have a duplicate with current logic
        val config = project.configurations.maybeCreate(configName).apply {
            description = "KSP dependencies for the '$ownerName' source set."
            isCanBeResolved = false // we'll resolve the processor classpath config
            isCanBeConsumed = false
            isVisible = false
        }
        cache[ownerSet] = config
        return config
    }

    private fun saveKotlinConfiguration(owner: KotlinSourceSet, name: String) =
        saveConfiguration(owner, owner.name, name, kotlinConfigurations)

    private fun saveAndroidConfiguration(key: String, name: String) =
        saveConfiguration(key, "$key (Android)", name, androidConfigurations)

    init {
        project.plugins.withType(KotlinBasePluginWrapper::class.java).configureEach {
            // 1.6.0: decorateKotlinProject(project.kotlinExtension)?
            decorateKotlinProject(project.extensions.getByName("kotlin") as KotlinProjectExtension, project)
        }
    }

    private fun decorateKotlinProject(kotlin: KotlinProjectExtension, project: Project) {
        when (kotlin) {
            is KotlinSingleTargetExtension -> decorateKotlinTarget(kotlin.target)
            is KotlinMultiplatformExtension -> {
                kotlin.targets.configureEach(::decorateKotlinTarget)

                // Adding multiplatform configuration removed support for the root ksp configuration.
                // Try to make this breaking change less breaking by adding a clear error.
                project.configurations.create("ksp").dependencies.all {
                    throw InvalidUserCodeException(
                        "The 'ksp' configuration cannot be used in Kotlin Multiplatform projects. " +
                            "Please use target-specific configurations like 'kspJvm' instead."
                    )
                }
            }
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
            AndroidPluginIntegration.forEachAndroidSourceSet(target.project) { sourceSet ->
                val isMain = sourceSet.endsWith("main", ignoreCase = true)
                val nameWithoutMain = when {
                    isMain -> sourceSet.substring(0, sourceSet.length - 4)
                    else -> sourceSet
                }
                val nameWithTargetPrefix = when {
                    target.name.isEmpty() -> nameWithoutMain
                    else -> target.name + nameWithoutMain.replaceFirstChar { it.uppercase() }
                }
                saveAndroidConfiguration(sourceSet, nameWithTargetPrefix)
            }
        } else {
            target.compilations.configureEach { compilation ->
                val isMain = compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME
                compilation.kotlinSourceSets.forEach { sourceSet ->
                    val isDefault = sourceSet.name == compilation.defaultSourceSetName
                    // Note: on single-platform, target name is conveniently set to "" so this resolves to "ksp".
                    val name = if (isMain && isDefault) target.name else sourceSet.name
                    saveKotlinConfiguration(sourceSet, name)
                }
            }
        }
    }

    /**
     * Returns the user-facing configurations involved in the given compilation.
     * We use [KotlinCompilation.kotlinSourceSets], not [KotlinCompilation.allKotlinSourceSets] for a few reasons:
     * 1) consistency with how we created the configurations. For example, all* can return user-defined sets
     *    that don't belong to any compilation, like user-defined intermediate source sets (e.g. iosMain).
     *    These do not currently have their own ksp configuration.
     * 2) all* can return sets belonging to other [KotlinCompilation]s
     *
     * See test: SourceSetConfigurationsTest.configurationsForMultiplatformApp_doesNotCrossCompilationBoundaries
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
