package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedAnnotationValue

class KSValueArgumentImpl(private val namedAnnotationValue: KtNamedAnnotationValue) : KSValueArgument {
    override val name: KSName? by lazy {
        KSNameImpl(namedAnnotationValue.name.asString())
    }
    override val isSpread: Boolean = false
    override val value: Any = namedAnnotationValue.expression

    override val annotations: Sequence<KSAnnotation> = emptySequence()

    override val origin: Origin = Origin.KOTLIN

    override val location: Location by lazy {
        namedAnnotationValue.expression.sourcePsi?.toLocation() ?: NonExistLocation
    }
    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueArgument(this, data)
    }
}
