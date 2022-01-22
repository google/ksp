package com.google.devtools.ksp.impl.symbol.kotlin

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
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.psi.KtFile

class KSPropertyDeclarationImpl(private val ktPropertySymbol: KtPropertySymbol) : KSPropertyDeclaration {
    override val getter: KSPropertyGetter?
        get() = TODO("Not yet implemented")
    override val setter: KSPropertySetter?
        get() = TODO("Not yet implemented")
    override val extensionReceiver: KSTypeReference?
        get() = TODO("Not yet implemented")
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

    override val simpleName: KSName
        get() = TODO("Not yet implemented")
    override val qualifiedName: KSName?
        get() = TODO("Not yet implemented")
    override val typeParameters: List<KSTypeParameter> by lazy {
        ktPropertySymbol.typeParameters.map { KSTypeParameterImpl(it) }
    }
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
    override val parent: KSNode? by lazy {
        analyzeWithSymbolAsContext(ktPropertySymbol) {
            ktPropertySymbol.getContainingSymbol()?.let{ KSClassDeclarationImpl(it as KtNamedClassOrObjectSymbol)}
                ?: KSFileSymbolImpl(ktPropertySymbol.psi!!.containingFile as KtFile)
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        TODO("Not yet implemented")
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
