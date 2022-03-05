package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.toKSModifiers
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KSTypeParameterImpl(private val ktTypeParameterSymbol: KtTypeParameterSymbol) : KSTypeParameter {
    override val name: KSName by lazy {
        KSNameImpl(ktTypeParameterSymbol.name.asString())
    }

    override val variance: Variance by lazy {
        when (ktTypeParameterSymbol.variance) {
            org.jetbrains.kotlin.types.Variance.IN_VARIANCE -> Variance.COVARIANT
            org.jetbrains.kotlin.types.Variance.OUT_VARIANCE -> Variance.CONTRAVARIANT
            org.jetbrains.kotlin.types.Variance.INVARIANT -> Variance.INVARIANT
        }

    }

    override val isReified: Boolean = ktTypeParameterSymbol.isReified

    override val bounds: Sequence<KSTypeReference> by lazy {
        ktTypeParameterSymbol.upperBounds.asSequence().map { KSTypeReferenceImpl(it) }
    }

    override val simpleName: KSName by lazy {
        KSNameImpl(ktTypeParameterSymbol.name.asString())
    }
    override val qualifiedName: KSName? by lazy {
        KSNameImpl(ktTypeParameterSymbol.psi?.safeAs<KtTypeParameter>()?.fqName?.asString() ?: "")
    }
    override val typeParameters: List<KSTypeParameter> = emptyList()

    override val packageName: KSName by lazy {
        KSNameImpl(this.containingFile?.packageName?.asString() ?: "")
    }
    override val parentDeclaration: KSDeclaration?
        get() = TODO("Not yet implemented")

    override val containingFile: KSFile? by lazy {
        ktTypeParameterSymbol.toContainingFile()
    }

    override val docString: String? by lazy {
        ktTypeParameterSymbol.toDocString()
    }

    override val modifiers: Set<Modifier> by lazy {
        ktTypeParameterSymbol.psi?.safeAs<KtTypeParameter>()?.toKSModifiers() ?: emptySet()
    }
    override val origin: Origin by lazy {
        mapAAOrigin(ktTypeParameterSymbol.origin)
    }

    override val location: Location by lazy {
        ktTypeParameterSymbol.psi?.toLocation() ?: NonExistLocation
    }

    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeParameter(this, data)
    }

    override val annotations: Sequence<KSAnnotation>
        get() = TODO("Not yet implemented")
    override val isActual: Boolean
        get() = TODO("Not yet implemented")
    override val isExpect: Boolean
        get() = TODO("Not yet implemented")

    override fun findActuals(): Sequence<KSDeclaration> {
        TODO("Not yet implemented")
    }

    override fun findExpects(): Sequence<KSDeclaration> {
        TODO("Not yet implemented")
    }
}
