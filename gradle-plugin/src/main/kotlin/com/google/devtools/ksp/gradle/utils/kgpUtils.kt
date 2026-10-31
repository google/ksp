package com.google.devtools.ksp.gradle.utils

import org.gradle.api.Project
import org.gradle.api.configuration.BuildFeatures
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinBaseApiPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.utils.ObservableSet
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import javax.inject.Inject

// NOTE: for AGP with built in kotlin enabled and android.disallowKotlinSourceSets=true, this returns empty set
internal val KotlinCompilation<*>.allKotlinSourceSetsObservable
    get() = this.allKotlinSourceSets as ObservableSet<KotlinSourceSet>

internal val KotlinCompilation<*>.kotlinSourceSetsObservable
    get() = this.kotlinSourceSets as ObservableSet<KotlinSourceSet>

fun Project.isKotlinBaseApiPluginApplied() = plugins.withType(KotlinBaseApiPlugin::class.java).firstOrNull() != null

fun Project.isKotlinAndroidPluginApplied() = pluginManager.hasPlugin("org.jetbrains.kotlin.android")

fun Project.isLegacyKaptPluginApplied() = pluginManager.hasPlugin("com.android.legacy-kapt")

fun Project.canUseGeneratedKotlinApi(): Boolean {
    val kotlinVersion = KotlinToolingVersion(getKotlinPluginVersion())
    return kotlinVersion >= KotlinToolingVersion("2.3.0-Beta2")
}

/**
 * Whether KSP should use the Gradle Project Isolation compatible codepath for registering generated sources.
 *
 * `ksp.project.isolation.enabled=true` forces the codepath on, regardless of Gradle's own configuration.
 * Otherwise this follows Gradle's effective Isolated Projects state, which also accounts for the
 * `--isolated-projects` / `--no-isolated-projects` command line options and their precedence over properties.
 */
fun Project.enableProjectIsolationCompatibleCodepath(): Boolean {
    if (providers.gradleProperty("ksp.project.isolation.enabled").orNull == "true") {
        return true
    }
    if (supportsBuildFeatures) {
        return objects.newInstance(KspBuildFeatures::class.java).buildFeatures.isolatedProjects.active.get()
    }
    // BuildFeatures isn't available, approximate Gradle's own property handling.
    return providers.gradleProperty("org.gradle.unsafe.isolated-projects").orNull == "true" ||
        providers.systemProperty("org.gradle.unsafe.isolated-projects").orNull == "true" ||
        providers.gradleProperty("org.gradle.isolated-projects").orNull == "true" ||
        providers.systemProperty("org.gradle.isolated-projects").orNull == "true"
}

// org.gradle.api.configuration.BuildFeatures was introduced in Gradle 8.5.
private val supportsBuildFeatures = GradleVersion.current() >= GradleVersion.version("8.5")

/**
 * Holder for the injected [BuildFeatures] service.
 *
 * Keeping the reference in a separate class means [BuildFeatures] is only loaded when we actually instantiate this,
 * i.e. never on Gradle versions that don't have it.
 */
internal abstract class KspBuildFeatures {
    @get:Inject
    abstract val buildFeatures: BuildFeatures
}
