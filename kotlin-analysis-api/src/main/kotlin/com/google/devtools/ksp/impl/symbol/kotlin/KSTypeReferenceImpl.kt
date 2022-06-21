/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSReferenceElement
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import org.jetbrains.kotlin.analysis.api.types.KtType

class KSTypeReferenceImpl(private val ktType: KtType) : KSTypeReference {
    companion object : KSObjectCache<KtType, KSTypeReference>() {
        fun getCached(type: KtType): KSTypeReference = cache.getOrPut(type) { KSTypeReferenceImpl(type) }
    }
    // FIXME: return correct reference element.
    override val element: KSReferenceElement? = null

    override fun resolve(): KSType {
        return KSTypeImpl.getCached(ktType)
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        ktType.annotations()
    }

    override val origin: Origin = Origin.KOTLIN

    override val location: Location
        get() = TODO("Not yet implemented")

    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }

    override val modifiers: Set<Modifier>
        get() = TODO("Not yet implemented")
}
