package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.common.lazyMemoizedSequence
import com.google.devtools.ksp.common.toKSModifiers
import com.google.devtools.ksp.impl.ResolverAAImpl
import com.google.devtools.ksp.impl.symbol.java.KSAnnotationJavaImpl
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.symbol.*
import com.intellij.psi.PsiField
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.name.Name

sealed class KSPropertyDeclarationJavaImpl : KSPropertyDeclaration, AbstractKSDeclarationImpl() {
    companion object {
        // Factory for PSI Fast Path
        fun getCached(psi: PsiField, parent: KSClassDeclarationImpl): KSPropertyDeclarationJavaImpl =
            KSPropertyDeclarationJavaPsiImpl.getCached(psi, parent)

        // Factory for AA Slow Path
        fun getCached(symbol: KaJavaFieldSymbol): KSPropertyDeclarationJavaImpl =
            KSPropertyDeclarationJavaAAImpl.getCached(symbol)
    }

    abstract val ktJavaFieldSymbol: KaJavaFieldSymbol

    override val ktDeclarationSymbol: KaDeclarationSymbol
        get() = ktJavaFieldSymbol

    // Manual delegation for KSExpectActual to avoid eager evaluation in the class header
    private val expectActualImpl by lazy { KSExpectActualImpl(ktJavaFieldSymbol) }
    override val isActual: Boolean get() = expectActualImpl.isActual
    override val isExpect: Boolean get() = expectActualImpl.isExpect
    override fun findActuals(): Sequence<KSDeclaration> = expectActualImpl.findActuals()
    override fun findExpects(): Sequence<KSDeclaration> = expectActualImpl.findExpects()

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
        get() = !ktJavaFieldSymbol.isVal

    override val hasBackingField: Boolean
        get() = true

    override fun isDelegated(): Boolean {
        return false
    }

    override fun findOverridee(): KSPropertyDeclaration? {
        return null
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
        get() = KSNameImpl.getCached(ktJavaFieldSymbol.callableId?.packageName?.asString() ?: "")

    override val origin: Origin
        get() = mapAAOrigin(ktJavaFieldSymbol)

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }

    override fun defer(): Restorable? {
        return ktJavaFieldSymbol.defer(::getCached)
    }
}

private class KSPropertyDeclarationJavaAAImpl(
    override val ktJavaFieldSymbol: KaJavaFieldSymbol
) : KSPropertyDeclarationJavaImpl() {
    companion object : KSObjectCache<KaJavaFieldSymbol, KSPropertyDeclarationJavaAAImpl>() {
        fun getCached(symbol: KaJavaFieldSymbol) = cache.getOrPut(symbol) {
            KSPropertyDeclarationJavaAAImpl(symbol)
        }
    }
}

class KSPropertyDeclarationJavaPsiImpl private constructor(
    val psiField: PsiField,
    override val parent: KSClassDeclarationImpl,
) : KSPropertyDeclarationJavaImpl() {
    companion object : KSObjectCache<PsiField, KSPropertyDeclarationJavaPsiImpl>() {
        fun getCached(psiField: PsiField, parent: KSClassDeclarationImpl) = cache.getOrPut(psiField) {
            KSPropertyDeclarationJavaPsiImpl(psiField, parent)
        }
    }

    override val ktJavaFieldSymbol: KaJavaFieldSymbol by lazy {
        analyze {
            val targetName = Name.identifier(psiField.name)
            // Note: Technically, declaredMemberScope and staticDeclaredMemberScope could be used here since the psi
            // field is declared in the parent. However, those scopes trigger the performance issue mentioned in
            // https://youtrack.jetbrains.com/issue/KT-85692, so stick with memberScope/staticMemberScope instead.
            val instanceSymbols = parent.ktClassOrObjectSymbol.memberScope.callables(targetName)
            val staticSymbols = parent.ktClassOrObjectSymbol.staticMemberScope.callables(targetName)
            requireNotNull(
                (instanceSymbols + staticSymbols)
                    .find { it.psi == psiField || it.psi?.isEquivalentTo(psiField) == true } as? KaJavaFieldSymbol
            ) { "Failed to resolve KaJavaFieldSymbol for field ${psiField.name}" }
        }
    }

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(psiField.name)
    }

    override val origin: Origin
        get() = parentDeclaration?.origin ?: Origin.JAVA

    override val containingFile: KSFile?
        get() = parentDeclaration?.containingFile

    override val packageName: KSName
        get() = parentDeclaration?.packageName ?: KSNameImpl.getCached("")

    override val isMutable: Boolean
        get() = !psiField.hasModifierProperty(PsiModifier.FINAL)

    override val modifiers: Set<Modifier> by lazy {
        if (origin == Origin.JAVA) {
            return@lazy psiField.toKSModifiers()
        }

        val psiModifiers = psiField.toKSModifiers().toMutableSet()

        // Emulate AA: Strip JVM-specific modifiers from compiled Java fields
        psiModifiers.remove(Modifier.JAVA_TRANSIENT)
        psiModifiers.remove(Modifier.JAVA_VOLATILE)
        psiModifiers.remove(Modifier.JAVA_SYNCHRONIZED)

        // Emulate AA: Java fields are treated as final 'val' properties
        psiModifiers.add(Modifier.FINAL)

        return@lazy psiModifiers
    }

    override val annotations: Sequence<KSAnnotation> by lazyMemoizedSequence {
        psiField.annotations.asSequence().map { KSAnnotationJavaImpl.getCached(it, this) }
    }
}

internal fun KaJavaFieldSymbol.toModifiers(): Set<Modifier> {
    val result = mutableSetOf<Modifier>()
    if (visibility != KaSymbolVisibility.PACKAGE_PRIVATE) {
        result.add(visibility.toModifier())
    }
    if (isStatic) {
        result.add(Modifier.JAVA_STATIC)
        result.add(Modifier.FINAL)
    }
    // Analysis API returns open for static members which should be ignored.
    if (!isStatic || modality != KaSymbolModality.OPEN) {
        result.add(modality.toModifier())
    }
    return result
}
