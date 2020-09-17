/*
 * Copyright 2010-2020 Google LLC, JetBrains s.r.o and and Kotlin Programming Language contributors.
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


package com.google.devtools.ksp.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.MemberDescriptor
import com.google.devtools.ksp.processing.impl.findActualsInKSDeclaration
import com.google.devtools.ksp.processing.impl.findExpectsInKSDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSExpectActual

class KSExpectActualDescriptorImpl(val descriptor: MemberDescriptor) : KSExpectActual {
    override val isExpect: Boolean = descriptor.isExpect

    override val isActual: Boolean = descriptor.isActual

    private val expects: List<KSDeclaration> by lazy {
        descriptor.findExpectsInKSDeclaration()
    }

    override fun findExpects(): List<KSDeclaration> {
        if (!isActual)
            return emptyList()
        return expects
    }

    private val actuals: List<KSDeclaration> by lazy {
        descriptor.findActualsInKSDeclaration()
    }

    override fun findActuals(): List<KSDeclaration> {
        if (!isExpect)
            return emptyList()
        return actuals
    }
}
