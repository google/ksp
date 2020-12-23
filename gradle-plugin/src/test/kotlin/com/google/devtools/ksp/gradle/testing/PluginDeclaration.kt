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
 * Value class to hold a plugin declaration information.
 *
 * When the project runner is created, [KspIntegrationTestRule] makes necessary updates on the
 * gradle files to add the plugin.
 *
 * To create an instance, use the helper methods in the companion.
 */
data class PluginDeclaration private constructor(
    val text: String,
    val version: String
) {
    fun toCode() = text

    companion object {
        /**
         * Creates a plugin declaration with the given id and version.
         */
        fun id(id: String, version: String) = PluginDeclaration("id(\"$id\")", version)

        /**
         * Creates a kotlin plugin declaration with the given id and version.
         */
        fun kotlin(id: String, version: String) = PluginDeclaration("kotlin(\"$id\")", version)
    }
}
