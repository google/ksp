package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.impl.ResolverAAImpl
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.processing.impl.KSNameImpl
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.symbols.KtJavaFieldSymbol
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities

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
        KSTypeReferenceResolvedImpl.getCached(ktJavaFieldSymbol.returnType, this@KSPropertyDeclarationJavaImpl)
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
        return ResolverAAImpl.instance.computeAsMemberOf(this, containing)
    }

    override val typeParameters: List<KSTypeParameter>
        get() = emptyList()

    override val qualifiedName: KSName? by lazy {
        KSNameImpl.getCached("${this.parentDeclaration!!.qualifiedName!!.asString()}.${simpleName.asString()}")
    }

    override val packageName: KSName
        get() = KSNameImpl.getCached(ktJavaFieldSymbol.callableIdIfNonLocal?.packageName?.asString() ?: "")

    override val origin: Origin
        get() = mapAAOrigin(ktJavaFieldSymbol)

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }

    override fun defer(): Restorable? {
        TODO("Not yet implemented")
    }
}

internal fun KtJavaFieldSymbol.toModifiers(): Set<Modifier> {
    val result = mutableSetOf<Modifier>()
    if (visibility != JavaVisibilities.PackageVisibility) {
        result.add(visibility.toModifier())
    }
    if (isStatic) {
        result.add(Modifier.JAVA_STATIC)
        result.add(Modifier.FINAL)
    }
    // Analysis API returns open for static members which should be ignored.
    if (!isStatic || modality != Modality.OPEN) {
        result.add(modality.toModifier())
    }
    return result
}
