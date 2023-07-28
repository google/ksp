package com.google.devtools.ksp.gradle

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.utils.ObservableSet

internal val KotlinCompilation<*>.allKotlinSourceSetsObservable
    get() = this.allKotlinSourceSets as ObservableSet<KotlinSourceSet>

internal val KotlinCompilation<*>.kotlinSourceSetsObservable
    get() = this.kotlinSourceSets as ObservableSet<KotlinSourceSet>
