/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.synthetic

import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import com.google.devtools.kotlin.symbol.processing.processing.impl.ResolverImpl
import com.google.devtools.kotlin.symbol.processing.symbol.KSPropertyDeclaration
import com.google.devtools.kotlin.symbol.processing.symbol.KSPropertySetter
import com.google.devtools.kotlin.symbol.processing.symbol.KSVariableParameter
import com.google.devtools.kotlin.symbol.processing.symbol.KSVisitor
import com.google.devtools.kotlin.symbol.processing.symbol.impl.KSObjectCache
import com.google.devtools.kotlin.symbol.processing.symbol.impl.binary.KSVariableParameterDescriptorImpl

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

    override fun toString(): String {
        return "$receiver.getter()"
    }
}