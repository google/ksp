package com.google.devtools.ksp.gradle.utils

import org.gradle.api.Project
import org.gradle.api.configuration.BuildFeatures
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

/**
 * Verifies that [kgpVersion] meets [MINIMUM_SUPPORTED_KGP_VERSION].
 *
 * @throws RuntimeException if [kgpVersion] is below [MINIMUM_SUPPORTED_KGP_VERSION].
 */
fun checkMinimumKgpVersion(kgpVersion: KotlinToolingVersion) {
    if (kgpVersion < MINIMUM_SUPPORTED_KGP_VERSION) {
        throw RuntimeException(
            "The minimum supported Kotlin Gradle Plugin version is $MINIMUM_SUPPORTED_KGP_VERSION " +
                "(found $kgpVersion). " +
                "Please upgrade the Kotlin Gradle Plugin version in your project."
        )
    }
}

/**
 * Defines the minimum supported Kotlin Gradle Plugin (KGP) version.
 *
 * KSP's API artifact is compiled with Kotlin languageVersion 2.3, which requires at least
 * Kotlin 2.2 to read its metadata, and AGP 9.0+ built-in Kotlin bundles KGP 2.2.10.
 */
val MINIMUM_SUPPORTED_KGP_VERSION = KotlinToolingVersion("2.2.10")

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
    return objects.newInstance(KspBuildFeatures::class.java).buildFeatures.isolatedProjects.active.get()
}

/**
 * Holder for the injected [BuildFeatures] service, allowing [enableProjectIsolationCompatibleCodepath]
 * to obtain [BuildFeatures] via [org.gradle.api.model.ObjectFactory.newInstance].
 */
internal abstract class KspBuildFeatures {
    @get:Inject
    abstract val buildFeatures: BuildFeatures
}
