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
import com.google.devtools.ksp.symbol.impl.kotlin.KSTypeArgumentImpl
import org.jetbrains.kotlin.types.TypeProjection

class KSTypeArgumentDescriptorImpl private constructor(val descriptor: TypeProjection, override val origin: Origin) : KSTypeArgumentImpl() {
    companion object : KSObjectCache<IdKey<Pair<TypeProjection, Origin>>, KSTypeArgumentDescriptorImpl>() {
        fun getCached(descriptor: TypeProjection, origin: Origin) = cache.getOrPut(IdKey(Pair(descriptor, origin))) { KSTypeArgumentDescriptorImpl(descriptor, origin) }
    }

    override val location: Location = NonExistLocation

    override val type: KSTypeReference by lazy {
        KSTypeReferenceDescriptorImpl.getCached(descriptor.type, origin)
    }

    override val variance: Variance by lazy {
        if (descriptor.isStarProjection)
            Variance.STAR
        else {
            when (descriptor.projectionKind) {
                org.jetbrains.kotlin.types.Variance.IN_VARIANCE -> Variance.CONTRAVARIANT
                org.jetbrains.kotlin.types.Variance.OUT_VARIANCE -> Variance.COVARIANT
                else -> Variance.INVARIANT
            }
        }
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        descriptor.type.annotations.asSequence().map { KSAnnotationDescriptorImpl.getCached(it) }
    }
}
