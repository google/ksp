package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.toKSModifiers
import org.jetbrains.kotlin.analysis.api.symbols.KtJavaFieldSymbol
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KSPropertyDeclarationJavaImpl private constructor(
    private val ktJavaFieldSymbol: KtJavaFieldSymbol
) : KSPropertyDeclaration {
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
        KSTypeReferenceImpl.getCached(ktJavaFieldSymbol.returnType)
    }

    override val isMutable: Boolean
        get() = true

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

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(ktJavaFieldSymbol.name.asString())
    }

    override val qualifiedName: KSName by lazy {
        KSNameImpl.getCached(ktJavaFieldSymbol.callableIdIfNonLocal?.asSingleFqName()?.asString() ?: "")
    }

    override val typeParameters: List<KSTypeParameter>
        get() = emptyList()

    override val packageName: KSName
        get() = KSNameImpl.getCached(ktJavaFieldSymbol.callableIdIfNonLocal?.packageName?.asString() ?: "")

    override val parentDeclaration: KSDeclaration? by lazy {
        ktJavaFieldSymbol.getContainingKSSymbol()
    }

    override val containingFile: KSFile? by lazy {
        ktJavaFieldSymbol.toContainingFile()
    }

    override val docString: String?
        get() = TODO("Not yet implemented")

    override val modifiers: Set<Modifier>
        get() = ktJavaFieldSymbol.psi.safeAs<KtModifierListOwner>()?.toKSModifiers() ?: emptySet()

    override val origin: Origin
        get() = Origin.JAVA

    override val location: Location
        get() = ktJavaFieldSymbol.psi.toLocation()

    override val parent: KSNode?
        get() = parentDeclaration

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }

    // FIXME: fix in upstream.
    override val annotations: Sequence<KSAnnotation>
        get() = emptySequence()

    override val isActual: Boolean
        get() = false

    override val isExpect: Boolean
        get() = false

    override fun findActuals(): Sequence<KSDeclaration> {
        return emptySequence()
    }

    override fun findExpects(): Sequence<KSDeclaration> {
        return emptySequence()
    }
}
