/*
 * Copyright 2020 Google LLC
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

package com.google.devtools.ksp.gradle.model.builder

import com.google.devtools.ksp.gradle.KspExtension
import com.google.devtools.ksp.gradle.model.Ksp
import com.google.devtools.ksp.gradle.model.impl.KspImpl
import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder

/**
 * [ToolingModelBuilder] for [Ksp] models.
 * This model builder is registered for Kotlin All Open sub-plugin.
 */
class KspModelBuilder : ToolingModelBuilder {

    override fun canBuild(modelName: String): Boolean {
        return modelName == Ksp::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): Any? {
        if (modelName == Ksp::class.java.name) {
            val extension = project.extensions.getByType(KspExtension::class.java)
            return KspImpl(project.name)
        }
        return null
    }
}
