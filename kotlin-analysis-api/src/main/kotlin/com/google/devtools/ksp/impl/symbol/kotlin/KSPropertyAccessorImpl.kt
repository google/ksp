package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.toKSModifiers
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

abstract class KSPropertyAccessorImpl(private val ktPropertyAccessorSymbol: KtPropertyAccessorSymbol, override val receiver: KSPropertyDeclaration) : KSPropertyAccessor {
    override val annotations: Sequence<KSAnnotation> by lazy {
        ktPropertyAccessorSymbol.annotations.asSequence().map { KSAnnotationImpl(it) }
    }

    override val location: Location by lazy {
        ktPropertyAccessorSymbol.psi?.toLocation() ?: NonExistLocation
    }

    override val modifiers: Set<Modifier> by lazy {
        ktPropertyAccessorSymbol.psi?.safeAs<KtModifierListOwner>()?.toKSModifiers() ?: emptySet()
    }

    override val origin: Origin by lazy {
        mapAAOrigin(ktPropertyAccessorSymbol.origin)
    }

    override val parent: KSNode?
        get() = TODO("Not yet implemented")
}

class KSPropertySetterImpl(owner: KSPropertyDeclaration, private val setter: KtPropertySetterSymbol
): KSPropertySetter, KSPropertyAccessorImpl(setter, owner) {
    override val parameter: KSValueParameter by lazy {
        KSValueParameterImpl(setter.parameter)
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertySetter(this, data)
    }
}

class KSPropertyGetterImpl(owner: KSPropertyDeclaration, getter: KtPropertyGetterSymbol
): KSPropertyGetter, KSPropertyAccessorImpl(getter, owner) {
    override val returnType: KSTypeReference? by lazy {
        KSTypeReferenceImpl(getter.returnType)
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyGetter(this, data)
    }
}
