/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import com.google.devtools.ksp.processing.impl.KSObjectCache
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection

class KSClassifierReferenceDescriptorImpl private constructor(
    val descriptor: ClassifierDescriptor,
    val arguments: List<TypeProjection>,
    override val origin: Origin,
    override val parent: KSNode?
) :
    KSClassifierReference {
    companion object : KSObjectCache<Triple<ClassifierDescriptor, List<TypeProjection>, Pair<KSNode?, Origin>>,
        KSClassifierReferenceDescriptorImpl>() {
        fun getCached(kotlinType: KotlinType, origin: Origin, parent: KSNode?) = cache.getOrPut(
            Triple(
                kotlinType.constructor.declarationDescriptor!!,
                kotlinType.arguments,
                Pair(parent, origin)
            )
        ) {
            KSClassifierReferenceDescriptorImpl(
                kotlinType.constructor.declarationDescriptor!!, kotlinType.arguments, origin, parent
            )
        }

        fun getCached(
            descriptor: ClassifierDescriptor,
            arguments: List<TypeProjection>,
            origin: Origin,
            parent: KSNode?
        ) = cache.getOrPut(
            Triple(descriptor, arguments, Pair(parent, origin))
        ) { KSClassifierReferenceDescriptorImpl(descriptor, arguments, origin, parent) }
    }

    private val nDeclaredArgs by lazy {
        (descriptor as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters?.size ?: 0
    }

    override val location: Location = NonExistLocation

    override val qualifier: KSClassifierReference? by lazy {
        val outerDescriptor = descriptor.containingDeclaration as? ClassifierDescriptor ?: return@lazy null
        val outerArguments = arguments.drop(nDeclaredArgs)
        getCached(outerDescriptor, outerArguments, origin, parent)
    }

    override val typeArguments: List<KSTypeArgument> by lazy {
        arguments.map { KSTypeArgumentDescriptorImpl.getCached(it, origin, this.parent) }
    }

    override fun referencedName(): String {
        val declaredArgs = if (arguments.isEmpty() || nDeclaredArgs == 0)
            emptyList()
        else
            arguments.subList(0, nDeclaredArgs)
        return descriptor.name.asString() + if (declaredArgs.isNotEmpty()) "<${
        declaredArgs.map { it.toString() }
            .joinToString(", ")
        }>" else ""
    }

    override fun toString() = referencedName()
}
