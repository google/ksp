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
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KSClassDeclarationEnumEntryImpl(private val ktEnumEntrySymbol: KtEnumEntrySymbol) : KSClassDeclaration {
    override val classKind: ClassKind = ClassKind.ENUM_ENTRY

    override val primaryConstructor: KSFunctionDeclaration? = null

    // TODO: Fix when type information is available in upstream.
    override val superTypes: Sequence<KSTypeReference> = emptySequence()

    override val isCompanionObject: Boolean = false

    override fun getSealedSubclasses(): Sequence<KSClassDeclaration> {
        TODO("Not yet implemented")
    }

    override fun getAllFunctions(): Sequence<KSFunctionDeclaration> {
        return analyzeWithSymbolAsContext(ktEnumEntrySymbol) {
            ktEnumEntrySymbol.getMemberScope().getCallableSymbols().filterIsInstance<KtFunctionLikeSymbol>()
                .map { KSFunctionDeclarationImpl(it) }
        }
    }

    override fun getAllProperties(): Sequence<KSPropertyDeclaration> {
        TODO("Not yet implemented")
    }

    override fun asType(typeArguments: List<KSTypeArgument>): KSType {
        TODO("Not yet implemented")
    }

    override fun asStarProjectedType(): KSType {
        TODO("Not yet implemented")
    }

    override val simpleName: KSName by lazy {
        KSNameImpl(ktEnumEntrySymbol.name.asString())
    }

    override val qualifiedName: KSName? by lazy {
        ktEnumEntrySymbol.containingEnumClassIdIfNonLocal?.let {
            KSNameImpl("${it.asFqNameString()}.${simpleName.asString()}")
        }
    }

    override val typeParameters: List<KSTypeParameter> = emptyList()

    override val packageName: KSName by lazy {
        ktEnumEntrySymbol.toContainingFile()!!.packageName
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        analyzeWithSymbolAsContext(ktEnumEntrySymbol) {
            ktEnumEntrySymbol.getContainingSymbol()
                .safeAs<KtNamedClassOrObjectSymbol>()?.let { KSClassDeclarationImpl(it) }
        }
    }

    override val containingFile: KSFile? by lazy {
        ktEnumEntrySymbol.toContainingFile()
    }
    override val docString: String?
        get() = TODO("Not yet implemented")

    override val modifiers: Set<Modifier>
        get() = TODO("Not yet implemented")

    override val origin: Origin
        get() = TODO("Not yet implemented")

    override val location: Location by lazy {
        ktEnumEntrySymbol.psi.toLocation()
    }

    override val parent: KSNode? by lazy {
        analyzeWithSymbolAsContext(ktEnumEntrySymbol) {
            ktEnumEntrySymbol.getContainingSymbol()
                .safeAs<KtNamedClassOrObjectSymbol>()?.let { KSClassDeclarationImpl(it) }
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitClassDeclaration(this, data)
    }

    override val annotations: Sequence<KSAnnotation> = emptySequence()

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
        // TODO: fix after .getDeclaredMemberScope() works for enum entry with no initializer.
        emptySequence()
    }
}
