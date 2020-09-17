/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.findPsi
import com.google.devtools.ksp.symbol.impl.kotlin.KSPropertyDeclarationImpl
import com.google.devtools.ksp.symbol.impl.kotlin.KSPropertyDeclarationParameterImpl
import com.google.devtools.ksp.symbol.impl.toFunctionKSModifiers
import com.google.devtools.ksp.symbol.impl.toKSModifiers
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty

abstract class KSPropertyAccessorDescriptorImpl(val descriptor: PropertyAccessorDescriptor) : KSPropertyAccessor {
    override val origin: Origin = Origin.CLASS

    override val receiver: KSPropertyDeclaration by lazy {
        KSPropertyDeclarationDescriptorImpl.getCached(descriptor.correspondingProperty)
    }

    override val location: Location = NonExistLocation

    override val annotations: List<KSAnnotation> by lazy {
        descriptor.annotations.map { KSAnnotationDescriptorImpl.getCached(it) }
    }

    override val modifiers: Set<Modifier> by lazy {
        val modifiers = mutableSetOf<Modifier>()
        modifiers.addAll(descriptor.toKSModifiers())
        modifiers.addAll(descriptor.toFunctionKSModifiers())
        modifiers
    }
}