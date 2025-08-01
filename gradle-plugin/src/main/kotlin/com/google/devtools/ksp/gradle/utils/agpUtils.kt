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
