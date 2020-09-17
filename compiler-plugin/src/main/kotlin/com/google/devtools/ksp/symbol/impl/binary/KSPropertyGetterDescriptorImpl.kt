/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache

class KSPropertyGetterDescriptorImpl private constructor(descriptor: PropertyGetterDescriptor) :
    KSPropertyAccessorDescriptorImpl(descriptor), KSPropertyGetter {
    companion object : KSObjectCache<PropertyGetterDescriptor, KSPropertyGetterDescriptorImpl>() {
        fun getCached(descriptor: PropertyGetterDescriptor) = cache.getOrPut(descriptor) { KSPropertyGetterDescriptorImpl(descriptor) }
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