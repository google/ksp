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
        const val ROOT = "ksp"
    }

    // "ksp" configuration. In single-platform projects, it is applied to the "main" sourceSet.
    // In multi-platform projects, it is applied to the "main" sourceSet of all targets.
    private val rootMainConfiguration = project.configurations.create(ROOT)

    // Stores all saved configurations for quick access.
    private val kotlinConfigurations = mutableMapOf<KotlinSourceSet, Configuration>()
    private val androidConfigurations = mutableMapOf<String, Configuration>()

    @OptIn(ExperimentalStdlibApi::class)
    private fun <T: Any> saveConfiguration(
        owner: T,
        parent: Configuration?,
        name: String,
        cache: MutableMap<T, Configuration>
    ): Configuration {
        val configName = ROOT + name.replaceFirstChar { it.uppercase() }
        val existingConfig = project.configurations.findByName(configName)
        if (existingConfig != null && configName != ROOT) {
            error("Unexpected duplicate configuration ($configName).")
        }

        val config = existingConfig ?: project.configurations.create(configName)
        if (parent != null && parent.name != configName) {
            config.extendsFrom(parent)
        }
        cache[owner] = config
        return config
    }

    private fun saveKotlinConfiguration(owner: KotlinSourceSet, parent: Configuration?, name: String) =
        saveConfiguration(owner, parent, name, kotlinConfigurations)

    private fun saveAndroidConfiguration(owner: String, parent: Configuration?, name: String) =
        saveConfiguration(owner, parent, name, androidConfigurations)

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

    /**
     * Decorate the source sets belonging to [target].
     * The end goal is to have one KSP configuration per source set. Examples:
     * - in a kotlin-jvm project, we want "ksp" (applied to main set) and "kspTest" (applied to test set)
     * - in a kotlin-multiplatform project, we want "ksp<Target>" and "ksp<Target>Test" for each target.
     * This is done by reading [KotlinCompilation.kotlinSourceSets], which contains appropriately named sets.
     *
     * For Android, we prefer to use AndroidSourceSets from AGP rather than [KotlinSourceSet]s like all other
     * targets. There are very slight differences between the two - this could be re-evaluated in the future,
     * because Kotlin Plugin does already create [KotlinSourceSet]s out of AndroidSourceSets
     * ( https://kotlinlang.org/docs/mpp-configure-compilations.html#compilation-of-the-source-set-hierarchy ).
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
            saveKotlinConfiguration(sourceSet, parent, compilation.target.name)
        } else {
            saveKotlinConfiguration(sourceSet, parent, sourceSet.name)
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
        val kotlinSourceSets = compilation.kotlinSourceSets
        val kotlinConfigurations = kotlinSourceSets.mapNotNull { kotlinConfigurations[it] }
        val androidConfigurations = if (compilation.platformType == KotlinPlatformType.androidJvm) {
            val androidSourceSets = AndroidPluginIntegration.getCompilationSourceSets(compilation as KotlinJvmAndroidCompilation)
            androidSourceSets.mapNotNull { androidConfigurations[it] }
        } else emptyList()
        return (kotlinConfigurations + androidConfigurations).toSet()
    }
}
