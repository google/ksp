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

object KSErrorValueParameter : KSValueParameter {
    override val name: KSName? = null
    override val type: KSTypeReference? = null
    override val isVararg: Boolean = false
    override val isNoInline: Boolean = false
    override val isCrossInline: Boolean = false
    override val isVal: Boolean = false
    override val isVar: Boolean = false
    override val hasDefault: Boolean = false
    override val annotations: List<KSAnnotation> get() = emptyList()
    override val origin: Origin = Origin.SYNTHETIC
    override val location: Location = NonExistLocation

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueParameter(this, data)
    }
}