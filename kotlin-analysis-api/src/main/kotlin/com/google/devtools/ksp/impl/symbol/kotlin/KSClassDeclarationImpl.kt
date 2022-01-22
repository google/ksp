package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol

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

    override val isCompanionObject: Boolean
        get() = TODO("Not yet implemented")

    override fun getSealedSubclasses(): Sequence<KSClassDeclaration> {
        TODO("Not yet implemented")
    }

    override fun getAllFunctions(): Sequence<KSFunctionDeclaration> {
        TODO("Not yet implemented")
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

    override val simpleName: KSName
        get() = TODO("Not yet implemented")
    override val qualifiedName: KSName?
        get() = TODO("Not yet implemented")
    override val typeParameters: List<KSTypeParameter>
        get() = TODO("Not yet implemented")
    override val packageName: KSName
        get() = TODO("Not yet implemented")
    override val parentDeclaration: KSDeclaration?
        get() = TODO("Not yet implemented")
    override val containingFile: KSFile?
        get() = TODO("Not yet implemented")
    override val docString: String?
        get() = TODO("Not yet implemented")
    override val modifiers: Set<Modifier>
        get() = TODO("Not yet implemented")
    override val origin: Origin
        get() = TODO("Not yet implemented")
    override val location: Location
        get() = TODO("Not yet implemented")
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
