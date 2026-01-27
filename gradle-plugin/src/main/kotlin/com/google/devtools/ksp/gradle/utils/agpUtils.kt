package com.google.devtools.ksp.gradle.utils

import com.android.build.api.AndroidPluginVersion
import org.gradle.api.Project

fun Project.getAgpVersion(): AndroidPluginVersion? = try {
    this.extensions
        .findByType(com.android.build.api.variant.AndroidComponentsExtension::class.java)
        ?.pluginVersion
} catch (e: NoClassDefFoundError) {
    // AGP not applied
    null
} catch (e: Exception) {
    // Perhaps a version of AGP before pluginVersion API was added.
    null
}

fun Project.isAgpBuiltInKotlinUsed() = isKotlinBaseApiPluginApplied() && isKotlinAndroidPluginApplied().not()

fun checkMinimumAgpVersion(pluginVersion: AndroidPluginVersion) {
    if (pluginVersion < MINIMUM_SUPPORTED_AGP_VERSION) {
        throw RuntimeException(
            "The minimum supported AGP version is ${MINIMUM_SUPPORTED_AGP_VERSION.version}. " +
                "Please upgrade the AGP version in your project."
        )
    }
}

/**
 * Returns false for AGP versions 8.10.0-alpha03 or higher.
 *
 * Returns true for older AGP versions or when AGP version cannot be determined.
 */
fun Project.useLegacyVariantApi(): Boolean {
    val agpVersion = project.getAgpVersion() ?: return true

    // Fall back to using the legacy Variant API if the AGP version can't be determined for now.
    return agpVersion < AndroidPluginVersion(8, 10, 0).alpha(3)
}

/**
 * Returns true for AGP version is 8.12.0-alpha06 or higher.
 * That is the version where addGeneratedSourceDirectories API was fixed
 */
fun Project.canUseAddGeneratedSourceDirectoriesApi(): Boolean {
    val agpVersion = project.getAgpVersion() ?: return false
    return agpVersion >= AndroidPluginVersion(8, 12, 0).alpha(6)
}

fun Project.canUseInternalKspApis(): Boolean {
    val agpVersion = project.getAgpVersion() ?: return false
    return agpVersion >= AndroidPluginVersion(9, 0, 0).alpha(14)
}

/**
 * Defines the minimum supported Android Gradle Plugin (AGP) version.
 *
 * KSP aims to support AGP versions released approximately within the last year
 * from the current KSP release date.
 */
val MINIMUM_SUPPORTED_AGP_VERSION = AndroidPluginVersion(8, 3, 0)
