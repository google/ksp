package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Origin
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol

class KSValueParameterImpl(private val ktValueParameterSymbol: KtValueParameterSymbol) : KSValueParameter {
    override val name: KSName?
        get() = TODO("Not yet implemented")
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
    override val origin: Origin
        get() = TODO("Not yet implemented")
    override val location: Location
        get() = TODO("Not yet implemented")
    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueParameter(this, data)
    }
}
