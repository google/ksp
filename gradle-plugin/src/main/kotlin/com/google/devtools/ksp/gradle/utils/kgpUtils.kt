package com.google.devtools.ksp.gradle.utils

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinBaseApiPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.utils.ObservableSet

internal val KotlinCompilation<*>.allKotlinSourceSetsObservable
    get() = this.allKotlinSourceSets as ObservableSet<KotlinSourceSet>

internal val KotlinCompilation<*>.kotlinSourceSetsObservable
    get() = this.kotlinSourceSets as ObservableSet<KotlinSourceSet>

fun Project.isKotlinBaseApiPluginApplied() = plugins.findPlugin(KotlinBaseApiPlugin::class.java) != null

fun Project.isKotlinAndroidPluginApplied() = pluginManager.hasPlugin("org.jetbrains.kotlin.android")
