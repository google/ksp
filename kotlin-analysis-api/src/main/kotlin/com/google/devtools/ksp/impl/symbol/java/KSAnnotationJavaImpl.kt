package com.google.devtools.ksp.impl.symbol.java

import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.impl.ResolverAAImpl
import com.google.devtools.ksp.impl.recordClassReferenceLookup
import com.google.devtools.ksp.impl.symbol.kotlin.KSAnnotationOps.getAbsentDefaultArguments
import com.google.devtools.ksp.impl.symbol.kotlin.KSAnnotationOps.getDefaultArguments
import com.google.devtools.ksp.impl.symbol.kotlin.KSClassDeclarationEnumEntryImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSErrorType
import com.google.devtools.ksp.impl.symbol.kotlin.analyze
import com.google.devtools.ksp.impl.symbol.kotlin.classifierSymbol
import com.google.devtools.ksp.impl.symbol.kotlin.findParentDeclaration
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.impl.symbol.kotlin.toKaType
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
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralValue
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId

class KSAnnotationJavaImpl private constructor(private val psi: PsiAnnotation, override val parent: KSNode?) :
    KSAnnotation {
    companion object : KSObjectCache<PsiAnnotation, KSAnnotationJavaImpl>() {
        fun getCached(psi: PsiAnnotation, parent: KSNode?) =
            KSAnnotationJavaImpl.cache.getOrPut(psi) { KSAnnotationJavaImpl(psi, parent) }
    }

    private val classId: ClassId by lazy {
        fun PsiClass.fqn(): String? {
            val parent = containingClass?.fqn()
                ?: return qualifiedName?.replace('.', '/')
            if (name == null)
                return null
            return "$parent.$name"
        }
        analyze {
            val resolved = psi.resolveAnnotationType()
            val fqn = resolved?.fqn() ?: "__KSP_unresolved_${psi.qualifiedName}"
            ClassId.fromString(fqn)
        }
    }

    private val type: KaType by lazy {
        analyze {
            buildClassType(classId)
        }
    }

    override val annotationType: KSTypeReference by lazy {
        // TODO: replace with psi based implementation once `PsiType -> KtType` is supported in AA.
        KSTypeReferenceResolvedImpl.getCached(type, this)
    }

    override val arguments: List<KSValueArgument> by lazy {
        analyze {
            val annotationConstructor =
                (type.classifierSymbol() as? KaClassSymbol)?.memberScope?.constructors?.singleOrNull()
            val presentArgs = psi.parameterList.attributes.mapIndexed { index, it ->
                val name = it.name ?: annotationConstructor?.valueParameters?.getOrNull(index)?.name?.asString()
                KSValueArgumentLiteImpl(
                    name?.let { KSNameImpl.getCached(it) },
                    calcValue(it.value, this@KSAnnotationJavaImpl),
                    this@KSAnnotationJavaImpl,
                    Origin.JAVA,
                    it.toLocation()
                )
            }
            presentArgs + getAbsentDefaultArguments(this@KSAnnotationJavaImpl, presentArgs)
        }
    }

    override val defaultArguments: List<KSValueArgument> by lazy {
        getDefaultArguments(this@KSAnnotationJavaImpl, classId)
    }

    override val shortName: KSName by lazy {
        KSNameImpl.getCached(psi.qualifiedName!!.split(".").last())
    }

    override val useSiteTarget: AnnotationUseSiteTarget? = null

    override val origin: Origin = if (parent == null) Origin.SYNTHETIC else Origin.JAVA

    override val location: Location
        get() = psi.toLocation()

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnnotation(this, data)
    }

    override fun toString(): String {
        return "@${shortName.asString()}"
    }
}

fun calcValue(value: PsiAnnotationMemberValue?, parent: KSNode?): Any? {
    if (value is PsiArrayInitializerMemberValue) {
        return value.initializers.map {
            calcValue(it, parent)
        }
    }
    if (value is PsiAnnotation) {
        value.qualifiedName?.let { fqn ->
            parent?.findParentDeclaration()?.let { decl ->
                recordClassReferenceLookup(fqn, decl)
            }
        }
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
                    .getClassDeclarationByName(it)?.asStarProjectedType()
            } ?: KSErrorType(result.boxedTypeName)
        }

        is PsiArrayType -> {
            val componentType = when (val component = result.componentType) {
                is PsiPrimitiveType -> component.boxedTypeName?.let { boxedTypeName ->
                    ResolverAAImpl.instance
                        .getClassDeclarationByName(boxedTypeName)?.asStarProjectedType()
                } ?: KSErrorType(component.boxedTypeName)

                else -> {
                    ResolverAAImpl.instance
                        .getClassDeclarationByName(component.canonicalText)?.asStarProjectedType()
                        ?.also { ksType ->
                            ksType.toKaType()?.let { kaType ->
                                parent?.findParentDeclaration()?.let { decl ->
                                    recordClassReferenceLookup(kaType, decl)
                                }
                            }
                        }
                        ?: KSErrorType(component.canonicalText)
                }
            }
            val componentTypeRef = ResolverAAImpl.instance.createKSTypeReferenceFromKSType(componentType)
            val typeArgs = listOf(ResolverAAImpl.instance.getTypeArgument(componentTypeRef, Variance.INVARIANT))
            ResolverAAImpl.instance
                .getClassDeclarationByName("kotlin.Array")!!.asType(typeArgs)
        }

        is PsiType -> {
            ResolverAAImpl.instance
                .getClassDeclarationByName(result.canonicalText)?.asStarProjectedType()
                ?.also { ksType ->
                    ksType.toKaType()?.let { kaType ->
                        parent?.findParentDeclaration()?.let { decl ->
                            recordClassReferenceLookup(kaType, decl)
                        }
                    }
                }
                ?: KSErrorType(result.canonicalText)
        }

        is PsiLiteralValue -> {
            result.value
        }

        is PsiField -> {
            // manually handle enums as constant expression evaluator does not seem to be resolving them.
            val containingClass = result.containingClass
            @Suppress("UnstableApiUsage")
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
