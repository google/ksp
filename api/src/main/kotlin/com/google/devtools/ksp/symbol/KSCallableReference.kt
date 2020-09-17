/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.symbol

/**
 * A reference to a callable entity, such as a function or a property.
 */
interface KSCallableReference : KSReferenceElement {
    /**
     * A reference to the type of its receiver.
     */
    val receiverType: KSTypeReference?

    /**
     * Parameters to this callable.
     */
    val functionParameters: List<KSVariableParameter>

    /**
     * A reference to its return type.
     */
    val returnType: KSTypeReference

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitCallableReference(this, data)
    }
}