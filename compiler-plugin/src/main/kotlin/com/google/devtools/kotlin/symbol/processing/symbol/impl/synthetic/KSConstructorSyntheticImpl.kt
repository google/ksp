/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.synthetic

import com.google.devtools.kotlin.symbol.processing.symbol.*
import com.google.devtools.kotlin.symbol.processing.symbol.impl.KSObjectCache
import com.google.devtools.kotlin.symbol.processing.symbol.impl.kotlin.KSNameImpl

class KSConstructorSyntheticImpl(val ksClassDeclaration: KSClassDeclaration) : KSFunctionDeclaration, KSDeclaration by ksClassDeclaration {
    companion object : KSObjectCache<KSClassDeclaration, KSConstructorSyntheticImpl>() {
        fun getCached(ksClassDeclaration: KSClassDeclaration) =
            KSConstructorSyntheticImpl.cache.getOrPut(ksClassDeclaration) { KSConstructorSyntheticImpl(ksClassDeclaration) }
    }

    override val isAbstract: Boolean = false

    override val extensionReceiver: KSTypeReference? = null

    override val parameters: List<KSVariableParameter> = emptyList()

    override val functionKind: FunctionKind = FunctionKind.MEMBER

    override val qualifiedName: KSName? by lazy {
        KSNameImpl.getCached(ksClassDeclaration.qualifiedName?.asString()?.plus(".<init>") ?: "")
    }

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached("<init>")
    }

    override val typeParameters: List<KSTypeParameter> = emptyList()

    override val containingFile: KSFile? by lazy {
        ksClassDeclaration.containingFile
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        ksClassDeclaration
    }

    override val returnType: KSTypeReference? = null

    override val annotations: List<KSAnnotation> = emptyList()

    override val isActual: Boolean = false

    override val isExpect: Boolean = false

    override val declarations: List<KSDeclaration> = emptyList()

    override val location: Location by lazy {
        ksClassDeclaration.location
    }

    override val modifiers: Set<Modifier> = emptySet()

    override val origin: Origin = Origin.SYNTHETIC

    override fun overrides(overridee: KSFunctionDeclaration): Boolean = false

    override fun findOverridee(): KSFunctionDeclaration? = null

    override fun findActuals(): List<KSDeclaration> {
        return emptyList()
    }

    override fun findExpects(): List<KSDeclaration> {
        return emptyList()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitFunctionDeclaration(this, data)
    }

    override fun toString(): String {
        return "synthetic constructor for ${this.parentDeclaration}"
    }
}
