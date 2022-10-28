package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.symbols.KtJavaFieldSymbol

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

    override val type: KSTypeReference by lazy {
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
