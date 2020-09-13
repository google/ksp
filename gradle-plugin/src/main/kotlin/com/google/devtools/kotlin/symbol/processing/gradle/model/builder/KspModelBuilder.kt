/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.gradle.model.builder

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import com.google.devtools.kotlin.symbol.processing.gradle.KspExtension
import com.google.devtools.kotlin.symbol.processing.gradle.model.impl.KspImpl
import com.google.devtools.kotlin.symbol.processing.gradle.model.Ksp

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