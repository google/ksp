/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.binary

import org.jetbrains.kotlin.ksp.symbol.KSTypeArgument
import org.jetbrains.kotlin.ksp.symbol.KSClassifierReference
import org.jetbrains.kotlin.types.KotlinType

class KSClassifierReferenceDescriptorImpl(val kotlinType: KotlinType) : KSClassifierReference {
    companion object {
        private val cache = mutableMapOf<KotlinType, KSClassifierReferenceDescriptorImpl>()

        fun getCached(kotlinType: KotlinType) = cache.getOrPut(kotlinType) { KSClassifierReferenceDescriptorImpl(kotlinType) }
    }

    override val qualifier: KSClassifierReference?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val typeArguments: List<KSTypeArgument> by lazy {
        kotlinType.arguments.map { KSTypeArgumentDescriptorImpl.getCached(it) }
    }

    override fun referencedName(): String {
        return kotlinType.constructor.declarationDescriptor?.name?.asString() ?: ""
    }
}