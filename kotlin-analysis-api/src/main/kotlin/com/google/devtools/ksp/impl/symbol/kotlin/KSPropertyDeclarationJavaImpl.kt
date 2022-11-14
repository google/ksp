@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtJavaFieldSymbol
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.java.resolveIfJavaType
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

class KSPropertyDeclarationJavaImpl private constructor(private val ktJavaFieldSymbol: KtJavaFieldSymbol) :
    KSPropertyDeclaration,
    AbstractKSDeclarationImpl(ktJavaFieldSymbol),
    KSExpectActual by KSExpectActualImpl(ktJavaFieldSymbol) {
    companion object : KSObjectCache<KtJavaFieldSymbol, KSPropertyDeclaration>() {
        fun getCached(ktJavaFieldSymbol: KtJavaFieldSymbol): KSPropertyDeclaration =
            cache.getOrPut(ktJavaFieldSymbol) { KSPropertyDeclarationJavaImpl(ktJavaFieldSymbol) }
    }
    override val getter: KSPropertyGetter?
        get() = null

    override val setter: KSPropertySetter?
        get() = null

    override val extensionReceiver: KSTypeReference?
        get() = null

    @OptIn(SymbolInternals::class)
    override val type: KSTypeReference by lazy {
        // FIXME: temporary workaround before upstream fixes java type refs.
        ((ktJavaFieldSymbol as KtFirJavaFieldSymbol).firSymbol.fir as FirJavaField).also {
            it.returnTypeRef = it.returnTypeRef.resolveIfJavaType(
                it.moduleData.session,
                (it.getContainingClass(it.moduleData.session) as FirJavaClass).javaTypeParameterStack
            )
        }
        KSTypeReferenceImpl.getCached(ktJavaFieldSymbol.returnType, this@KSPropertyDeclarationJavaImpl)
    }

    override val isMutable: Boolean
        get() = !modifiers.contains(Modifier.FINAL)

    override val hasBackingField: Boolean
        get() = true

    override fun isDelegated(): Boolean {
        return false
    }

    override fun findOverridee(): KSPropertyDeclaration? {
        TODO("Not yet implemented")
    }

    override fun asMemberOf(containing: KSType): KSType {
        TODO("Not yet implemented")
    }

    override val typeParameters: List<KSTypeParameter>
        get() = emptyList()

    override val qualifiedName: KSName? by lazy {
        KSNameImpl.getCached("${this.parentDeclaration!!.qualifiedName!!.asString()}.${simpleName.asString()}")
    }

    override val packageName: KSName
        get() = KSNameImpl.getCached(ktJavaFieldSymbol.callableIdIfNonLocal?.packageName?.asString() ?: "")

    override val origin: Origin
        get() = Origin.JAVA

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }
}
