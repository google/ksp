package com.google.devtools.ksp.impl.symbol.java

import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.impl.ResolverAAImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSClassDeclarationEnumEntryImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSErrorType
import com.google.devtools.ksp.impl.symbol.kotlin.KSValueArgumentImpl
import com.google.devtools.ksp.impl.symbol.kotlin.analyze
import com.google.devtools.ksp.impl.symbol.kotlin.classifierSymbol
import com.google.devtools.ksp.impl.symbol.kotlin.getDefaultValue
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.impl.symbol.kotlin.toLocation
import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.Variance
import com.intellij.lang.jvm.JvmClassKind
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiAnnotationMethod
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralValue
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiType
import com.intellij.psi.impl.compiled.ClsClassImpl
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KtUnsupportedAnnotationValue
import org.jetbrains.kotlin.analysis.api.components.buildClassType
import org.jetbrains.kotlin.analysis.api.lifetime.KtAlwaysAccessibleLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class KSAnnotationJavaImpl private constructor(private val psi: PsiAnnotation, override val parent: KSNode?) :
    KSAnnotation {
    companion object : KSObjectCache<PsiAnnotation, KSAnnotationJavaImpl>() {
        fun getCached(psi: PsiAnnotation, parent: KSNode?) =
            KSAnnotationJavaImpl.cache.getOrPut(psi) { KSAnnotationJavaImpl(psi, parent) }
    }

    private val type: KtType by lazy {
        // TODO: local class annotations?
        analyze {
            buildClassType(ClassId.topLevel(FqName(psi.qualifiedName!!)))
        }
    }

    override val annotationType: KSTypeReference by lazy {
        // TODO: repleace with psi based implementation once `PsiType -> KtType` is supported in AA.
        KSTypeReferenceResolvedImpl.getCached(type, this)
    }

    override val arguments: List<KSValueArgument> by lazy {
        val annotationConstructor = analyze {
            (type.classifierSymbol() as? KtClassOrObjectSymbol)?.getMemberScope()?.getConstructors()?.singleOrNull()
        }
        val presentArgs = psi.parameterList.attributes.mapIndexed { index, it ->
            val name = it.name ?: annotationConstructor?.valueParameters?.getOrNull(index)?.name?.asString()
            val value = it.value
            val calculatedValue: Any? = if (value is PsiArrayInitializerMemberValue) {
                value.initializers.map {
                    calcValue(it)
                }
            } else {
                calcValue(it.value)
            }
            KSValueArgumentLiteImpl.getCached(
                name?.let { KSNameImpl.getCached(it) },
                calculatedValue,
                Origin.JAVA
            )
        }
        val presentValueArgumentNames = presentArgs.map { it.name?.asString() ?: "" }
        presentArgs + defaultArguments.filter { it.name?.asString() !in presentValueArgumentNames }
    }

    @OptIn(KtAnalysisApiInternals::class)
    override val defaultArguments: List<KSValueArgument> by lazy {
        analyze {
            (type.classifierSymbol() as? KtClassOrObjectSymbol)?.getMemberScope()?.getConstructors()?.singleOrNull()
                ?.let { symbol ->
                    // ClsClassImpl means psi is decompiled psi.
                    if (symbol.origin == KtSymbolOrigin.JAVA && symbol.psi != null && symbol.psi !is ClsClassImpl) {
                        (symbol.psi as PsiClass).allMethods.filterIsInstance<PsiAnnotationMethod>()
                            .mapNotNull { annoMethod ->
                                annoMethod.defaultValue?.let {
                                    val value = it
                                    val calculatedValue: Any? = if (value is PsiArrayInitializerMemberValue) {
                                        value.initializers.map {
                                            calcValue(it)
                                        }
                                    } else {
                                        calcValue(it)
                                    }
                                    KSValueArgumentLiteImpl.getCached(
                                        KSNameImpl.getCached(annoMethod.name),
                                        calculatedValue,
                                        Origin.SYNTHETIC
                                    )
                                }
                            }
                    } else {
                        symbol.valueParameters.map { valueParameterSymbol ->
                            valueParameterSymbol.getDefaultValue().let { constantValue ->
                                KSValueArgumentImpl.getCached(
                                    KtNamedAnnotationValue(
                                        valueParameterSymbol.name,
                                        // null will be returned as the `constantValue` for non array annotation values.
                                        // fallback to unsupported annotation value to indicate such use cases.
                                        // when seeing unsupported annotation value we return `null` for the value.
                                        // which might still be incorrect but there might not be a perfect way.
                                        constantValue
                                            ?: KtUnsupportedAnnotationValue(
                                                KtAlwaysAccessibleLifetimeToken(ResolverAAImpl.ktModule.project)
                                            ),
                                        KtAlwaysAccessibleLifetimeToken(ResolverAAImpl.ktModule.project)
                                    ),
                                    Origin.SYNTHETIC
                                )
                            }
                        }
                    }
                }
        } ?: emptyList()
    }

    override val shortName: KSName by lazy {
        KSNameImpl.getCached(psi.qualifiedName!!.split(".").last())
    }

    override val useSiteTarget: AnnotationUseSiteTarget? = null

    override val origin: Origin = Origin.JAVA

    override val location: Location
        get() = psi.toLocation()

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnnotation(this, data)
    }

    override fun toString(): String {
        return "@${shortName.asString()}"
    }
}

fun calcValue(value: PsiAnnotationMemberValue?): Any? {
    if (value is PsiAnnotation) {
        return KSAnnotationJavaImpl.getCached(value, null)
    }
    val result = when (value) {
        is PsiReference -> value.resolve()?.let { resolved ->
            JavaPsiFacade.getInstance(value.project).constantEvaluationHelper.computeConstantExpression(value)
                ?: resolved
        }
        else -> value?.let {
            JavaPsiFacade.getInstance(value.project).constantEvaluationHelper.computeConstantExpression(value)
        }
    }
    return when (result) {
        is PsiPrimitiveType -> {
            result.boxedTypeName?.let {
                ResolverAAImpl.instance
                    .getClassDeclarationByName(result.boxedTypeName!!)?.asStarProjectedType() ?: KSErrorType
            }
        }
        is PsiArrayType -> {
            val componentType = when (val component = result.componentType) {
                is PsiPrimitiveType -> component.boxedTypeName?.let {
                    ResolverAAImpl.instance
                        .getClassDeclarationByName(component.boxedTypeName!!)?.asStarProjectedType()
                } ?: KSErrorType
                else -> {
                    ResolverAAImpl.instance
                        .getClassDeclarationByName(component.canonicalText)?.asStarProjectedType() ?: KSErrorType
                }
            }
            val componentTypeRef = ResolverAAImpl.instance.createKSTypeReferenceFromKSType(componentType)
            val typeArgs = listOf(ResolverAAImpl.instance.getTypeArgument(componentTypeRef, Variance.INVARIANT))
            ResolverAAImpl.instance
                .getClassDeclarationByName("kotlin.Array")!!.asType(typeArgs)
        }
        is PsiType -> {
            ResolverAAImpl.instance
                .getClassDeclarationByName(result.canonicalText)?.asStarProjectedType() ?: KSErrorType
        }
        is PsiLiteralValue -> {
            result.value
        }
        is PsiField -> {
            // manually handle enums as constant expression evaluator does not seem to be resolving them.
            val containingClass = result.containingClass
            if (containingClass?.classKind == JvmClassKind.ENUM) {
                // this is an enum entry
                containingClass.qualifiedName?.let {
                    ResolverAAImpl.instance.getClassDeclarationByName(it)
                }?.declarations?.find {
                    it is KSClassDeclaration && it.classKind == ClassKind.ENUM_ENTRY &&
                        it.simpleName.asString() == result.name
                } as? KSClassDeclarationEnumEntryImpl
            } else {
                null
            }
        }
        else -> result
    }
}
