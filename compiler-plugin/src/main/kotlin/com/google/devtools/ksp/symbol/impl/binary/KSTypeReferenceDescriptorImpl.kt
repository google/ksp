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

import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.kotlin.IdKey
import com.google.devtools.ksp.symbol.impl.kotlin.getKSTypeCached
import com.google.devtools.ksp.symbol.impl.memoized
import org.jetbrains.kotlin.builtins.isSuspendFunctionTypeOrSubtype
import org.jetbrains.kotlin.types.KotlinType

class KSTypeReferenceDescriptorImpl private constructor(val kotlinType: KotlinType, override val origin: Origin) :
    KSTypeReference {
    companion object : KSObjectCache<IdKey<Pair<KotlinType, Origin>>, KSTypeReferenceDescriptorImpl>() {
        fun getCached(kotlinType: KotlinType, origin: Origin) =
            cache.getOrPut(IdKey(Pair(kotlinType, origin))) { KSTypeReferenceDescriptorImpl(kotlinType, origin) }
    }

    override val location: Location = NonExistLocation

    override val element: KSReferenceElement by lazy {
        KSClassifierReferenceDescriptorImpl.getCached(kotlinType, origin)
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        kotlinType.annotations.asSequence().map { KSAnnotationDescriptorImpl.getCached(it) }.memoized()
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
