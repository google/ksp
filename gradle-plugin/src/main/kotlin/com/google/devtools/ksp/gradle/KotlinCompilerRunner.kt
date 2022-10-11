/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.google.devtools.ksp.gradle

import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.dsl.CompilerCommonOptions
import org.jetbrains.kotlin.gradle.dsl.CompilerJsOptions
import org.jetbrains.kotlin.gradle.dsl.CompilerJvmOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.io.File

interface KotlinCompilerRunner {
    // TODO: Remove those properties when getting into KGP.
    // They should be configured by KGP. For now, they allow KSP to copy the settings from compilation task.

    @get:Internal
    val compilerExecutionStrategy: Property<KotlinCompilerExecutionStrategy>

    @get:Internal
    val useDaemonFallbackStrategy: Property<Boolean>

    @get:Internal
    val kotlinDaemonJvmArguments: ListProperty<String>

    @get:Classpath
    val compilerClasspath: ConfigurableFileCollection
}

interface KotlinJvmCompilerRunner : KotlinCompilerRunner {
    fun runJvmCompilerAsync(
        options: CompilerJvmOptions,
        freeArgs: List<String>,
        sources: List<File>,
        commonSources: List<File>,
        friendPaths: List<File>,
        libraries: List<File>,
        destination: File
    )
}

interface KotlinJsCompilerRunner : KotlinCompilerRunner {
    fun runJsCompilerAsync(
        options: CompilerJsOptions,
        freeArgs: List<String>,
        sources: List<File>,
        commonSources: List<File>,
        friendPaths: List<File>,
        libraries: List<File>,
        destination: File
    )
}

interface KotlinMetadataCompilerRunner : KotlinCompilerRunner {
    fun runMetadataCompilerAsync(
        options: CompilerCommonOptions,
        freeArgs: List<String>,
        sources: List<File>,
        commonSources: List<File>,
        friendPaths: List<File>,
        libraries: List<File>,
        destination: File
    )
}

interface KotlinNativeCompilerRunner : KotlinCompilerRunner {
    fun runNativeCompilerAsync(
        options: CompilerCommonOptions,
        freeArgs: List<String>,
        sources: List<File>,
        commonSources: List<File>,
        friendPaths: List<File>,
        libraries: List<File>,
        destination: File,
        target: String
    )
}

// TODO: Maybe move those functions into proper KGP class when getting into KGP.

// TODO: Remove objectFactory when getting into KGP.
fun createKotlinJvmCompilerRunner(
    task: Task,
    objectFactory: ObjectFactory,
): KotlinJvmCompilerRunner {
    return objectFactory.newInstance<KotlinJvmCompilerRunnerImpl>(task)
}

// TODO: Remove objectFactory when getting into KGP.
fun createKotlinJsCompilerRunner(
    task: Task,
    objectFactory: ObjectFactory,
): KotlinJsCompilerRunner {
    return objectFactory.newInstance<KotlinJsCompilerRunnerImpl>(task)
}

// TODO: Remove objectFactory when getting into KGP.
fun createKotlinMetadataCompilerRunner(
    task: Task,
    objectFactory: ObjectFactory,
): KotlinMetadataCompilerRunner {
    return objectFactory.newInstance<KotlinMetadataCompilerRunnerImpl>(task)
}

// TODO: Remove objectFactory when getting into KGP.
fun createKotlinNativeCompilerRunner(
    task: Task,
    objectFactory: ObjectFactory,
): KotlinNativeCompilerRunner {
    return objectFactory.newInstance<KotlinNativeCompilerRunnerImpl>(task)
}
