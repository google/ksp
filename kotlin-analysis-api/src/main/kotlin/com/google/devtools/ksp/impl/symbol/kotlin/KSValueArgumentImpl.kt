package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Origin
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedAnnotationValue

class KSValueArgumentImpl(private val namedAnnotationValue: KtNamedAnnotationValue) : KSValueArgument {
    override val name: KSName? by lazy {
        KSNameImpl(namedAnnotationValue.name.asString())
    }
    override val isSpread: Boolean = false
    override val value: Any = namedAnnotationValue.expression

    override val annotations: Sequence<KSAnnotation> = emptySequence()

    override val origin: Origin = Origin.KOTLIN

    override val location: Location
        get() = TODO("Not yet implemented")
    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueArgument(this, data)
    }
}
