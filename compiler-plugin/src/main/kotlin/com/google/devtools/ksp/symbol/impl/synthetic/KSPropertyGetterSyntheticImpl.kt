/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.symbol.impl.synthetic

import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.binary.KSTypeReferenceDescriptorImpl

class KSPropertyGetterSyntheticImpl(val ksPropertyDeclaration: KSPropertyDeclaration) :
    KSPropertyAccessorSyntheticImpl(ksPropertyDeclaration), KSPropertyGetter {
    companion object : KSObjectCache<KSPropertyDeclaration, KSPropertyGetterSyntheticImpl>() {
        fun getCached(ksPropertyDeclaration: KSPropertyDeclaration) =
            KSPropertyGetterSyntheticImpl.cache.getOrPut(ksPropertyDeclaration) { KSPropertyGetterSyntheticImpl(ksPropertyDeclaration) }
    }

    private val descriptor: PropertyAccessorDescriptor by lazy {
        ResolverImpl.instance.resolvePropertyDeclaration(ksPropertyDeclaration)!!.getter!!
    }

    override val returnType: KSTypeReference? by lazy {
        if (descriptor.returnType != null) {
            KSTypeReferenceDescriptorImpl.getCached(descriptor.returnType!!)
        } else {
            null
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyGetter(this, data)
    }

    override fun toString(): String {
        return "$receiver.getter()"
    }
}