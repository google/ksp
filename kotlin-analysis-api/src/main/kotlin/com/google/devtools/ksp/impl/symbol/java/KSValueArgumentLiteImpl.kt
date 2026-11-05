package com.google.devtools.ksp.impl.symbol.java

import com.google.devtools.ksp.impl.symbol.kotlin.AbstractKSValueArgumentImpl
import com.google.devtools.ksp.symbol.*

class KSValueArgumentLiteImpl(
    override val name: KSName?,
    valueProvider: (KSValueArgumentLiteImpl) -> Any?,
    override val parent: KSNode,
    override val origin: Origin,
    override val location: Location
) : AbstractKSValueArgumentImpl() {
    override val value: Any? by lazy { valueProvider(this) }

    override val isSpread: Boolean = false

    override val annotations: Sequence<KSAnnotation> = emptySequence()
}
