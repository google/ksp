/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.gradle.model.impl

import com.google.devtools.kotlin.symbol.processing.gradle.model.Ksp
import java.io.Serializable

/**
 * Implementation of the [Ksp] interface.
 */
data class KspImpl(
    override val name: String
) : Ksp, Serializable {

    override val modelVersion = serialVersionUID

    companion object {
        private const val serialVersionUID = 1L
    }
}