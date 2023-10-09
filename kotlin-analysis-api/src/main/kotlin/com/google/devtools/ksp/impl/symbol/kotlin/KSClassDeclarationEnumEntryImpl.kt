package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.processing.impl.KSNameImpl
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSExpectActual
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
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol

class KSClassDeclarationEnumEntryImpl private constructor(private val ktEnumEntrySymbol: KtEnumEntrySymbol) :
    KSClassDeclaration,
    AbstractKSDeclarationImpl(ktEnumEntrySymbol),
    KSExpectActual by KSExpectActualImpl(ktEnumEntrySymbol) {
    companion object : KSObjectCache<KtEnumEntrySymbol, KSClassDeclarationEnumEntryImpl>() {
        fun getCached(ktEnumEntrySymbol: KtEnumEntrySymbol) =
            cache.getOrPut(ktEnumEntrySymbol) { KSClassDeclarationEnumEntryImpl(ktEnumEntrySymbol) }
    }

    override val qualifiedName: KSName? by lazy {
        KSNameImpl.getCached("${this.parentDeclaration!!.qualifiedName!!.asString()}.${simpleName.asString()}")
    }

    override val classKind: ClassKind = ClassKind.ENUM_ENTRY

    override val primaryConstructor: KSFunctionDeclaration? = null

    override val superTypes: Sequence<KSTypeReference> = emptySequence()

    override val isCompanionObject: Boolean = false

    override fun getSealedSubclasses(): Sequence<KSClassDeclaration> {
        TODO("Not yet implemented")
    }

    override fun getAllFunctions(): Sequence<KSFunctionDeclaration> {
        return ktEnumEntrySymbol.declarations().filterIsInstance<KSFunctionDeclaration>()
    }

    override fun getAllProperties(): Sequence<KSPropertyDeclaration> {
        return ktEnumEntrySymbol.declarations().filterIsInstance<KSPropertyDeclaration>()
    }

    override fun asType(typeArguments: List<KSTypeArgument>): KSType {
        return KSTypeImpl.getCached(ktEnumEntrySymbol.returnType).replace(typeArguments)
    }

    override fun asStarProjectedType(): KSType {
        return KSTypeImpl.getCached(ktEnumEntrySymbol.returnType).starProjection()
    }

    override val typeParameters: List<KSTypeParameter> = emptyList()

    override val packageName: KSName by lazy {
        ktEnumEntrySymbol.toContainingFile()!!.packageName
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        analyze {
            (
                ktEnumEntrySymbol.getContainingSymbol()
                    as? KtNamedClassOrObjectSymbol
                )?.let { KSClassDeclarationImpl.getCached(it) }
        }
    }

    override val containingFile: KSFile? by lazy {
        ktEnumEntrySymbol.toContainingFile()
    }

    override val location: Location by lazy {
        ktEnumEntrySymbol.psi.toLocation()
    }

    override val parent: KSNode? by lazy {
        analyze {
            (ktEnumEntrySymbol.getContainingSymbol() as? KtNamedClassOrObjectSymbol)
                ?.let { KSClassDeclarationImpl.getCached(it) }
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitClassDeclaration(this, data)
    }

    override val annotations: Sequence<KSAnnotation> = emptySequence()

    override val declarations: Sequence<KSDeclaration> by lazy {
        // TODO: fix after .getDeclaredMemberScope() works for enum entry with no initializer.
        emptySequence()
    }

    override fun defer(): Restorable? {
        return ktEnumEntrySymbol.defer(Companion::getCached)
    }
}
