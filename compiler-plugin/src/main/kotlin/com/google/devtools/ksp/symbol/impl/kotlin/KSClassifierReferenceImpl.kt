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

package com.google.devtools.ksp.symbol.impl.kotlin

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.findParentOfType
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.psi.*

class KSClassifierReferenceImpl private constructor(val ktUserType: KtUserType) : KSClassifierReference {
    companion object : KSObjectCache<KtUserType, KSClassifierReferenceImpl>() {
        fun getCached(ktUserType: KtUserType) = cache.getOrPut(ktUserType) { KSClassifierReferenceImpl(ktUserType) }
    }

    override val origin = Origin.KOTLIN

    override val location: Location by lazy {
        ktUserType.toLocation()
    }

    override val parent: KSNode? by lazy {
        ktUserType.findParentOfType<KtTypeReference>()?.let { KSTypeReferenceImpl.getCached(it) }
    }

    override val typeArguments: List<KSTypeArgument> by lazy {
        ktUserType.typeArguments.map { KSTypeArgumentKtImpl.getCached(it) }
    }

    override fun referencedName(): String {
        return ktUserType.referencedName ?: ""
    }

    override val qualifier: KSClassifierReference? by lazy {
        if (ktUserType.qualifier == null) {
            null
        } else {
            KSClassifierReferenceImpl.getCached(ktUserType.qualifier!!)
        }
    }

    override fun toString() = referencedName()
}
