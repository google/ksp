package com.google.devtools.ksp.symbol

interface KSDefNonNullReference : KSReferenceElement {
    /**
     * Enclosed reference element of the Definitely non null type.
     * For a reference of `T & Any`, this returns `T`.
     */
    val enclosedType: KSClassifierReference

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitDefNonNullReference(this, data)
    }
}
