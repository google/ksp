/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol

/**
 * An application / reference to a user declared type such as class, interface and object.
 */
interface KSClassifierReference : KSReferenceElement {
    /**
     * The outer class of an inner class.
     */
    val qualifier: KSClassifierReference?

    /**
     * The text which appears in the reference. For example, it is "Int" in `val temperature: Int` or
     * "kotlin.Any" in `val canBeAnything: kotlin.Any`
     */
    fun referencedName(): String

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitClassifierReference(this, data)
    }
}