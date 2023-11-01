/*
 * Copyright 2023 Google LLC
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import com.google.devtools.ksp.IdKeyPair
import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Location
import org.jetbrains.kotlin.psi.KtUserType

class KSClassifierReferenceImpl private constructor(
    val ktUserType: KtUserType,
    override val parent: KSNode
) : KSClassifierReference {
    companion object : KSObjectCache<IdKeyPair<KtUserType, KSNode?>, KSClassifierReferenceImpl>() {
        fun getCached(ktUserType: KtUserType, parent: KSNode) =
            cache.getOrPut(IdKeyPair(ktUserType, parent)) { KSClassifierReferenceImpl(ktUserType, parent) }
    }

    override val origin = parent.origin

    override val location: Location by lazy {
        ktUserType.toLocation()
    }

    override val typeArguments: List<KSTypeArgument> by lazy {
        ktUserType.typeArguments.map { KSTypeArgumentImpl.getCached(it) }
        // ktUserType.typeArguments.map { KSTypeArgumentKtImpl.getCached(it) }
    }

    override fun referencedName(): String {
        return ktUserType.referencedName ?: ""
    }

    override val qualifier: KSClassifierReference? by lazy {
        if (ktUserType.qualifier == null) {
            null
        } else {
            getCached(ktUserType.qualifier!!, parent)
        }
    }

    override fun toString(): String {
        return ktUserType.referencedName + if (typeArguments.isNotEmpty()) "<${
        typeArguments.map { it.toString() }.joinToString(", ")
        }>" else ""
    }
}
