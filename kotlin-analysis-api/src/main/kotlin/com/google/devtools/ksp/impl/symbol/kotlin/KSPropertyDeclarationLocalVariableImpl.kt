package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.processing.impl.KSNameImpl
import com.google.devtools.ksp.symbol.KSExpectActual
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import org.jetbrains.kotlin.analysis.api.symbols.KtLocalVariableSymbol

class KSPropertyDeclarationLocalVariableImpl private constructor(
    private val ktLocalVariableSymbol: KtLocalVariableSymbol
) : KSPropertyDeclaration,
    AbstractKSDeclarationImpl(ktLocalVariableSymbol),
    KSExpectActual by KSExpectActualImpl(ktLocalVariableSymbol) {
    companion object : KSObjectCache<KtLocalVariableSymbol, KSPropertyDeclarationLocalVariableImpl>() {
        fun getCached(ktLocalVariableSymbol: KtLocalVariableSymbol) =
            cache.getOrPut(ktLocalVariableSymbol) { KSPropertyDeclarationLocalVariableImpl(ktLocalVariableSymbol) }
    }

    override val getter: KSPropertyGetter? = null

    override val setter: KSPropertySetter? = null

    override val extensionReceiver: KSTypeReference? = null

    override val type: KSTypeReference by lazy {
        KSTypeReferenceImpl.getCached(ktLocalVariableSymbol.returnType)
    }

    override val isMutable: Boolean = !ktLocalVariableSymbol.isVal

    override val hasBackingField: Boolean = false

    override fun isDelegated(): Boolean = false

    override fun findOverridee() = null

    override fun asMemberOf(containing: KSType): KSType {
        TODO("Not yet implemented")
    }

    override val qualifiedName: KSName? by lazy {
        KSNameImpl.getCached("${parentDeclaration?.qualifiedName?.asString()}.${this.simpleName.asString()}")
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }
}
