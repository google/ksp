package com.google.devtools.ksp.impl.symbol.kotlin.resolved

import com.google.devtools.ksp.common.IdKeyPair
import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.impl.ResolverAAImpl
import com.google.devtools.ksp.impl.symbol.java.KSValueArgumentLiteImpl
import com.google.devtools.ksp.impl.symbol.java.calcValue
import com.google.devtools.ksp.impl.symbol.kotlin.KSValueArgumentImpl
import com.google.devtools.ksp.impl.symbol.kotlin.analyze
import com.google.devtools.ksp.impl.symbol.kotlin.getDefaultValue
import com.google.devtools.ksp.impl.symbol.kotlin.toKtClassSymbol
import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.NonExistLocation
import com.google.devtools.ksp.symbol.Origin
import com.intellij.psi.PsiAnnotationMethod
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.compiled.ClsClassImpl
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaUnsupportedAnnotationValueImpl
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinAlwaysAccessibleLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*

class KSAnnotationResolvedImpl private constructor(
    private val annotationApplication: KaAnnotation,
    override val parent: KSNode?
) : KSAnnotation {
    companion object :
        KSObjectCache<IdKeyPair<KaAnnotation, KSNode?>, KSAnnotationResolvedImpl>() {
        fun getCached(annotationApplication: KaAnnotation, parent: KSNode? = null) =
            cache.getOrPut(IdKeyPair(annotationApplication, parent)) {
                KSAnnotationResolvedImpl(annotationApplication, parent)
            }
    }
    override val annotationType: KSTypeReference by lazy {
        analyze {
            KSTypeReferenceResolvedImpl.getCached(
                buildClassType(annotationApplication.classId!!),
                parent = this@KSAnnotationResolvedImpl
            )
        }
    }
    override val arguments: List<KSValueArgument> by lazy {
        val presentArgs = annotationApplication.arguments.map { KSValueArgumentImpl.getCached(it, Origin.KOTLIN) }
        val presentNames = presentArgs.mapNotNull { it.name?.asString() }
        val absentArgs = defaultArguments.filter {
            it.name?.asString() !in presentNames
        }
        presentArgs + absentArgs
    }

    @OptIn(KaImplementationDetail::class)
    override val defaultArguments: List<KSValueArgument> by lazy {
        analyze {
            annotationApplication.classId?.toKtClassSymbol()?.let { symbol ->
                if (symbol.origin == KaSymbolOrigin.JAVA_SOURCE && symbol.psi != null && symbol.psi !is ClsClassImpl) {
                    (symbol.psi as PsiClass).allMethods.filterIsInstance<PsiAnnotationMethod>()
                        .mapNotNull { annoMethod ->
                            annoMethod.defaultValue?.let { value ->
                                val calculatedValue: Any? = if (value is PsiArrayInitializerMemberValue) {
                                    value.initializers.map {
                                        calcValue(it)
                                    }
                                } else {
                                    calcValue(value)
                                }
                                KSValueArgumentLiteImpl.getCached(
                                    KSNameImpl.getCached(annoMethod.name),
                                    calculatedValue,
                                    Origin.SYNTHETIC
                                )
                            }
                        }
                } else {
                    symbol.memberScope.constructors.singleOrNull()?.let {
                        it.valueParameters.map { valueParameterSymbol ->
                            valueParameterSymbol.getDefaultValue().let { constantValue ->
                                KSValueArgumentImpl.getCached(
                                    KaBaseNamedAnnotationValue(
                                        valueParameterSymbol.name,
                                        constantValue
                                            ?: KaUnsupportedAnnotationValueImpl(
                                                KotlinAlwaysAccessibleLifetimeToken(ResolverAAImpl.ktModule.project)
                                            )
                                    ),
                                    Origin.SYNTHETIC
                                )
                            }
                        }
                    }
                }
            } ?: emptyList()
        }
    }

    override val shortName: KSName by lazy {
        KSNameImpl.getCached(annotationApplication.classId!!.shortClassName.asString())
    }

    override val useSiteTarget: AnnotationUseSiteTarget? by lazy {
        when (annotationApplication.useSiteTarget) {
            null -> null
            FILE -> AnnotationUseSiteTarget.FILE
            PROPERTY -> AnnotationUseSiteTarget.PROPERTY
            FIELD -> AnnotationUseSiteTarget.FIELD
            PROPERTY_GETTER -> AnnotationUseSiteTarget.GET
            PROPERTY_SETTER -> AnnotationUseSiteTarget.SET
            RECEIVER -> AnnotationUseSiteTarget.RECEIVER
            CONSTRUCTOR_PARAMETER -> AnnotationUseSiteTarget.PARAM
            SETTER_PARAMETER -> AnnotationUseSiteTarget.SETPARAM
            PROPERTY_DELEGATE_FIELD -> AnnotationUseSiteTarget.DELEGATE
        }
    }

    override val origin: Origin = Origin.KOTLIN_LIB

    override val location: Location by lazy {
        NonExistLocation
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnnotation(this, data)
    }

    override fun toString(): String {
        return "@${shortName.asString()}"
    }
}
