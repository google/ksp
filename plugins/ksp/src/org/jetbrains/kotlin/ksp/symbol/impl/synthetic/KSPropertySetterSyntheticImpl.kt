/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.synthetic

import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.ksp.processing.impl.ResolverImpl
import org.jetbrains.kotlin.ksp.symbol.KSPropertyDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSPropertySetter
import org.jetbrains.kotlin.ksp.symbol.KSVariableParameter
import org.jetbrains.kotlin.ksp.symbol.KSVisitor
import org.jetbrains.kotlin.ksp.symbol.impl.KSObjectCache
import org.jetbrains.kotlin.ksp.symbol.impl.binary.KSVariableParameterDescriptorImpl

class KSPropertySetterSyntheticImpl(val ksPropertyDeclaration: KSPropertyDeclaration) :
    KSPropertyAccessorSyntheticImpl(ksPropertyDeclaration), KSPropertySetter {
    companion object : KSObjectCache<KSPropertyDeclaration, KSPropertySetterSyntheticImpl>() {
        fun getCached(ksPropertyDeclaration: KSPropertyDeclaration) =
            KSPropertySetterSyntheticImpl.cache.getOrPut(ksPropertyDeclaration) { KSPropertySetterSyntheticImpl(ksPropertyDeclaration) }
    }

    private val descriptor: PropertyAccessorDescriptor by lazy {
        ResolverImpl.instance.resolvePropertyDeclaration(ksPropertyDeclaration)!!.setter!!
    }

    override val parameter: KSVariableParameter by lazy {
        descriptor.valueParameters.singleOrNull()?.let { KSVariableParameterDescriptorImpl.getCached(it) } ?: throw IllegalStateException()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertySetter(this, data)
    }
}