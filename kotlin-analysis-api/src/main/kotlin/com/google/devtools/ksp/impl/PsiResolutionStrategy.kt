/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package com.google.devtools.ksp.impl

import com.google.devtools.ksp.common.mergeMapNotNullKeys
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.impl.symbol.kotlin.KSClassDeclarationImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSFileImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSFunctionDeclarationImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSValueParameterImpl
import com.google.devtools.ksp.impl.symbol.kotlin.Restorable
import com.google.devtools.ksp.impl.symbol.kotlin.analyze
import com.google.devtools.ksp.impl.symbol.kotlin.getFqn
import com.google.devtools.ksp.impl.symbol.kotlin.getGeneratedProperty
import com.google.devtools.ksp.impl.symbol.kotlin.getter
import com.google.devtools.ksp.impl.symbol.kotlin.psi
import com.google.devtools.ksp.impl.symbol.kotlin.setter
import com.google.devtools.ksp.impl.symbol.kotlin.toKSAnnotated
import com.google.devtools.ksp.impl.symbol.kotlin.toKSAnnotationUseSiteTarget
import com.google.devtools.ksp.impl.symbol.kotlin.toKSClassDeclaration
import com.google.devtools.ksp.impl.symbol.kotlin.toKSFile
import com.google.devtools.ksp.impl.symbol.kotlin.toKSFunctionDeclaration
import com.google.devtools.ksp.impl.symbol.kotlin.toKSPropertyDeclaration
import com.google.devtools.ksp.impl.symbol.kotlin.toLocation
import com.google.devtools.ksp.impl.visitor.CollectAnnotatedSymbolsPsiVisitor
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSValueParameter
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.PsiTypeParameterList
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.parameterIndex
import org.jetbrains.kotlin.utils.addToStdlib.flatGroupBy

/**
 * An [AnnotationResolutionStrategy] that uses a combination of Psi and Kotlin's Analysis API to resolve
 * annotated symbols.
 *
 * This strategy is experimental, but should be faster than the default [AAResolutionStrategy].
 */
class PsiResolutionStrategy(
    override val newKSFiles: List<KSFile>,
    override val deferredSymbols: Map<SymbolProcessor, List<Restorable>>
) : AnnotationResolutionStrategy {

    /**
     * Returns all symbols annotated with [annotationName].
     */
    override fun getSymbolsWithAnnotation(annotationName: String, inDepth: Boolean): Sequence<KSAnnotated> =
        if (inDepth)
            aaResolutionStrategy.getSymbolsWithAnnotation(annotationName, inDepth = true)
        else
            getAnnotatedSymbols(annotationName)

    /**
     * Calls to [getSymbolsWithAnnotation] with `inDepth = true` are
     * delegated to the [AAResolutionStrategy].
     * It is stored in a lazy val to ensure it is only initialized if
     * needed and to cache its results.
     */
    private val aaResolutionStrategy: AAResolutionStrategy by lazy {
        AAResolutionStrategy(newKSFiles, deferredSymbols)
    }

    /**
     * Returns the symbols annotated with the fully qualified name [annotationName].
     */
    private fun getAnnotatedSymbols(annotationName: String): Sequence<KSAnnotated> =
        getAnnotatedJavaSymbols(annotationName) +
            getAnnotatedKotlinSymbols(annotationName) +
            getRestoredSymbols(annotationName)

    /**
     * Returns the Kotlin symbols annotated with the fully qualified name [annotationName].
     */
    private fun getAnnotatedKotlinSymbols(annotationName: String): Sequence<KSAnnotated> {
        val newKotlinSymbols = annotatedKotlinElementsByFullyQualifiedName[annotationName]?.value ?: emptyList()
        return newKotlinSymbols.asSequence()
    }

    /**
     * Returns the Java symbols annotated with the fully qualified name [annotationName].
     */
    private fun getAnnotatedJavaSymbols(annotationName: String): Sequence<KSAnnotated> =
        annotatedJavaElementsByFullyQualifiedName.getOrPut(annotationName) {
            resolveJavaAnnotationByShortName(annotationName)
        }.asSequence()

    /**
     * Returns the [deferredSymbols] that are annotated with [annotationName].
     */
    private fun getRestoredSymbols(annotationName: String): Sequence<KSAnnotated> =
        deferredSymbolsGroupedByAnnotations[annotationName]?.asSequence() ?: emptySequence()

    /**
     * [deferredSymbols] grouped by their fully qualified annotation names.
     *
     * See [annotatedKotlinElementsByFullyQualifiedName] for an example of the grouping.
     */
    private val deferredSymbolsGroupedByAnnotations: Map<String, Collection<KSAnnotated>> by lazy {
        deferredSymbols.values.flatten().mapNotNull { it.restore() }.distinct()
            .flatGroupBy { sym -> sym.annotations.mapNotNull { it.getFqn() }.toSet() }
    }

    /**
     * Returns the Java symbols annotated with the fully qualified name [annotationName].
     * The function only considers the subset of annotations in the program that share the same
     * short name / unqualified name.
     */
    private fun resolveJavaAnnotationByShortName(annotationName: String): Collection<KSAnnotated> {
        val annotationShortName = annotationName.substringAfterLast('.')
        return annotatedJavaElementsByShortName[annotationShortName]?.filter { element ->
            val annotationsForElement = fullyQualifiedJavaAnnotationNamesByElements[element] ?: emptyMap()
            annotationsForElement[annotationShortName]?.value?.contains(annotationName) ?: false
        }?.flatMap {
            it.resolveToKSAnnotated()
        } ?: emptyList()
    }

    /**
     * All annotated [PsiElement]s in [newKSFiles] from both Kotlin and Java sources.
     */
    private val annotatedPsiElements: Collection<PsiElement> by lazy {
        newKSFiles.flatMap { collectAnnotatedPsiElementsIn(it) }
    }

    /**
     * Returns all [PsiElement]s that are annotated in [file] except for declarations
     * in function bodies or property accessors.
     */
    private fun collectAnnotatedPsiElementsIn(file: KSFile): Collection<PsiElement> {
        val visitor = CollectAnnotatedSymbolsPsiVisitor()
        file.psi?.accept(visitor)
        return visitor.result
    }

    /**
     * All annotated Java elements.
     */
    private val annotatedJavaElements: List<PsiModifierListOwner> by lazy {
        annotatedPsiElements.filterIsInstance<PsiModifierListOwner>()
    }

    /**
     * Groups [annotatedJavaElements] by the short names of their annotations.
     */
    private val annotatedJavaElementsByShortName: Map<String, Collection<PsiElement>> by lazy {
        annotatedJavaElements.flatGroupBy { element ->
            // N.B.: deduplicate by converting to a set, since
            // otherwise it will add `element` more than once to the list of values
            element.annotations.map { it.shortName }.toSet()
        }
    }

    /**
     * A map from Java elements to their annotations' fully qualified names, indexed by short name.
     */
    private val fullyQualifiedJavaAnnotationNamesByElements: Map<PsiElement, Map<String, Lazy<Set<String>>>> by lazy {
        annotatedJavaElements.associate { element ->
            // N.B.: Performance: Lazily compute the set of qualified names to avoid resolution until requested
            //  for a specific annotation.
            element to buildMap<String, Lazy<MutableSet<String>>> {
                element.annotations.forEach { anno ->
                    val lazyFullyQualifiedNames = this[anno.shortName]
                    if (lazyFullyQualifiedNames == null) {
                        this[anno.shortName] = lazy {
                            mutableSetOf(
                                anno.qualifiedName
                                    ?: error("Unexpected unqualified name at ${anno.toLocation()}: ${anno.javaClass}")
                            )
                        }
                    } else {
                        this[anno.shortName] = lazy {
                            lazyFullyQualifiedNames.value.add(
                                anno.qualifiedName
                                    ?: error("Unexpected unqualified name at ${anno.toLocation()}: ${anno.javaClass}")
                            )
                            lazyFullyQualifiedNames.value
                        }
                    }
                }
            }
        }
    }

    /**
     * Groups all annotated Java symbols by their fully qualified annotation names.
     *
     * @see [annotatedKotlinElementsByFullyQualifiedName] for further explanation.
     */
    private val annotatedJavaElementsByFullyQualifiedName: MutableMap<String, Collection<KSAnnotated>> =
        mutableMapOf()

    /**
     * All annotated Kotlin elements.
     */
    private val annotatedKotlinElements: List<KtAnnotated> by lazy {
        annotatedPsiElements.filterIsInstance<KtAnnotated>()
    }

    /**
     * Groups all annotated Kotlin symbols by their fully qualified annotation names.
     *
     * In other words, if `fun1, fun2, fun3` are annotated as follows:
     * ```
     * @Annotation1 fun fun1
     * @Annotation1 @Annotation2 fun fun2
     * @Annotation2 @Annotation3 fun fun3
     * ```
     * Then the result is
     *
     * ```
     * "Annotation1" -> [fun1, fun2]
     * "Annotation2" -> [fun2, fun3]
     * "Annotation3" -> [fun3]
     * ```
     */
    private val annotatedKotlinElementsByFullyQualifiedName: Map<String, Lazy<Collection<KSAnnotated>>> by lazy {
        annotatedKotlinElements
            .flatGroupBy { element -> element.annotationEntries }
            .mapValues { entry ->
                lazy { entry.value.flatMap { it.resolveToKSAnnotated(entry.key) } }
            }
            // N.B.: Skip nullable qualified names without error. This is what the AA-based implementation does.
            //   For now, this is the intended behavior. If it is changed in the future, then it is an API change.
            .mergeMapNotNullKeys { it.qualifiedName }
    }

    /**
     * Resolves this [PsiElement] to the set of [KSAnnotated] symbols targeted by [annotation].
     *
     * The [annotation] is only required for Kotlin sources since annotations may have use site targets.
     * Java sources never require an [annotation] to be present since annotations directly target the element
     * being annotated.
     */
    private fun PsiElement.resolveToKSAnnotated(annotation: KtAnnotationEntry? = null): Collection<KSAnnotated> =
        when (val element = this@resolveToKSAnnotated) {
            // Kotlin sources
            is KtDeclaration -> {
                if (annotation == null) {
                    error("Unexpected null annotation at ${toLocation()} : $javaClass")
                }
                element.resolve(annotation)
            }

            is KtFile ->
                listOf(element.resolve())

            is KtTypeReference ->
                emptyList() // Do nothing

            // Java sources
            is PsiParameter ->
                listOf(element.resolve())

            is PsiTypeParameter ->
                element.resolve()

            is PsiClass ->
                listOf(element.resolve())

            is PsiField ->
                listOf(element.resolve())

            is PsiMethod ->
                listOf(element.resolve())

            else ->
                error("Unreachable: ${element.javaClass}")
        }

    /**
     * Resolves this [KtDeclaration] to the set of [KSAnnotated] symbols targeted by [annotationEntry].
     */
    private fun KtDeclaration.resolve(annotationEntry: KtAnnotationEntry): Collection<KSAnnotated> {
        // TODO: This should perform case distinction instead of getTargetedSymbol
        val ksSym = analyze { symbol.toKSAnnotated() }
        return ksSym.getTargetedSymbol(annotationEntry.ksUseSiteTarget)
    }

    /**
     * Resolves the [KSFileImpl] corresponding to this [KtFile].
     */
    private fun KtFile.resolve(): KSFileImpl =
        analyze { symbol }.toKSFile()

    /**
     * Resolves the [KSValueParameter] corresponding to this [PsiParameter].
     */
    private fun PsiParameter.resolve(): KSValueParameter {
        val functionDecl = callableSymbol.toKSFunctionDeclaration()
            ?: error(
                "Failed to convert callable symbol to KSFunctionDeclaration at " +
                    "${toLocation()}: " +
                    "${callableSymbol.javaClass}"
            )
        return functionDecl.parameters[parameterIndex()]
    }

    /**
     * Resolves the [KSTypeParameter]s corresponding to this [PsiTypeParameter].
     */
    private fun PsiTypeParameter.resolve(): Collection<KSTypeParameter> = when (val decl = parent.parent) {
        is PsiMethod ->
            listOf(resolveTypeParameterOfMethod(decl))

        is PsiClass ->
            resolveTypeParameterOfClass(decl)

        else ->
            error("Unexpected Java declaration at ${decl.toLocation()}: ${decl.javaClass}")
    }

    /**
     * Resolves the [KSClassDeclarationImpl] corresponding to this [PsiClass].
     */
    private fun PsiClass.resolve(): KSClassDeclarationImpl {
        val sym = analyze { namedClassSymbol }
            ?: error("Unexpected null named class symbol at ${toLocation()}: $javaClass")
        return sym.toKSClassDeclaration()
    }

    /**
     * Resolves the [KSPropertyDeclaration] corresponding to this [PsiField].
     */
    private fun PsiField.resolve(): KSPropertyDeclaration {
        val sym = analyze { this@resolve.callableSymbol }
            ?: error("Unexpected null callable symbol at ${toLocation()}: $javaClass")
        return sym.toKSPropertyDeclaration()
            ?: error("Unexpected null KSPropertyDeclaration at ${toLocation()}: $javaClass")
    }

    /**
     * Resolves the [KSFunctionDeclarationImpl] corresponding to this [PsiMethod].
     */
    private fun PsiMethod.resolve(): KSFunctionDeclarationImpl {
        val sym = analyze { this@resolve.callableSymbol }
            ?: error("Unexpected null callable symbol at ${toLocation()}: $javaClass")
        return sym.toKSFunctionDeclaration()
            ?: error("Unexpected null KSFunctionDeclaration at ${toLocation()}: $javaClass")
    }

    /**
     * Returns the targeted symbols by a given annotated symbol at its use site target.
     *
     * E.g., `class A(@MyAnno val p: Int)` returns `p, p.getter`
     */
    private fun KSAnnotated.getTargetedSymbol(useSiteTarget: AnnotationUseSiteTarget?): Collection<KSAnnotated> =
        // TODO: Rewrite this call chain as double dispatch visitor.
        when (useSiteTarget) {
            null -> when (this) {
                is KSValueParameterImpl -> {
                    val generatedPropertySymbol = this.getGeneratedProperty()
                    if (generatedPropertySymbol != null) {
                        listOf(this, generatedPropertySymbol)
                    } else {
                        listOf(this)
                    }
                }

                is KSFunctionDeclarationImpl -> when {
                    this.isGetter -> listOf(
                        this.parentDeclaration?.getter()
                            ?: error("Missing getter $location: $javaClass")
                    )

                    this.isSetter -> listOf(
                        this.parentDeclaration?.setter()
                            ?: error("Missing setter $location: $javaClass")
                    )

                    else -> listOf(this)
                }

                else ->
                    listOf(this)
            }

            AnnotationUseSiteTarget.FILE ->
                listOf(
                    this.containingFile
                        ?: error("Missing file at $location: $javaClass")
                )

            AnnotationUseSiteTarget.PROPERTY ->
                listOf(this)

            AnnotationUseSiteTarget.FIELD ->
                listOf(this)

            AnnotationUseSiteTarget.GET ->
                listOf(
                    this.getter()
                        ?: error("Missing getter at $location: $javaClass")
                )

            AnnotationUseSiteTarget.SET ->
                listOf(
                    this.setter()
                        ?: error("Missing setter at $location: $javaClass")
                )

            AnnotationUseSiteTarget.RECEIVER -> when (this) {
                is KSFunctionDeclarationImpl -> listOf(
                    extensionReceiver
                        ?: error("Missing extension receiver at $location: $javaClass")
                )

                is KSPropertyDeclaration -> listOf(
                    extensionReceiver
                        ?: error("Missing extension receiver at $location: $javaClass")
                )

                else -> error("Unexpected declaration at $location: $javaClass")
            }

            AnnotationUseSiteTarget.PARAM ->
                listOf(this)

            AnnotationUseSiteTarget.SETPARAM ->
                // Return this, since it's the parameter that's directly annotated.
                listOf(this)

            AnnotationUseSiteTarget.DELEGATE ->
                listOf(this)

            AnnotationUseSiteTarget.ALL ->
                listOf(this) // FIXME: Correct impl
        }

    /**
     * Resolves this [PsiTypeParameter] to its [KSTypeParameter] representation within [method].
     */
    private fun PsiTypeParameter.resolveTypeParameterOfMethod(method: PsiMethod): KSTypeParameter {
        val callableSym = analyze { method.callableSymbol }
            ?: error("Unexpected null callable symbol at ${method.toLocation()}: ${method.javaClass}")
        val functionDecl = callableSym.toKSFunctionDeclaration()
            ?: error(
                "Failed to convert callable symbol to KSFunctionDeclaration at " +
                    "${method.toLocation()}: " +
                    "${callableSym.javaClass}"
            )
        return functionDecl.typeParameters[parameterIndex]
    }

    /**
     * Resolves this [PsiTypeParameter] to its [KSTypeParameter] representations within [clazz].
     */
    private fun PsiTypeParameter.resolveTypeParameterOfClass(clazz: PsiClass): List<KSTypeParameter> {
        val classSym = analyze { clazz.namedClassSymbol }
            ?: error("Unexpected named class symbol at ${clazz.toLocation()}: ${clazz.javaClass}")
        val typeParamSym = classSym.toKSClassDeclaration().typeParameters[parameterIndex]
        // Type parameters for classes return two symbols with Analysis API,
        // one as a Psi-based KaFirTypeParameter and one as a "regular" KaFirTypeParameter,
        // so we return a clone here to maintain KSP API parity.
        val clone = object : KSTypeParameter by typeParamSym {
            override fun toString(): String = typeParamSym.toString()
        }
        return listOf(typeParamSym, clone)
    }

    /**
     * Returns the short name used at the annotation site.
     * Throws an exception if the name is `null`.
     */
    private val PsiAnnotation.shortName: String
        get() = this.nameReferenceElement?.referenceName
            ?: error("Unexpected nullable annotation short name at ${toLocation()}: $javaClass")

    /**
     * Returns the callable symbol, i.e., the method symbol for the Java parameter `this`.
     * Assumes that `this` is a parameter to a method.
     */
    private val PsiParameter.callableSymbol: KaCallableSymbol
        get() {
            val decl = this.parent.parent as? PsiMember
                ?: error("Unexpected PsiParameter at ${toLocation()}: $javaClass")
            return analyze {
                decl.callableSymbol
                    ?: error("Unexpected null callable symbol at ${decl.toLocation()}: ${decl.javaClass}")
            }
        }

    /**
     * Returns the index of `this` in the list of type parameters.
     */
    private val PsiTypeParameter.parameterIndex: Int
        get() {
            when (val parent = this.parent) {
                is PsiTypeParameterList ->
                    return parent.getTypeParameterIndex(this)

                else ->
                    error("Unexpected parent of PsiTypeParameter at ${parent.toLocation()}: ${parent.javaClass}")
            }
        }

    /**
     * The fully qualified name of the annotation entry.
     * This member is expensive to compute.
     */
    private val KtAnnotationEntry.qualifiedName: String?
        get() = analyze {
            this@qualifiedName.typeReference
                ?.type
                ?.fullyExpandedType
                ?.expandedSymbol
                ?.classId
                ?.asFqNameString()
        }

    /**
     * The use-site target specified for this annotation entry.
     */
    private val KtAnnotationEntry.ksUseSiteTarget: AnnotationUseSiteTarget?
        get() = useSiteTarget?.getAnnotationUseSiteTarget()?.toKSAnnotationUseSiteTarget()
}
