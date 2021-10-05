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

    private val allowAllTargetConfiguration =
        project.findProperty("ksp.allow_all_target_configuration")?.let {
            it.toString().toBoolean()
        } ?: true

    // The "ksp" configuration, applied to every compilations.
    private val configurationForAll = project.configurations.create(PREFIX)

    private fun configurationNameOf(vararg parts: String): String {
        return parts.joinToString("") {
            it.replaceFirstChar { it.uppercase() }
        }.replaceFirstChar { it.lowercase() }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun createConfiguration(
        name: String,
        readableSetName: String,
    ): Configuration {
        // maybeCreate to be future-proof, but we should never have a duplicate with current logic
        return project.configurations.maybeCreate(name).apply {
            description = "KSP dependencies for the '$readableSetName' source set."
            isCanBeResolved = false // we'll resolve the processor classpath config
            isCanBeConsumed = false
            isVisible = false
        }
    }

    private fun getAndroidConfigurationName(target: KotlinTarget, sourceSet: String): String {
        val isMain = sourceSet.endsWith("main", ignoreCase = true)
        val nameWithoutMain = when {
            isMain -> sourceSet.substring(0, sourceSet.length - 4)
            else -> sourceSet
        }
        // Note: on single-platform, target name is conveniently set to "".
        return configurationNameOf(PREFIX, target.name, nameWithoutMain)
    }

    private fun getKotlinConfigurationName(compilation: KotlinCompilation<*>, sourceSet: KotlinSourceSet): String {
        val isMain = compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME
        val isDefault = sourceSet.name == compilation.defaultSourceSetName
        // Note: on single-platform, target name is conveniently set to "".
        val name = if (isMain && isDefault) compilation.target.name else sourceSet.name
        return configurationNameOf(PREFIX, name)
    }

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

                project.afterEvaluate {
                    if (configurationForAll.dependencies.isNotEmpty()) {
                        val msg = "The 'ksp' configuration is deprecated in Kotlin Multiplatform projects. " +
                            "Please use target-specific configurations like 'kspJvm' instead."

                        if (allowAllTargetConfiguration) {
                            project.logger.warn(msg)
                        } else {
                            throw InvalidUserCodeException(msg)
                        }
                    }
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
                createConfiguration(
                    name = getAndroidConfigurationName(target, sourceSet),
                    readableSetName = "$sourceSet (Android)"
                )
            }
        } else {
            target.compilations.configureEach { compilation ->
                compilation.kotlinSourceSets.forEach { sourceSet ->
                    createConfiguration(
                        name = getKotlinConfigurationName(compilation, sourceSet),
                        readableSetName = sourceSet.name
                    )
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
        val results = mutableListOf<String>()
        compilation.kotlinSourceSets.mapTo(results) {
            getKotlinConfigurationName(compilation, it)
        }
        if (compilation.platformType == KotlinPlatformType.androidJvm) {
            compilation as KotlinJvmAndroidCompilation
            AndroidPluginIntegration.getCompilationSourceSets(compilation).mapTo(results) {
                getAndroidConfigurationName(compilation.target, it)
            }
        }

        // Include the `ksp` configuration, if it exists, for all compilations.
        if (allowAllTargetConfiguration) {
            results.add(configurationForAll.name)
        }

        return results.mapNotNull {
            compilation.target.project.configurations.findByName(it)
        }.toSet()
    }
}
