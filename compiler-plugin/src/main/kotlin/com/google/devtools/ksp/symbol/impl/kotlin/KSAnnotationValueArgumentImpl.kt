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

import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache

class KSAnnotationValueArgumentLiteImpl private constructor(override val index: Int, override val name: KSName, override val value: Any?) : KSAnnotationValueArgumentImpl() {
    companion object : KSObjectCache<Triple<Int, KSName, Any?>, KSAnnotationValueArgumentLiteImpl>() {
        fun getCached(index: Int, name: KSName, value: Any?) = cache.getOrPut(Triple(index, name, value)) { KSAnnotationValueArgumentLiteImpl(index, name, value) }
    }

    override val origin = Origin.KOTLIN

    override val location: Location = NonExistLocation

    override val annotations: List<KSAnnotation> = emptyList()

    override val isSpread: Boolean = false
}

abstract class KSAnnotationValueArgumentImpl : KSAnnotationValueArgument {
    override fun hashCode(): Int {
        return name.hashCode() * 31 + value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is KSAnnotationValueArgument)
            return false

        return other.name == this.name && other.value == this.value
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnnotationValueArgument(this, data)
    }

    override fun toString(): String {
        return "${name?.asString() ?: ""} = ${value.toString()}"
    }
}