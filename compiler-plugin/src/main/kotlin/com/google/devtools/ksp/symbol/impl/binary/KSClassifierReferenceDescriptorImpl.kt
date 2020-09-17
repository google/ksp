/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection

class KSClassifierReferenceDescriptorImpl private constructor(val descriptor: ClassifierDescriptor, val arguments: List<TypeProjection>) :
    KSClassifierReference {
    companion object : KSObjectCache<Pair<ClassifierDescriptor, List<TypeProjection>>, KSClassifierReferenceDescriptorImpl>() {
        fun getCached(kotlinType: KotlinType) = cache.getOrPut(
            Pair(
                kotlinType.constructor.declarationDescriptor!!,
                kotlinType.arguments
            )
        ) { KSClassifierReferenceDescriptorImpl(kotlinType.constructor.declarationDescriptor!!, kotlinType.arguments) }

        fun getCached(descriptor: ClassifierDescriptor, arguments: List<TypeProjection>) =
            cache.getOrPut(Pair(descriptor, arguments)) { KSClassifierReferenceDescriptorImpl(descriptor, arguments) }
    }

    private val nDeclaredArgs by lazy {
        (descriptor as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters?.size ?: 0
    }

    override val origin = Origin.CLASS

    override val location: Location = NonExistLocation

    override val qualifier: KSClassifierReference? by lazy {
        val outerDescriptor = descriptor.containingDeclaration as? ClassifierDescriptor ?: return@lazy null
        val outerArguments = arguments.drop(nDeclaredArgs)
        getCached(outerDescriptor, outerArguments)
    }

    override val typeArguments: List<KSTypeArgument> by lazy {
        arguments.map { KSTypeArgumentDescriptorImpl.getCached(it) }
    }

    override fun referencedName(): String {
        val declaredArgs = if (arguments.isEmpty() || nDeclaredArgs == 0)
            emptyList()
        else
            arguments.subList(0, nDeclaredArgs)
        return descriptor.name.asString() + if (declaredArgs.isNotEmpty()) "<${declaredArgs.map { it.toString() }
            .joinToString(", ")}>" else ""
    }

    override fun toString() = referencedName()
}