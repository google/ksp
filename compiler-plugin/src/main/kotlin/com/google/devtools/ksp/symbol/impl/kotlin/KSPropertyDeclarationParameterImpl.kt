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

import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.*
import com.google.devtools.ksp.symbol.impl.synthetic.KSPropertyGetterSyntheticImpl
import com.google.devtools.ksp.symbol.impl.synthetic.KSPropertySetterSyntheticImpl
import org.jetbrains.kotlin.psi.KtParameter

class KSPropertyDeclarationParameterImpl private constructor(val ktParameter: KtParameter) : KSPropertyDeclaration,
    KSDeclarationImpl(ktParameter),
    KSExpectActual by KSExpectActualImpl(ktParameter) {
    companion object : KSObjectCache<KtParameter, KSPropertyDeclarationParameterImpl>() {
        fun getCached(ktParameter: KtParameter) = cache.getOrPut(ktParameter) { KSPropertyDeclarationParameterImpl(ktParameter) }
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        ktParameter.filterUseSiteTargetAnnotations().map { KSAnnotationImpl.getCached(it) }
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        ktParameter.findParentDeclaration()!!.parentDeclaration
    }

    override val hasBackingField: Boolean
        get() = true

    override val extensionReceiver: KSTypeReference? = null

    override val isMutable: Boolean by lazy {
        ktParameter.isMutable
    }

    override val getter: KSPropertyGetter? by lazy {
        if (this.isPrivate()) {
            null
        } else {
            KSPropertyGetterSyntheticImpl.getCached(this)
        }
    }

    override val setter: KSPropertySetter? by lazy {
        if (ktParameter.isMutable && !this.isPrivate()) {
            KSPropertySetterSyntheticImpl.getCached(this)
        } else {
            null
        }
    }

    override val type: KSTypeReference by lazy {
        if (ktParameter.typeReference != null) {
            KSTypeReferenceImpl.getCached(ktParameter.typeReference!!)
        } else {
            throw IllegalStateException("properties in parameter must have explicit type")
        }
    }

    override fun isDelegated(): Boolean = false

    override fun findOverridee(): KSPropertyDeclaration? {
        return ResolverImpl.instance.resolvePropertyDeclaration(this)?.original?.overriddenDescriptors?.singleOrNull { it.overriddenDescriptors.isEmpty() }
            ?.toKSPropertyDeclaration()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }

    override fun asMemberOf(containing: KSType): KSType =
        ResolverImpl.instance.asMemberOf(this, containing)
}
