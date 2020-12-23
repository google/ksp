/*
 * Copyright 2021 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
@file:Suppress("DataClassPrivateConstructor")

package com.google.devtools.ksp.gradle.testing

/**
 * Value class to declare dependencies between test projects.
 * See builder methods in the companion to create an instance of this.
 */
data class DependencyDeclaration private constructor(
    val configuration: String,
    val dependency: String
) {
    fun toCode() = "${configuration}($dependency)"

    companion object {
        /**
         * Creates a module dependency for the given configuration.
         */
        fun module(configuration: String, module: TestModule) =
            DependencyDeclaration(configuration, "project(\":${module.name}\")")

        /**
         * Create an artifact dependency for the given configuration.
         */
        fun artifact(configuration: String, coordinates: String) =
            DependencyDeclaration(configuration, "\"$coordinates\"")

        /**
         * Creates a files dependency for the given configuration.
         */
        fun files(configuration: String, path: String) =
            DependencyDeclaration(configuration, "files(\"$path\")")
    }

}