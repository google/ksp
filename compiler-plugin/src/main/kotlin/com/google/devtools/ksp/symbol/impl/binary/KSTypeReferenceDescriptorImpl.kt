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

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSReferenceElement
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.NonExistLocation
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.kotlin.IdKeyTriple
import com.google.devtools.ksp.symbol.impl.kotlin.getKSTypeCached
import com.google.devtools.ksp.symbol.impl.memoized
import org.jetbrains.kotlin.builtins.isSuspendFunctionTypeOrSubtype
import org.jetbrains.kotlin.types.KotlinType

class KSTypeReferenceDescriptorImpl private constructor(
    val kotlinType: KotlinType,
    override val origin: Origin,
    override val parent: KSNode?
) : KSTypeReference {
    companion object : KSObjectCache<IdKeyTriple<KotlinType, Origin, KSNode?>, KSTypeReferenceDescriptorImpl>() {
        fun getCached(kotlinType: KotlinType, origin: Origin, parent: KSNode?) = cache
            .getOrPut(IdKeyTriple(kotlinType, origin, parent)) {
                KSTypeReferenceDescriptorImpl(kotlinType, origin, parent)
            }
    }

    override val location: Location = NonExistLocation

    override val element: KSReferenceElement by lazy {
        KSClassifierReferenceDescriptorImpl.getCached(kotlinType, origin, this)
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        kotlinType.annotations.asSequence().map { KSAnnotationDescriptorImpl.getCached(it, this) }.memoized()
    }

    override val modifiers: Set<Modifier> by lazy {
        if (kotlinType.isSuspendFunctionTypeOrSubtype) {
            setOf(Modifier.SUSPEND)
        } else {
            emptySet<Modifier>()
        }
    }

    override fun resolve(): KSType {
        return getKSTypeCached(kotlinType)
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }

    override fun toString(): String {
        return element.toString()
    }
}
