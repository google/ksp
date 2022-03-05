package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.getDocString
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.toKSModifiers
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KSPropertyDeclarationImpl(private val ktPropertySymbol: KtPropertySymbol) : KSPropertyDeclaration {
    override val getter: KSPropertyGetter? by lazy {
        ktPropertySymbol.getter?.let { KSPropertyGetterImpl(this, it) }
    }
    override val setter: KSPropertySetter? by lazy {
        ktPropertySymbol.setter?.let { KSPropertySetterImpl(this, it) }
    }

    override val extensionReceiver: KSTypeReference? by lazy {
        ktPropertySymbol.receiverType?.let { KSTypeReferenceImpl(it) }
    }
    override val type: KSTypeReference by lazy {
        KSTypeReferenceImpl(ktPropertySymbol.returnType)
    }
    override val isMutable: Boolean by lazy {
        !ktPropertySymbol.isVal
    }
    override val hasBackingField: Boolean by lazy {
        ktPropertySymbol.hasBackingField
    }

    override fun isDelegated(): Boolean {
        return ktPropertySymbol.isDelegatedProperty
    }

    override fun findOverridee(): KSPropertyDeclaration? {
        TODO("Not yet implemented")
    }

    override fun asMemberOf(containing: KSType): KSType {
        TODO("Not yet implemented")
    }

    override val simpleName: KSName by lazy {
        KSNameImpl(ktPropertySymbol.name.asString())
    }

    override val qualifiedName: KSName? by lazy {
        (ktPropertySymbol.psi as? KtProperty)?.fqName?.asString()?.let { KSNameImpl(it) }
    }
    override val typeParameters: List<KSTypeParameter> by lazy {
        ktPropertySymbol.typeParameters.map { KSTypeParameterImpl(it) }
    }
    override val packageName: KSName by lazy {
        KSNameImpl(this.containingFile?.packageName?.asString() ?: "")
    }
    override val parentDeclaration: KSDeclaration?
        get() = TODO("Not yet implemented")

    override val containingFile: KSFile? by lazy {
        (ktPropertySymbol.psi?.containingFile as? KtFile)?.let { KSFileImpl(it) }
    }
    override val docString: String? by lazy {
        ktPropertySymbol.psi?.getDocString()
    }

    override val modifiers: Set<Modifier> by lazy {
        ktPropertySymbol.psi?.safeAs<KtProperty>()?.toKSModifiers() ?: emptySet()
    }

    override val origin: Origin by lazy {
        mapAAOrigin(ktPropertySymbol.origin)
    }

    override val location: Location by lazy {
        ktPropertySymbol.psi?.toLocation() ?: NonExistLocation
    }

    override val parent: KSNode? by lazy {
        analyzeWithSymbolAsContext(ktPropertySymbol) {
            ktPropertySymbol.getContainingSymbol()?.let { KSClassDeclarationImpl(it as KtNamedClassOrObjectSymbol) }
                ?: KSFileImpl(ktPropertySymbol.psi!!.containingFile as KtFile)
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        ktPropertySymbol.annotations.asSequence().map { KSAnnotationImpl(it) }
    }

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
