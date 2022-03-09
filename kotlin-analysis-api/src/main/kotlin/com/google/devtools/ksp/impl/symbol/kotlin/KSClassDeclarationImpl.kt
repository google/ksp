package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.getDocString
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.toKSModifiers
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtObjectDeclaration

class KSClassDeclarationImpl(private val ktNamedClassOrObjectSymbol: KtNamedClassOrObjectSymbol) : KSClassDeclaration {
    override val classKind: ClassKind
        get() = TODO("Not yet implemented")
    override val primaryConstructor: KSFunctionDeclaration? by lazy {
        analyzeWithSymbolAsContext(ktNamedClassOrObjectSymbol) {
            ktNamedClassOrObjectSymbol.getMemberScope().getConstructors().singleOrNull { it.isPrimary }?.let {
                KSFunctionDeclarationImpl(it)
            }
        }
    }
    override val superTypes: Sequence<KSTypeReference> by lazy {
        analyzeWithSymbolAsContext(ktNamedClassOrObjectSymbol) {
            ktNamedClassOrObjectSymbol.superTypes.map { KSTypeReferenceImpl(it) }.asSequence()
        }
    }

    override val isCompanionObject: Boolean by lazy {
        (ktNamedClassOrObjectSymbol.psi as? KtObjectDeclaration)?.isCompanion() ?: false
    }

    override fun getSealedSubclasses(): Sequence<KSClassDeclaration> {
        TODO("Not yet implemented")
    }

    override fun getAllFunctions(): Sequence<KSFunctionDeclaration> {
        return analyzeWithSymbolAsContext(ktNamedClassOrObjectSymbol) {
            ktNamedClassOrObjectSymbol.getMemberScope().getCallableSymbols().filterIsInstance<KtFunctionLikeSymbol>()
                .map { KSFunctionDeclarationImpl(it) }
        }
    }

    override fun getAllProperties(): Sequence<KSPropertyDeclaration> {
        return analyzeWithSymbolAsContext(ktNamedClassOrObjectSymbol) {
            ktNamedClassOrObjectSymbol.getMemberScope().getAllSymbols().filterIsInstance<KtPropertySymbol>()
                .map { KSPropertyDeclarationImpl(it) }
        }
    }

    override fun asType(typeArguments: List<KSTypeArgument>): KSType {
        TODO("Not yet implemented")
    }

    override fun asStarProjectedType(): KSType {
        TODO("Not yet implemented")
    }

    override val simpleName: KSName by lazy {
        KSNameImpl(ktNamedClassOrObjectSymbol.name.asString())
    }

    override val qualifiedName: KSName? by lazy {
        (ktNamedClassOrObjectSymbol.psi as? KtClassOrObject)?.fqName?.asString()?.let { KSNameImpl(it) }
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        ktNamedClassOrObjectSymbol.typeParameters.map { KSTypeParameterImpl(it) }
    }

    override val packageName: KSName by lazy {
        this.containingFile?.packageName ?: KSNameImpl("")
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        TODO()
    }
    override val containingFile: KSFile? by lazy {
        ktNamedClassOrObjectSymbol.toContainingFile()
    }
    override val docString: String? by lazy {
        ktNamedClassOrObjectSymbol.psi?.getDocString()
    }

    override val modifiers: Set<Modifier> by lazy {
        (ktNamedClassOrObjectSymbol.psi as? KtClassOrObject)?.toKSModifiers() ?: emptySet()
    }
    override val origin: Origin by lazy {
        mapAAOrigin(ktNamedClassOrObjectSymbol.origin)
    }

    override val location: Location by lazy {
        ktNamedClassOrObjectSymbol.psi?.toLocation() ?: NonExistLocation
    }
    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitClassDeclaration(this, data)
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        analyzeWithSymbolAsContext(ktNamedClassOrObjectSymbol) {
            ktNamedClassOrObjectSymbol.annotations.map { KSAnnotationImpl(it) }.asSequence()
        }
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

    override val declarations: Sequence<KSDeclaration> by lazy {
        analyzeWithSymbolAsContext(ktNamedClassOrObjectSymbol) {
            ktNamedClassOrObjectSymbol.getDeclaredMemberScope().getAllSymbols().map {
                when (it) {
                    is KtNamedClassOrObjectSymbol -> KSClassDeclarationImpl(it)
                    is KtFunctionSymbol -> KSFunctionDeclarationImpl(it)
                    is KtPropertySymbol -> KSPropertyDeclarationImpl(it)
                    else -> throw IllegalStateException()
                }
            }
        }
    }
}
