package com.google.devtools.ksp.impl.symbol.java

import com.google.devtools.ksp.common.IdKeyPair
import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.impl.ResolverAAImpl
import com.google.devtools.ksp.impl.recordClassReferenceLookup
import com.google.devtools.ksp.impl.recordLookup
import com.google.devtools.ksp.impl.symbol.kotlin.DeferrableAnnotation
import com.google.devtools.ksp.impl.symbol.kotlin.KSClassDeclarationEnumEntryImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSErrorType
import com.google.devtools.ksp.impl.symbol.kotlin.analyze
import com.google.devtools.ksp.impl.symbol.kotlin.classifierSymbol
import com.google.devtools.ksp.impl.symbol.kotlin.deferAnnotationByOrdinal
import com.google.devtools.ksp.impl.symbol.kotlin.getAbsentDefaultArguments
import com.google.devtools.ksp.impl.symbol.kotlin.getDefaultArguments
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.impl.symbol.kotlin.toKaType
import com.google.devtools.ksp.impl.symbol.kotlin.toLocation
import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
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
import com.intellij.psi.impl.compiled.ClsFileImpl
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId

class KSAnnotationJavaImpl private constructor(
    private val psi: PsiAnnotation,
    override val parent: KSNode?,
    override val origin: Origin
) : KSAnnotation, DeferrableAnnotation {
    companion object : KSObjectCache<IdKeyPair<PsiAnnotation, KSNode?>, KSAnnotationJavaImpl>() {
        fun getCached(
            psi: PsiAnnotation,
            parent: KSNode?,
            origin: Origin = when {
                parent == null -> Origin.SYNTHETIC
                psi.containingFile is ClsFileImpl -> Origin.JAVA_LIB
                else -> Origin.JAVA
            }
        ) = cache.getOrPut(IdKeyPair(psi, parent)) { KSAnnotationJavaImpl(psi, parent, origin) }
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
        }.also {
            if (origin == Origin.JAVA) {
                recordLookup(it, this)
            }
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
            val presentOrigin = if (psi.containingFile is ClsFileImpl) Origin.JAVA_LIB else Origin.JAVA
            val presentArgs = psi.parameterList.attributes.mapIndexed { index, it ->
                val name = it.name ?: annotationConstructor?.valueParameters?.getOrNull(index)?.name?.asString()
                KSValueArgumentLiteImpl(
                    name?.let { KSNameImpl.getCached(it) },
                    { arg -> calcValue(it.value, arg, presentOrigin) },
                    this@KSAnnotationJavaImpl,
                    presentOrigin,
                    it.toLocation()
                )
            }
            presentArgs + getAbsentDefaultArguments(presentArgs)
        }
    }

    override val defaultArguments: List<KSValueArgument> by lazy {
        getDefaultArguments(classId)
    }

    override val shortName: KSName by lazy {
        KSNameImpl.getCached(psi.qualifiedName!!.split(".").last())
    }

    override val useSiteTarget: AnnotationUseSiteTarget? = null

    override val location: Location
        get() = psi.toLocation()

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnnotation(this, data)
    }

    override fun toString(): String {
        return "@${shortName.asString()}"
    }

    internal val annotationFqName: String?
        get() = psi.resolveAnnotationType()?.qualifiedName

    override fun deferAnnotation(): (() -> KSAnnotation?)? {
        return deferAnnotationByOrdinal(annotationFqName)
    }
}

fun calcValue(value: PsiAnnotationMemberValue?, parent: KSNode?, origin: Origin = Origin.JAVA): Any? {
    if (value is PsiArrayInitializerMemberValue) {
        return value.initializers.map {
            calcValue(it, parent, origin)
        }
    }
    if (value is PsiAnnotation) {
        val fqn = value.resolveAnnotationType()?.qualifiedName ?: value.qualifiedName
        if (fqn != null && parent != null) {
            recordClassReferenceLookup(fqn, parent)
        }
        return KSAnnotationJavaImpl.getCached(value, parent, origin)
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
        is PsiType -> resolvePsiClassLiteralType(result, parent)

        is PsiLiteralValue -> {
            result.value
        }

        is PsiField -> {
            // manually handle enums as constant expression evaluator does not seem to be resolving them.
            val containingClass = result.containingClass
            @Suppress("UnstableApiUsage")
            if (containingClass?.classKind == JvmClassKind.ENUM) {
                // this is an enum entry
                containingClass.qualifiedName?.let { fqn ->
                    if (parent != null) {
                        recordClassReferenceLookup(fqn, parent)
                    }
                    ResolverAAImpl.instance.getClassDeclarationByName(fqn)
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

private fun resolvePsiClassLiteralType(psiType: PsiType, parent: KSNode?): KSType = when (psiType) {
    is PsiPrimitiveType -> {
        psiType.boxedTypeName?.let {
            ResolverAAImpl.instance
                .getClassDeclarationByName(it)?.asStarProjectedType()
        } ?: KSErrorType(psiType.boxedTypeName)
    }

    is PsiArrayType -> {
        val componentType = resolvePsiClassLiteralType(psiType.componentType, parent)
        val componentTypeRef = ResolverAAImpl.instance.createKSTypeReferenceFromKSType(componentType)
        val typeArgs = listOf(ResolverAAImpl.instance.getTypeArgument(componentTypeRef, Variance.INVARIANT))
        ResolverAAImpl.instance
            .getClassDeclarationByName("kotlin.Array")!!.asType(typeArgs)
    }

    else -> {
        ResolverAAImpl.instance
            .getClassDeclarationByName(psiType.canonicalText)?.asStarProjectedType()
            ?.also { ksType ->
                ksType.toKaType()?.let { kaType ->
                    parent?.let { ctx ->
                        recordClassReferenceLookup(kaType, ctx)
                    }
                }
            }
            ?: KSErrorType(psiType.canonicalText)
    }
}
