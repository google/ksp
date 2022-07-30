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
class KspConfigurations(private val project: Project, multiplatformEnabled: Boolean) {
    companion object {
        private const val PREFIX = "ksp"
    }

    private val allowAllTargetConfiguration =
        project.findProperty("ksp.allow.all.target.configuration")?.let {
            it.toString().toBoolean()
        } ?: true

    // The "ksp" configuration, applied to every compilations.
    private val configurationForAll = project.configurations.create(PREFIX)

    private val kspMultiplatformExtension: KspMultiplatformExtension? =
        if (multiplatformEnabled) project.extensions.getByType(KspMultiplatformExtension::class.java) else null

    private val kspExtension: KspExtension =
        kspMultiplatformExtension?.kspExtension ?: project.extensions.getByType(KspExtension::class.java)

    private val resolvedSourceSetOptions = mutableMapOf<KotlinSourceSet, SourceSetOptions>()
    private val compilationsConfiguredOrSkipped = mutableSetOf<KotlinCompilation<*>>()

    private fun maybeCreateConfiguration(name: String, readableSetName: String): Configuration {
        // Configurations get created lazily
        // - when decorating a Kotlin project, and
        // - when creating a KSP task.
        // This can occur in any order, depending on when a KSP task is referenced, so it is necessary to
        // tolerate multiple invocations with idempotence.
        return project.configurations.maybeCreate(name).apply {
            description = "KSP dependencies for the '$readableSetName' source set."
            isCanBeResolved = false // we'll resolve the processor classpath config
            isCanBeConsumed = false
            isVisible = false
        }
    }

    private fun maybeCreateConfiguration(compilation: KotlinCompilation<*>) {
        val kspConfigurationName = getKotlinConfigurationName(compilation)
        maybeCreateConfiguration(name = kspConfigurationName, readableSetName = "KSP $compilation")
    }

    private fun getAndroidConfigurationName(target: KotlinTarget, sourceSet: String): String {
        val isMain = sourceSet.endsWith("main", ignoreCase = true)
        val nameWithoutMain = when {
            isMain -> sourceSet.substring(0, sourceSet.length - 4)
            else -> sourceSet
        }
        // Note: on single-platform, target name is conveniently set to "".
        return lowerCamelCased(PREFIX, target.name, nameWithoutMain)
    }

    private fun getKotlinConfigurationName(compilation: KotlinCompilation<*>): String {
        var targetName = compilation.target.targetName

        when (targetName) {
            "jsIr", "jsLegacy" -> targetName = "Js"
            "metadata" -> {
                // This reversal of target and compilation name is unnecessarily complicated, but retains
                // backward compatibility for dependency-based configuration via `dependencies { add(...) }`.
                when (compilation.name) {
                    KotlinCompilation.MAIN_COMPILATION_NAME, "commonMain" ->
                        return "${PREFIX}CommonMainMetadata"
                }
            }
        }

        return if (compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME) {
            lowerCamelCased(PREFIX, targetName)
        } else {
            lowerCamelCased(PREFIX, targetName, compilation.name)
        }
    }

    init {
        project.plugins.withType(KotlinBasePluginWrapper::class.java).configureEach {
            decorateKotlinProject(project)
        }
    }

    private fun decorateKotlinProject(project: Project) {
        when (val kotlin = project.kotlinExtension) {
            is KotlinSingleTargetExtension -> decorateKotlinTarget(kotlin.target)
            is KotlinMultiplatformExtension -> {
                kotlin.targets.configureEach(::decorateKotlinTarget)

                var reported = false
                configurationForAll.dependencies.whenObjectAdded {
                    if (!reported) {
                        reported = true
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
     * Decorate [target]'s source sets (Android) or compilations (non-Android), creating one KSP configuration
     * per source set or compilation.
     *
     * For Android, we prefer to use AndroidSourceSets from AGP rather than [KotlinSourceSet]s.
     * Even though the Kotlin Plugin does create [KotlinSourceSet]s out of AndroidSourceSets
     * ( https://kotlinlang.org/docs/mpp-configure-compilations.html#compilation-of-the-source-set-hierarchy ),
     * there are slight differences between the two - Kotlin creates some extra sets with unexpected word ordering,
     * and things get worse when you add product flavors. So, we use AGP sets as the source of truth.
     * Android configurations are named ksp<SourceSet>, stripping a "Main" suffix (so what would be "kspJvmMain"
     * becomes "kspJvm").
     *
     * Non-Android compilations are named ksp<Target><Compilation> except for main compilations, which are
     * named ksp<Target>.
     */
    private fun decorateKotlinTarget(target: KotlinTarget) {
        // TODO: Check whether special AGP handling is still necessary.
        if (target.platformType == KotlinPlatformType.androidJvm) {
            AndroidPluginIntegration.forEachAndroidSourceSet(target.project) { sourceSet ->
                maybeCreateConfiguration(
                    name = getAndroidConfigurationName(target, sourceSet),
                    readableSetName = "$sourceSet (Android)"
                )
            }
        } else {
            target.compilations.configureEach(::maybeCreateConfiguration)
        }
    }

    /**
     * Returns the configurations relevant for [compilation].
     */
    fun find(compilation: KotlinCompilation<*>): Set<Configuration> {
        configureCompilation(compilation)

        val configurationNames = mutableListOf(getKotlinConfigurationName(compilation))

        // TODO: Check whether special AGP handling is still necessary.
        if (compilation.platformType == KotlinPlatformType.androidJvm) {
            compilation as KotlinJvmAndroidCompilation
            AndroidPluginIntegration.getCompilationSourceSets(compilation).mapTo(configurationNames) {
                getAndroidConfigurationName(compilation.target, it)
            }
        }

        // Include the `ksp` configuration, if it exists, for all compilations.
        if (configurationNames.isNotEmpty() && allowAllTargetConfiguration) {
            configurationNames.add(configurationForAll.name)
        }

        return configurationNames.mapNotNull {
            project.configurations.findByName(it)
        }.toSet()
    }

    private fun configureCompilation(compilation: KotlinCompilation<*>) {
        if (compilation in compilationsConfiguredOrSkipped)
            return

        compilationsConfiguredOrSkipped.add(compilation)

        val sourceSetOptions = resolvedSourceSetOptions(compilation)
        if (sourceSetOptions.enabled == true) {
            sourceSetOptions.processor?.let { processor ->
                maybeCreateConfiguration(compilation)
                project.dependencies.add(getKotlinConfigurationName(compilation), processor)
            }
        }
    }

    /**
     * Returns the source set-dependent options for [kotlinCompilation], with hierarchically resolved inheritance.
     *
     * Source set options are put together by following source set dependencies in bottom-up order.
     * (Inheriting incompatible KSP configurations from multiple parents is discouraged as the
     * evaluation order in such cases is considered undefined.)
     *
     * The result's properties are guaranteed to be non-null, as each of them eventually inherits a non-null value
     * from global options.
     */
    internal fun resolvedSourceSetOptions(kotlinCompilation: KotlinCompilation<*>): SourceSetOptions =
        resolvedSourceSetOptions.computeIfAbsent(kotlinCompilation.defaultSourceSet) { compilationSourceSet ->
            kspMultiplatformExtension?.let { kspMultiplatformExtension ->
                val result = SourceSetOptions().inheritFrom(
                    kspMultiplatformExtension.sourceSetOptions(compilationSourceSet),
                    initializationMode = true
                )

                kotlinCompilation.parentSourceSetsBottomUp()
                    .map { kspMultiplatformExtension.sourceSetOptions(it) }
                    .takeWhile { it.inheritable }
                    .forEach { parentOptions ->
                        result.inheritFrom(parentOptions)
                    }

                // Finally, complete missing options with global options (which are always inheritable).
                result.inheritFrom(kspMultiplatformExtension.globalSourceSetOptions())
            } ?: kspExtension.globalSourceSetOptions()
        }
}

internal fun KotlinSourceSet.bottomUpDependencies(): Sequence<KotlinSourceSet> = sequence {
    yield(this@bottomUpDependencies)
    dependsOn.forEach {
        yieldAll(it.bottomUpDependencies())
    }
}

internal fun KotlinCompilation<*>.parentSourceSetsBottomUp(): Sequence<KotlinSourceSet> =
    defaultSourceSet.bottomUpDependencies()
        .drop(1) // exclude the compilation source set
        .distinct() // avoid repetitions if multiple parents are present

internal fun lowerCamelCased(vararg parts: String): String {
    return parts.joinToString("") { part ->
        part.replaceFirstChar { it.uppercase() }
    }.replaceFirstChar { it.lowercase() }
}
