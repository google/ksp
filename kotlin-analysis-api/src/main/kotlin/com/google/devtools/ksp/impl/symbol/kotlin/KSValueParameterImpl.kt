package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol

class KSValueParameterImpl(private val ktValueParameterSymbol: KtValueParameterSymbol) : KSValueParameter {
    override val name: KSName? by lazy {
        KSNameImpl(ktValueParameterSymbol.name.asString())
    }

    override val type: KSTypeReference by lazy {
        KSTypeReferenceImpl(ktValueParameterSymbol.returnType)
    }

    override val isVararg: Boolean by lazy {
        ktValueParameterSymbol.isVararg
    }
    override val isNoInline: Boolean
        get() = TODO("Not yet implemented")
    override val isCrossInline: Boolean
        get() = TODO("Not yet implemented")
    override val isVal: Boolean
        get() = TODO("Not yet implemented")
    override val isVar: Boolean
        get() = TODO("Not yet implemented")
    override val hasDefault: Boolean by lazy {
        ktValueParameterSymbol.hasDefaultValue
    }
    override val annotations: Sequence<KSAnnotation> by lazy {
        ktValueParameterSymbol.annotations.asSequence().map { KSAnnotationImpl(it) }
    }
    override val origin: Origin by lazy {
        mapAAOrigin(ktValueParameterSymbol.origin)
    }

    override val location: Location by lazy {
        ktValueParameterSymbol.psi?.toLocation() ?: NonExistLocation
    }
    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueParameter(this, data)
    }
}
