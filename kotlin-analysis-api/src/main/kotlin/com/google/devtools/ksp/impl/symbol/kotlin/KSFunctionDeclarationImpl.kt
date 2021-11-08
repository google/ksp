package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.symbol.FunctionKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunction
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import org.jetbrains.kotlin.analysis.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.psi.KtFile

class KSFunctionDeclarationImpl(private val ktFunctionSymbol: KtFunctionLikeSymbol) : KSFunctionDeclaration {
    override val functionKind: FunctionKind
        get() = TODO("Not yet implemented")
    override val isAbstract: Boolean by lazy {
        (ktFunctionSymbol as? KtFunctionSymbol)?.modality == Modality.ABSTRACT
    }
    override val extensionReceiver: KSTypeReference? by lazy {
        analyzeWithSymbolAsContext(ktFunctionSymbol) {
            if (!ktFunctionSymbol.isExtension) {
                null
            } else {
                ktFunctionSymbol.receiverType?.let { KSTypeReferenceImpl(it) }
            }
        }
    }
    override val returnType: KSTypeReference? by lazy {
        analyzeWithSymbolAsContext(ktFunctionSymbol) {
            KSTypeReferenceImpl(ktFunctionSymbol.annotatedType)
        }
    }
    override val parameters: List<KSValueParameter> by lazy {
        ktFunctionSymbol.valueParameters.map { KSValueParameterImpl(it) }
    }

    override fun findOverridee(): KSDeclaration? {
        TODO("Not yet implemented")
    }

    override fun asMemberOf(containing: KSType): KSFunction {
        TODO("Not yet implemented")
    }

    override val simpleName: KSName by lazy {
        if (ktFunctionSymbol is KtFunctionSymbol) {
            KSNameImpl(ktFunctionSymbol.name.asString())
        } else {
            KSNameImpl("<init>")
        }
    }
    override val qualifiedName: KSName?
        get() = TODO("Not yet implemented")
    override val typeParameters: List<KSTypeParameter> by lazy {
        (ktFunctionSymbol as? KtFunctionSymbol)?.typeParameters?.map { KSTypeParameterImpl(it) } ?: emptyList()
    }
    override val packageName: KSName by lazy {
        containingFile?.packageName ?: KSNameImpl("")
    }
    override val parentDeclaration: KSDeclaration?
        get() = TODO("Not yet implemented")
    override val containingFile: KSFile? by lazy {
        (ktFunctionSymbol.psi?.containingFile as? KtFile)?.let { KSFileSymbolImpl(it) }
    }
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
        return visitor.visitFunctionDeclaration(this, data)
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        (ktFunctionSymbol as KtAnnotatedSymbol).annotations.asSequence().map { KSAnnotationImpl(it) }
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

    override val declarations: Sequence<KSDeclaration>
        get() = TODO("Not yet implemented")
}

@OptIn(InvalidWayOfUsingAnalysisSession::class)
internal inline fun <R> analyzeWithSymbolAsContext(
    contextSymbol: KtSymbol,
    action: KtAnalysisSession.() -> R
): R {
    return KtAnalysisSessionProvider
        .getInstance(contextSymbol.psi!!.project).analyzeWithSymbolAsContext(contextSymbol, action)
}
