/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.ExceptionMessage
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.impl.KSPCoreEnvironment
import com.google.devtools.ksp.impl.ResolverAAImpl
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSAnnotationResolvedImpl
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSClassifierParameterImpl
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSClassifierReferenceResolvedImpl
import com.google.devtools.ksp.impl.symbol.util.getDocString
import com.google.devtools.ksp.symbol.*
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.impl.compiled.ClsMemberImpl
import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.components.KaSubstitutorBuilder
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirAnnotationValueConverter
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirFunctionType
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseStarTypeProjection
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinAlwaysAccessibleLifetimeToken
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.state.InfoForMangling
import org.jetbrains.kotlin.codegen.state.collectFunctionSignatureForManglingSuffix
import org.jetbrains.kotlin.codegen.state.md5base64
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.moduleName
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.toFirExpression
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationArgument
import org.jetbrains.kotlin.load.java.structure.impl.JavaUnknownAnnotationArgumentImpl
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_SUPPRESS_WILDCARDS_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_WILDCARD_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.getEffectiveVariance
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

internal val ktSymbolOriginToOrigin = mapOf(
    KaSymbolOrigin.JAVA_SOURCE to Origin.JAVA,
    KaSymbolOrigin.JAVA_LIBRARY to Origin.JAVA_LIB,
    KaSymbolOrigin.SOURCE to Origin.KOTLIN,
    KaSymbolOrigin.SAM_CONSTRUCTOR to Origin.SYNTHETIC,
    KaSymbolOrigin.SOURCE_MEMBER_GENERATED to Origin.SYNTHETIC,
    KaSymbolOrigin.DELEGATED to Origin.SYNTHETIC,
    KaSymbolOrigin.PROPERTY_BACKING_FIELD to Origin.KOTLIN,
    KaSymbolOrigin.JAVA_SYNTHETIC_PROPERTY to Origin.SYNTHETIC,
    KaSymbolOrigin.INTERSECTION_OVERRIDE to Origin.KOTLIN,
    // TODO: distinguish between kotlin library and java library.
    KaSymbolOrigin.LIBRARY to Origin.KOTLIN_LIB,
    KaSymbolOrigin.SUBSTITUTION_OVERRIDE to Origin.JAVA_LIB
)

internal fun mapAAOrigin(ktSymbol: KaSymbol): Origin {
    val symbolOrigin = ktSymbolOriginToOrigin[ktSymbol.origin]
        ?: throw IllegalStateException("unhandled origin ${ktSymbol.origin.name}")
    return if (symbolOrigin == Origin.JAVA && ktSymbol.psi?.containingFile?.fileType?.isBinary == true) {
        Origin.JAVA_LIB
    } else {
        if (ktSymbol.psi == null) {
            if (analyze { ktSymbol.containingModule is KaLibraryModule }) {
                Origin.KOTLIN_LIB
            } else {
                Origin.SYNTHETIC
            }
        } else {
            symbolOrigin
        }
    }
}

internal fun KaAnnotation.render(): String {
    return buildString {
        append("@")
        if (this@render.useSiteTarget != null) {
            append(this@render.useSiteTarget!!.renderName + ":")
        }
        append(this@render.classId!!.shortClassName.asString())
        if (arguments.isNotEmpty()) {
            append("(")
            append(arguments.joinToString(", ") { it.expression.render() })
            append(")")
        }
    }
}

internal fun KaAnnotationValue.render(): String {
    return when (this) {
        is KaAnnotationValue.NestedAnnotationValue -> annotation.render()
        is KaAnnotationValue.ArrayValue -> values.joinToString(",", "{", "}") { it.render() }
        is KaAnnotationValue.ConstantValue -> value.render()
        is KaAnnotationValue.EnumEntryValue -> callableId.toString()
        is KaAnnotationValue.ClassLiteralValue -> {
            val type = KSTypeImpl.getCached(type)
            // KSTypeImpl takes care of the error types, if applicable
            if (type.isError) type.toString() else "$type::class"
        }

        is KaAnnotationValue.UnsupportedValue -> throw IllegalStateException("Unsupported annotation value: $this")
    }
}

@OptIn(KaNonPublicApi::class)
internal fun KaType.render(inFunctionType: Boolean = false): String {
    return buildString {
        if (annotations.isNotEmpty()) {
            append("[")
            append(annotations.joinToString(", ") { it.render() })
            append("] ")
        }
        append(
            when (this@render) {
                is KaClassType -> buildString {
                    val symbol = this@render.classifierSymbol()
                    if (symbol is KaTypeAliasSymbol) {
                        if (!inFunctionType) {
                            append("[typealias ${symbol.name.asString()}]")
                        } else {
                            append(this@render.fullyExpand().render(inFunctionType = true))
                        }
                    } else {
                        append(this@render.symbol.name?.asString())
                        if (typeArguments().isNotEmpty()) {
                            typeArguments().joinToString(separator = ", ", prefix = "<", postfix = ">") {
                                when (it) {
                                    is KaStarTypeProjection -> "*"
                                    is KaTypeArgumentWithVariance ->
                                        "${it.variance}" +
                                            (if (it.variance != Variance.INVARIANT) " " else "") +
                                            it.type.render(this@render is KaFunctionType)

                                    else -> throw IllegalStateException("Unhandled type argument type ${it.javaClass}")
                                }
                            }.also { append(it) }
                        }
                    }
                }

                is KaClassErrorType -> KSErrorType(qualifiers.joinToString(".") { it.name.asString() }).toString()
                is KaErrorType -> KSErrorType(presentableText).toString()
                is KaCapturedType -> this@render.toString()
                is KaDefinitelyNotNullType -> original.render(inFunctionType) + " & Any"
                is KaDynamicType -> "<dynamic type>"
                is KaFlexibleType -> "(${lowerBound.render(inFunctionType)}..${upperBound.render(inFunctionType)})"
                is KaIntersectionType ->
                    this@render.conjuncts
                        .joinToString(separator = " & ", prefix = "(", postfix = ")") { it.render(inFunctionType) }

                is KaTypeParameterType -> name.asString()
                else -> throw IllegalStateException("Unhandled type ${this@render.javaClass}")
            } + if (nullability == KaTypeNullability.NULLABLE) "?" else ""
        )
    }
}

internal fun KaType.toClassifierReference(parent: KSTypeReference?): KSReferenceElement? {
    return when (val ktType = this) {
        is KaFunctionType -> KSCallableReferenceImpl.getCached(ktType, parent)
        is KaDynamicType -> KSDynamicReferenceImpl.getCached(parent!!)
        is KaUsualClassType -> KSClassifierReferenceResolvedImpl.getCached(ktType, ktType.qualifiers.size - 1, parent)
        is KaFlexibleType -> ktType.lowerBound.toClassifierReference(parent)
        is KaErrorType -> null
        is KaTypeParameterType -> KSClassifierParameterImpl.getCached(ktType, parent)
        is KaDefinitelyNotNullType -> KSDefNonNullReferenceImpl.getCached(ktType, parent)
        else -> throw IllegalStateException("Unexpected type element ${ktType.javaClass}, $ExceptionMessage")
    }
}

@OptIn(KaImplementationDetail::class)
internal fun KSTypeArgument.toKtTypeProjection(): KaTypeProjection {
    val variance = when (this.variance) {
        com.google.devtools.ksp.symbol.Variance.INVARIANT -> Variance.INVARIANT
        com.google.devtools.ksp.symbol.Variance.COVARIANT -> Variance.OUT_VARIANCE
        com.google.devtools.ksp.symbol.Variance.CONTRAVARIANT -> Variance.IN_VARIANCE
        else -> null
    }
    val argType = (this.type?.resolve() as? KSTypeImpl)?.type
    // TODO: maybe make a singleton of alwaysAccessibleLifetimeToken?
    val alwaysAccessibleLifetimeToken = KotlinAlwaysAccessibleLifetimeToken(ResolverAAImpl.ktModule.project)
    return if (argType == null || variance == null) {
        KaBaseStarTypeProjection(alwaysAccessibleLifetimeToken)
    } else {
        KaBaseTypeArgumentWithVariance(argType, variance, alwaysAccessibleLifetimeToken)
    }
}

internal fun PsiElement?.toLocation(): Location {
    if (this == null) {
        return NonExistLocation
    }
    val file = this.containingFile
    val document = KSPCoreEnvironment.instance.psiDocumentManager.getDocument(file) ?: return NonExistLocation
    return FileLocation(file.virtualFile.path, document.getLineNumber(this.textOffset) + 1)
}

internal fun KaSymbol.toContainingFile(): KSFile? {
    return when (val psi = this.psi) {
        is KtElement -> analyze {
            KSFileImpl.getCached(psi.containingKtFile.symbol)
        }

        is PsiElement -> KSFileJavaImpl.getCached(psi.containingFile as PsiJavaFile)
        else -> null
    }
}

internal fun KaSymbol.toDocString(): String? = this.psi?.getDocString()

internal inline fun <R> analyze(crossinline action: KaSession.() -> R): R {
    return analyze(ResolverAAImpl.ktModule, action)
}

internal fun KaDeclarationContainerSymbol.declarations(): Sequence<KSDeclaration> {
    return analyze {
        this@declarations.let {
            it.declaredMemberScope.declarations + it.staticDeclaredMemberScope.declarations
        }.distinct().map { symbol ->
            when (symbol) {
                is KaNamedClassSymbol -> KSClassDeclarationImpl.getCached(symbol)
                is KaFunctionSymbol -> KSFunctionDeclarationImpl.getCached(symbol)
                is KaPropertySymbol -> KSPropertyDeclarationImpl.getCached(symbol)
                is KaEnumEntrySymbol -> KSClassDeclarationEnumEntryImpl.getCached(symbol)
                is KaJavaFieldSymbol -> KSPropertyDeclarationJavaImpl.getCached(symbol)
                else -> throw IllegalStateException()
            }
        }
    }
}

@OptIn(KaExperimentalApi::class)
internal fun KaDeclarationContainerSymbol.getAllProperties(): Sequence<KSPropertyDeclaration> {
    return analyze {
        this@getAllProperties.memberScope.callables { true }
            .filter {
                it.isVisibleInClass(this@getAllProperties as KaClassSymbol) ||
                    it.containingSymbol == this@getAllProperties ||
                    it.containingSymbol?.psi == this@getAllProperties.psi
            }
            .mapNotNull { callableSymbol ->
                when (callableSymbol) {
                    is KaPropertySymbol -> KSPropertyDeclarationImpl.getCached(callableSymbol)
                    is KaJavaFieldSymbol -> KSPropertyDeclarationJavaImpl.getCached(callableSymbol)
                    else -> null
                }
            }
    }
}

@OptIn(KaExperimentalApi::class)
internal fun KaDeclarationContainerSymbol.getAllFunctions(): Sequence<KSFunctionDeclaration> {
    return analyze {
        this@getAllFunctions.memberScope.let { it.callables { true } + it.constructors }
            .filter {
                it.isVisibleInClass(this@getAllFunctions as KaClassSymbol) ||
                    it.containingSymbol == this@getAllFunctions ||
                    it.containingSymbol?.psi == this@getAllFunctions.psi
            }
            .mapNotNull { callableSymbol ->
                // TODO: replace with single safe cast if no more implementations of KSFunctionDeclaration is added.
                when (callableSymbol) {
                    is KaFunctionSymbol -> KSFunctionDeclarationImpl.getCached(callableSymbol)
                    else -> null
                }
            }
    }
}

private val jvmRepeatableClassId = ClassId.fromString("java/lang/annotation/Repeatable")
private val repeatableClassId = ClassId.fromString("kotlin/annotation/Repeatable")

private fun KaClassSymbol.isRepeatableAnnotation(container: KaAnnotation): Boolean {
    return this.classKind == KaClassKind.ANNOTATION_CLASS && annotations.any {
        when (it.classId) {
            jvmRepeatableClassId -> {
                it.arguments.singleOrNull()?.let { arg ->
                    val expression = arg.expression as? KaAnnotationValue.ClassLiteralValue ?: return@let null
                    arg.name.asString() == "value" && expression.classId == container.classId
                } ?: false
            }

            repeatableClassId -> {
                container.classId?.asFqNameString() == "${this.classId?.asFqNameString()}.Container"
            }

            else -> false
        }
    }
}

private fun KaAnnotated.annotationsWithRepeatableUnfolded(): List<KaAnnotation> {
    return if (
        this is KaSymbol && (this.origin == KaSymbolOrigin.LIBRARY || this.origin == KaSymbolOrigin.JAVA_LIBRARY)
    ) {
        annotations.flatMap { container ->
            // Try to unwrap repeatable containers
            // https://github.com/Kotlin/KEEP/blob/master/proposals/repeatable-annotations.md
            val containedAnnotations: List<KaAnnotation>? =
                (container.arguments.singleOrNull()?.expression as? KaAnnotationValue.ArrayValue)?.values?.map {
                    (it as? KaAnnotationValue.NestedAnnotationValue)?.annotation ?: return@flatMap listOf(container)
                }
            val containedAnnotationClassId: ClassId =
                containedAnnotations?.firstOrNull()?.classId ?: return@flatMap listOf(container)
            val containedClass = containedAnnotationClassId.toKtClassSymbol() ?: return@flatMap listOf(container)
            if (containedClass.isRepeatableAnnotation(container)) {
                containedAnnotations
            } else {
                listOf(container)
            }
        }
    } else annotations
}

internal fun KaAnnotated.annotations(parent: KSNode? = null): Sequence<KSAnnotation> {
    return annotationsWithRepeatableUnfolded().asSequence().map { KSAnnotationResolvedImpl.getCached(it, parent) }
}

internal fun KtAnnotated.annotations(
    kaAnnotated: KaAnnotated,
    parent: KSNode? = null,
    candidates: List<KaAnnotation> = kaAnnotated.annotationsWithRepeatableUnfolded()
): Sequence<KSAnnotation> {
    if (candidates.isEmpty())
        return emptySequence()
    return annotationEntries.filter { !it.isUseSiteTargetAnnotation() }.asSequence().map { annotationEntry ->
        KSAnnotationImpl.getCached(annotationEntry, parent) {
            candidates.single { it.psi == annotationEntry }
        }
    }
}

internal fun KaSymbol.getContainingKSSymbol(): KSDeclaration? {
    return analyze {
        when (val containingSymbol = this@getContainingKSSymbol.containingSymbol) {
            is KaNamedClassSymbol -> KSClassDeclarationImpl.getCached(containingSymbol)
            is KaFunctionSymbol -> KSFunctionDeclarationImpl.getCached(containingSymbol)
            is KaPropertySymbol -> KSPropertyDeclarationImpl.getCached(containingSymbol)
            else -> null
        }
    }
}

internal fun KaSymbol.toKSDeclaration(): KSDeclaration? = this.toKSNode() as? KSDeclaration

internal fun KaSymbol.toKSNode(): KSNode {
    return when (this) {
        is KaPropertySymbol -> KSPropertyDeclarationImpl.getCached(this)
        is KaNamedClassSymbol -> KSClassDeclarationImpl.getCached(this)
        is KaFunctionSymbol -> KSFunctionDeclarationImpl.getCached(this)
        is KaTypeAliasSymbol -> KSTypeAliasImpl.getCached(this)
        is KaJavaFieldSymbol -> KSPropertyDeclarationJavaImpl.getCached(this)
        is KaFileSymbol -> KSFileImpl.getCached(this)
        is KaEnumEntrySymbol -> KSClassDeclarationEnumEntryImpl.getCached(this)
        is KaTypeParameterSymbol -> KSTypeParameterImpl.getCached(this)
        is KaLocalVariableSymbol -> KSPropertyDeclarationLocalVariableImpl.getCached(this)
        else -> throw IllegalStateException("Unexpected class for KtSymbol: ${this.javaClass}")
    }
}

internal fun ClassId.toKtClassSymbol(): KaClassSymbol? {
    return analyze {
        if (this@toKtClassSymbol.isLocal) {
            this@toKtClassSymbol.outerClassId?.toKtClassSymbol()?.declaredMemberScope?.classifiers {
                it.asString() == this@toKtClassSymbol.shortClassName.asString()
            }?.singleOrNull() as? KaClassSymbol
        } else {
            // Try to find KaFirPsiJavaClassSymbol. See KT-74598 for details.
            val candidate = findClass(this@toKtClassSymbol)
            val psi = candidate?.psi
            // Checking of JAVA_SOURCE is needed because psi can come from libraries.
            if (candidate?.origin == KaSymbolOrigin.JAVA_SOURCE && psi is PsiClass)
                psi.namedClassSymbol
            else
                candidate
        }
    }
}

internal fun ClassId.toTypeAlias(): KaTypeAliasSymbol? {
    return analyze {
        findTypeAlias(this@toTypeAlias)
    }
}

internal fun KaType.classifierSymbol(): KaClassifierSymbol? {
    return try {
        when (this) {
            is KaTypeParameterType -> this.symbol
            // TODO: upstream is not exposing enough information for captured types.
            is KaCapturedType -> TODO("fix in upstream")
            is KaClassErrorType, is KaErrorType -> null
            is KaFunctionType -> (this as? KaFirFunctionType)?.abbreviatedSymbol() ?: symbol
            is KaUsualClassType -> symbol
            is KaDefinitelyNotNullType -> original.classifierSymbol()
            is KaDynamicType -> null
            // flexible types have 2 bounds, using lower bound for a safer approximation.
            // TODO: find a case where lower bound is not appropriate.
            is KaFlexibleType -> lowerBound.classifierSymbol()
            // TODO: KSP does not support intersection type.
            is KaIntersectionType -> null
            else -> throw IllegalStateException("Unexpected type ${this.javaClass}")
        }
    } catch (e: KotlinExceptionWithAttachments) {
        // The implementation for getting symbols from a type throws an exception
        // when it can't find the corresponding class symbol fot the given class ID.
        null
    }
}

internal fun KaType.typeArguments(): List<KaTypeProjection> {
    return if (this is KaFlexibleType) {
        this.lowerBound
    } else {
        this
    }.let {
        (it as? KaClassType)?.qualifiers?.reversed()?.flatMap(KaResolvedClassTypeQualifier::typeArguments)
            ?: emptyList()
    }
}

internal fun KSAnnotated.findAnnotationFromUseSiteTarget(): Sequence<KSAnnotation> {
    return when (this) {
        is KSPropertyGetter -> (this.receiver as? AbstractKSDeclarationImpl)?.let { decl ->
            decl.originalAnnotations.filter { it.useSiteTarget == AnnotationUseSiteTarget.GET }
        }

        is KSPropertySetter -> (this.receiver as? AbstractKSDeclarationImpl)?.let { decl ->
            decl.originalAnnotations.filter { it.useSiteTarget == AnnotationUseSiteTarget.SET }
        }

        is KSValueParameter -> {
            var parent = this.parent
            // TODO: eliminate annotationsFromParents to make this fully sequence.
            val annotationsFromParents = mutableListOf<KSAnnotation>()
            (parent as? KSPropertyAccessorImpl)?.let { propertyAccessor ->
                annotationsFromParents.addAll(
                    propertyAccessor.originalAnnotations
                        .filter { it.useSiteTarget == AnnotationUseSiteTarget.SETPARAM }
                )
                parent = (parent as KSPropertyAccessorImpl).receiver
            }
            (parent as? KSPropertyDeclarationImpl)?.let { propertyDeclaration ->
                annotationsFromParents.addAll(
                    propertyDeclaration.originalAnnotations
                        .filter { it.useSiteTarget == AnnotationUseSiteTarget.SETPARAM }
                )
            }
            annotationsFromParents.asSequence()
        }

        else -> emptySequence()
    } ?: emptySequence()
}

internal fun KaSymbolVisibility.toModifier(): Modifier {
    return when (this) {
        KaSymbolVisibility.PUBLIC -> Modifier.PUBLIC
        KaSymbolVisibility.PRIVATE -> Modifier.PRIVATE
        KaSymbolVisibility.INTERNAL -> Modifier.INTERNAL
        KaSymbolVisibility.PROTECTED, KaSymbolVisibility.PACKAGE_PROTECTED ->
            Modifier.PROTECTED

        else -> Modifier.PUBLIC
    }
}

internal fun KaSymbolModality.toModifier(): Modifier {
    return when (this) {
        KaSymbolModality.FINAL -> Modifier.FINAL
        KaSymbolModality.ABSTRACT -> Modifier.ABSTRACT
        KaSymbolModality.OPEN -> Modifier.OPEN
        KaSymbolModality.SEALED -> Modifier.SEALED
    }
}

internal inline fun <reified T : KSNode> KSNode.findParentOfType(): KSNode? {
    var result = parent
    while (!(result == null || result is T)) {
        result = result.parent
    }
    return result
}

internal fun KaAnnotationValue.toValue(): Any? = when (this) {
    is KaAnnotationValue.ArrayValue -> this.values.map { it.toValue() }
    is KaAnnotationValue.NestedAnnotationValue -> KSAnnotationResolvedImpl.getCached(this.annotation)
    // TODO: Enum entry should return a type, use declaration as a placeholder.
    is KaAnnotationValue.EnumEntryValue -> this.callableId?.classId?.let { classId ->
        analyze {
            classId.toKtClassSymbol()?.let { classSymbol ->
                classSymbol.declarations().filterIsInstance<KSClassDeclarationEnumEntryImpl>().singleOrNull {
                    it.simpleName.asString() == this@toValue.callableId?.callableName?.asString()
                }
            }
        }
    } ?: KSErrorType

    is KaAnnotationValue.ClassLiteralValue -> {
        KSTypeImpl.getCached(this@toValue.type)
    }

    is KaAnnotationValue.ConstantValue -> this.value.value
    is KaAnnotationValue.UnsupportedValue -> null
}

@OptIn(SymbolInternals::class, KaImplementationDetail::class, KaExperimentalApi::class)
internal fun KaValueParameterSymbol.getDefaultValue(): KaAnnotationValue? {
    return this.psi.let { psiElement ->
        when (psiElement) {
            is KtParameter -> analyze {
                psiElement.defaultValue?.evaluateAsAnnotationValue()
            }
            // ClsMethodImpl means the psi is decompiled psi.
            null, is ClsMemberImpl<*> -> {
                // TODO: multiplatform
                if (!ResolverAAImpl.instance.isJvm)
                    return@let null
                val fileManager = ResolverAAImpl.instance.javaFileManager
                val parentClass = this.getContainingKSSymbol()!!.findParentOfType<KSClassDeclaration>()
                val classId = (parentClass as KSClassDeclarationImpl).ktClassOrObjectSymbol.classId
                    ?: return@let null

                val defaultValue: JavaAnnotationArgument? = analyze {
                    val jc = fileManager.findClass(classId, analysisScope) ?: return@analyze null
                    jc.methods.firstOrNull { it.name == name }?.annotationParameterDefaultValue
                }

                (this as? KaFirValueParameterSymbol)?.let {
                    val firSession = it.firSymbol.fir.moduleData.session
                    val symbolBuilder = it.builder
                    val expectedTypeRef = it.firSymbol.fir.returnTypeRef
                    // when no default value is declared in the class file, ideally users should
                    // apply a value for such property at use site, therefore value obtained here should not be
                    // returned. In case of a user failed to do so, we try our best to return values
                    // to ensure no annotation argument is missing from KSP side.
                    // Supplying `JavaUnknownAnnotationArgumentImpl` as the expression base
                    // will produce empty array for array type values and `null` for the rest of value types.
                    val expression = (defaultValue ?: JavaUnknownAnnotationArgumentImpl(null))
                        .toFirExpression(firSession, JavaTypeParameterStack.EMPTY, expectedTypeRef, null)
                    FirAnnotationValueConverter.toConstantValue(expression, symbolBuilder)
                }
            }

            else -> throw IllegalStateException("Unhandled default value type ${psiElement.javaClass}")
        }
    }
}

@OptIn(KaExperimentalApi::class)
internal fun fillInDeepSubstitutor(context: KaType, substitutorBuilder: KaSubstitutorBuilder) {
    val unwrappedType = when (context) {
        is KaClassType -> context
        is KaFlexibleType -> {
            fillInDeepSubstitutor(context.upperBound, substitutorBuilder)
            fillInDeepSubstitutor(context.lowerBound, substitutorBuilder)
            return
        }
        else -> return
    }
    val parameters = unwrappedType.symbol.typeParameters
    val arguments = unwrappedType.typeArguments
    if (parameters.size != arguments.size) {
        throw IllegalStateException("invalid substitution for $context")
    }
    parameters.zip(arguments).forEach { (param, projection) ->
        val arg = projection.type ?: param.upperBounds.firstOrNull() ?: analyze { useSiteSession.builtinTypes.any }
        substitutorBuilder.substitution(param, arg)
    }
    (context.symbol as? KaClassSymbol)?.superTypes?.forEach {
        fillInDeepSubstitutor(it, substitutorBuilder)
    }
}

internal fun KaSymbol.psiIfSource(): PsiElement? {
    return if (origin == KaSymbolOrigin.SOURCE || origin == KaSymbolOrigin.JAVA_SOURCE && toContainingFile() != null) {
        psi
    } else {
        null
    }
}

fun interface Restorable {
    fun restore(): KSAnnotated?
}

fun interface Deferrable {
    fun defer(): Restorable?
}

fun <T : KaSymbol> T.defer(restore: (T) -> KSAnnotated?): Restorable {
    val ptr = analyze { this@defer.createPointer() }
    return Restorable {
        analyze {
            val restored = ptr.restoreSymbol() ?: return@analyze null
            @Suppress("UNCHECKED_CAST")
            restore(restored as T)
        }
    }
}

fun ClassId.toKSName() = KSNameImpl.getCached(asSingleFqName().toString())

// Only need to map java types if type declaration has origin of java
// or kotlin functional type (in the case of Java functional type).
internal fun KaClassLikeSymbol.shouldMapToKotlinForAssignabilityCheck(): Boolean {
    return this.origin == KaSymbolOrigin.JAVA_SOURCE ||
        this.origin == KaSymbolOrigin.JAVA_LIBRARY ||
        this.classId?.packageFqName?.asString() == "kotlin.jvm.functions"
}

// recursively replace type & type argument to map java types into kotlin types.
internal fun KaType.convertToKotlinType(): KaType {
    if (this !is KaClassType) {
        return this
    }
    val declaration = this.classifierSymbol() as? KaClassLikeSymbol ?: return this
    if (declaration.classId == null) {
        return this
    }
    val base = if (declaration.classId != null && declaration.shouldMapToKotlinForAssignabilityCheck()) {
        JavaToKotlinClassMap.mapJavaToKotlin(declaration.classId!!.asSingleFqName())?.toKtClassSymbol()
            ?: declaration
    } else declaration
    return analyze {
        buildClassType(base.tryResolveToTypePhase()) {
            this@convertToKotlinType.typeArguments().forEach { typeProjection ->
                if (typeProjection is KaTypeArgumentWithVariance) {
                    argument(typeProjection.type.convertToKotlinType(), typeProjection.variance)
                } else {
                    argument(typeProjection)
                }
            }
            nullability = this@convertToKotlinType.nullability
        }
    }
}

internal fun KaType.isAssignableFrom(that: KaType): Boolean {
    return if (that is KaFlexibleType) {
        this.isAssignableFrom(that.upperBound) || this.isAssignableFrom(that.lowerBound)
    } else {
        analyze {
            this@isAssignableFrom.convertToKotlinType()
            that.convertToKotlinType().isSubtypeOf(this@isAssignableFrom.convertToKotlinType())
        }
    }
}

// TODO: fix flexible type creation once upstream available.
internal fun KaType.replace(newArgs: List<KaTypeProjection>): KaType {
    require(newArgs.isEmpty() || newArgs.size == this.typeArguments().size)
    return analyze {
        when (val symbol = classifierSymbol().tryResolveToTypePhase()) {
            is KaClassLikeSymbol -> useSiteSession.buildClassType(symbol) {
                newArgs.forEach { arg -> argument(arg) }
                nullability = this@replace.nullability
            }
            // No need to copy nullability for type parameters
            // because it is overridden to be always nullable in compiler.
            is KaTypeParameterSymbol -> useSiteSession.buildTypeParameterType(symbol)
            else -> throw IllegalStateException("Unexpected type ${this@replace}")
        }
    }
}

internal fun getVarianceForWildcard(
    parameter: KaTypeParameterSymbol,
    projection: KaTypeProjection,
    mode: TypeMappingMode
): Variance {
    val projectionKind = if (projection is KaTypeArgumentWithVariance) {
        projection.variance
    } else {
        Variance.OUT_VARIANCE
    }
    val parameterVariance = parameter.variance
    if (parameterVariance == Variance.INVARIANT) {
        return projectionKind
    }
    if (mode.skipDeclarationSiteWildcards) {
        return Variance.INVARIANT
    }
    if (projectionKind == Variance.INVARIANT || projectionKind == parameterVariance) {
        if (mode.skipDeclarationSiteWildcardsIfPossible && projection !is KaStarTypeProjection) {
            val type = projection.type ?: return parameterVariance
            if (parameterVariance == Variance.OUT_VARIANCE && type.isMostPreciseCovariantArgument()) {
                return Variance.INVARIANT
            }
            if (parameterVariance == Variance.IN_VARIANCE && type.isMostPreciseContravariantArgument()) {
                return Variance.INVARIANT
            }
        }
        return parameterVariance
    }
    return Variance.OUT_VARIANCE
}

internal fun KaType.isMostPreciseContravariantArgument(): Boolean = analyze { isAnyType }

internal fun KaType.isMostPreciseCovariantArgument() = !canHaveSubtypesIgnoreNullability()

@OptIn(KaExperimentalApi::class)
private fun KaType.canHaveSubtypesIgnoreNullability(): Boolean =
    analyze {
        val type = fullyExpandedType
        val symbol = expandedSymbol ?: return@analyze true
        if (symbol.classKind == KaClassKind.ENUM_CLASS || symbol.isExpect) {
            return@analyze true
        }
        if (symbol.modality != KaSymbolModality.FINAL) {
            return@analyze true
        }

        symbol.typeParameters.forEachIndexed { idx, param ->
            val projection = type.typeArguments().get(idx)

            if (projection !is KaTypeArgumentWithVariance) {
                return@analyze true
            }

            val type = projection.type
            val effectiveVariance = getEffectiveVariance(param.variance, projection.variance)
            if (effectiveVariance == Variance.OUT_VARIANCE && !type.isMostPreciseCovariantArgument()) {
                return@analyze true
            }
            if (effectiveVariance == Variance.IN_VARIANCE && !type.isMostPreciseContravariantArgument()) {
                return@analyze true
            }
        }
        false
    }

@OptIn(KaExperimentalApi::class)
internal fun KaType.toWildcard(mode: TypeMappingMode): KaType {
    val args = this.typeArguments()
    return analyze {
        when (this@toWildcard) {
            is KaClassType -> {
                // TODO: missing annotations from original type.
                buildClassType(symbol.tryResolveToTypePhase()) {
                    val parameters = symbol.typeParameters
                    parameters.zip(args).map { (param, arg) ->
                        val argMode = mode.updateFromAnnotations(arg.type)
                        val variance = getVarianceForWildcard(param, arg, argMode)
                        val genericMode = argMode.toGenericArgumentMode(
                            getEffectiveVariance(
                                param.variance,
                                (arg as? KaTypeArgumentWithVariance)?.variance ?: Variance.INVARIANT
                            )
                        )
                        val argType =
                            arg.type ?: useSiteSession.builtinTypes.any.withNullability(KaTypeNullability.NULLABLE)
                        argument(argType.toWildcard(genericMode), variance)
                    }
                    nullability = this@toWildcard.nullability
                }
            }

            is KaTypeParameterType -> {
                buildTypeParameterType(this@toWildcard.symbol.tryResolveToTypePhase())
            }

            else -> throw IllegalStateException("Unexpected type ${this@toWildcard}")
        }
    }
}

internal fun TypeMappingMode.suppressJvmWildcards(
    suppress: Boolean
): TypeMappingMode {
    return TypeMappingMode.createWithConstantDeclarationSiteWildcardsMode(
        skipDeclarationSiteWildcards = suppress,
        isForAnnotationParameter = isForAnnotationParameter,
        needInlineClassWrapping = needInlineClassWrapping,
        mapTypeAliases = mapTypeAliases
    )
}

internal fun TypeMappingMode.updateFromParents(
    ref: KSTypeReference
): TypeMappingMode {
    return ref.findJvmSuppressWildcards()?.let {
        this.suppressJvmWildcards(it)
    } ?: this
}

internal fun KSTypeReference.findJvmSuppressWildcards(): Boolean? {
    var candidate: KSNode? = this
    while (candidate != null) {
        if ((candidate is KSTypeReference || candidate is KSDeclaration)) {
            (candidate as KSAnnotated).annotations.singleOrNull {
                it.annotationType.resolve().declaration.qualifiedName?.asString()?.equals(
                    JVM_SUPPRESS_WILDCARDS_ANNOTATION_FQ_NAME.asString()
                ) == true
            }?.let { a -> a.arguments.singleOrNull { it.name?.asString() == "suppress" } }?.let {
                return it.value as? Boolean
            }
        }
        candidate = candidate.parent
    }
    return null
}

internal fun TypeMappingMode.updateFromAnnotations(
    type: KaType?
): TypeMappingMode {
    if (type == null) {
        return this
    }
    type.annotations().firstOrNull {
        it.annotationType.resolve().declaration.qualifiedName?.asString()
            ?.equals(JVM_SUPPRESS_WILDCARDS_ANNOTATION_FQ_NAME.asString()) == true
    }?.let { annotation ->
        annotation.arguments.firstOrNull { it.name?.asString() == "suppress" }?.let {
            return (it.value as? Boolean)?.let(::suppressJvmWildcards) ?: this
        }
    }
    return if (type.annotations().any {
        it.annotationType.resolve().declaration.qualifiedName?.asString()
            ?.equals(JVM_WILDCARD_ANNOTATION_FQ_NAME.asString()) == true
    }
    ) {
        TypeMappingMode.createWithConstantDeclarationSiteWildcardsMode(
            skipDeclarationSiteWildcards = false,
            isForAnnotationParameter = isForAnnotationParameter,
            fallbackMode = this,
            needInlineClassWrapping = needInlineClassWrapping,
            mapTypeAliases = mapTypeAliases
        )
    } else {
        this
    }
}

internal fun KaFunctionType.abbreviatedSymbol(): KaTypeAliasSymbol? {
    val classId = (this as? KaFirFunctionType)?.coneType?.abbreviatedType?.classId ?: return null
    return classId.toTypeAlias()
}

fun <T : KaSymbol?> T.tryResolveToTypePhase(): T {
    (this as? KaFirSymbol<*>)?.firSymbol?.lazyResolveToPhase(FirResolvePhase.TYPES)
    return this
}

private val KaType.upperBoundIfFlexible: KaType
    get() = (this as? KaFlexibleType)?.upperBound ?: this

private fun KaType.requiresMangling(): Boolean = (symbol as? KaNamedClassSymbol)?.isInline ?: false

private fun KaType.asInfoForMangling(): InfoForMangling? {
    val upperBound = upperBoundIfFlexible
    val fqName = upperBound.symbol?.classId?.asSingleFqName()?.toUnsafe() ?: return null
    val isValue = upperBound.requiresMangling()
    val isNullable = upperBound.nullability.isNullable
    return InfoForMangling(fqName = fqName, isValue = isValue, isNullable = isNullable)
}

private fun mangleInlineSuffix(
    parameters: List<KaType>,
    returnType: KaType?,
    shouldMangleReturnType: Boolean
): String {
    val signature = collectFunctionSignatureForManglingSuffix(
        false,
        parameters.any { it.requiresMangling() },
        parameters.map { it.asInfoForMangling() },
        if (shouldMangleReturnType) returnType?.asInfoForMangling() else null
    ) ?: return ""
    return "-${md5base64(signature)}"
}

private val KaSymbol.isKotlin: Boolean
    get() = when (origin) {
        KaSymbolOrigin.SOURCE, KaSymbolOrigin.LIBRARY -> true
        else -> false
    }

internal val KaFunctionSymbol.inlineSuffix: String
    get() = mangleInlineSuffix(
        valueParameters.map { it.returnType },
        returnType,
        analyze {
            returnType.requiresMangling() && isKotlin && containingDeclaration != null
        }
    )

internal val KaPropertyAccessorSymbol.inlineSuffix: String
    get() = when (this) {
        is KaPropertyGetterSymbol ->
            mangleInlineSuffix(
                emptyList(),
                returnType,
                analyze {
                    returnType.requiresMangling() && isKotlin && containingDeclaration?.containingDeclaration != null
                }
            )
        is KaPropertySetterSymbol -> mangleInlineSuffix(listOf(parameter.returnType), null, false)
    }

private val jvmNameClassId = ClassId.fromString("kotlin/jvm/JvmName")
internal fun KaCallableSymbol.explictJvmName(): String? {
    return annotations.singleOrNull() {
        it.classId == jvmNameClassId
    }?.arguments?.single()?.expression?.toValue() as? String
}

@OptIn(SymbolInternals::class)
internal val KaDeclarationSymbol.internalSuffix: String
    get() = analyze {
        if (visibility != KaSymbolVisibility.INTERNAL)
            return@analyze ""

        // Skip top level functions and properties
        when (this@internalSuffix) {
            is KaPropertyAccessorSymbol -> {
                if (containingDeclaration?.containingDeclaration == null)
                    return@analyze ""
            }
            is KaFunctionSymbol -> {
                if (containingDeclaration == null)
                    return@analyze ""
            }
            else -> {}
        }

        fun String.toSuffix(): String = "\$$this".replace('.', '_').replace('-', '_')
        when (val module = containingModule) {
            is KaSourceModule -> module.name.toSuffix()
            is KaLibraryModule -> {
                // Read module name from metadata.
                // FIXME: need an API in AA.
                val firSymbol = (this@internalSuffix as? KaFirSymbol<*>)?.firSymbol
                val firClassSymbol = firSymbol?.getContainingClassSymbol()
                val moduleName = (firClassSymbol?.fir as? FirRegularClass)?.moduleName
                (moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME).toSuffix()
            }
            else -> ""
        }
    }
