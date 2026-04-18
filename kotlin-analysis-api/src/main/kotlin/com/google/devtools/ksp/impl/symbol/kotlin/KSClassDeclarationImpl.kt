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

package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.common.errorTypeOnInconsistentArguments
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.common.impl.KSTypeReferenceSyntheticImpl
import com.google.devtools.ksp.common.isObjectOverride
import com.google.devtools.ksp.common.lazyMemoizedSequence
import com.google.devtools.ksp.impl.ResolverAAImpl
import com.google.devtools.ksp.impl.recordGetSealedSubclasses
import com.google.devtools.ksp.impl.recordLookup
import com.google.devtools.ksp.impl.recordLookupForGetAllFunctions
import com.google.devtools.ksp.impl.recordLookupForGetAllProperties
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.symbol.*
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseStarTypeProjection
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.abbreviationOrSelf
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject

class KSClassDeclarationImpl private constructor(internal val ktClassOrObjectSymbol: KaClassSymbol) :
    KSClassDeclaration,
    AbstractKSDeclarationImpl(),
    KSExpectActual by KSExpectActualImpl(ktClassOrObjectSymbol) {
    private val isExperimentalPsiResolutionEnabled: Boolean by lazy {
        ResolverAAImpl.kspConfig.experimentalPsiResolution
    }

    override val ktDeclarationSymbol = ktClassOrObjectSymbol

    companion object : KSObjectCache<KaClassSymbol, KSClassDeclarationImpl>() {
        fun getCached(ktClassOrObjectSymbol: KaClassSymbol) =
            cache.getOrPut(ktClassOrObjectSymbol) { KSClassDeclarationImpl(ktClassOrObjectSymbol) }
    }

    override val qualifiedName: KSName? by lazy {
        ktClassOrObjectSymbol.classId?.asFqNameString()?.let { KSNameImpl.getCached(it) }
    }

    override val classKind: ClassKind by lazy {
        when (ktClassOrObjectSymbol.classKind) {
            KaClassKind.CLASS -> ClassKind.CLASS
            KaClassKind.ENUM_CLASS -> ClassKind.ENUM_CLASS
            KaClassKind.ANNOTATION_CLASS -> ClassKind.ANNOTATION_CLASS
            KaClassKind.INTERFACE -> ClassKind.INTERFACE
            KaClassKind.COMPANION_OBJECT, KaClassKind.ANONYMOUS_OBJECT, KaClassKind.OBJECT -> ClassKind.OBJECT
        }
    }

    override val primaryConstructor: KSFunctionDeclaration? by lazy {
        if (ktClassOrObjectSymbol.origin == KaSymbolOrigin.JAVA_SOURCE) {
            null
        } else {
            analyze {
                ktClassOrObjectSymbol.memberScope.constructors.singleOrNull { it.isPrimary }?.let {
                    KSFunctionDeclarationImpl.getCached(it)
                }
            }
        }
    }

    override val superTypes: Sequence<KSTypeReference> by lazyMemoizedSequence {
        (ktClassOrObjectSymbol.psiIfSource() as? KtClassOrObject)?.let { classOrObject ->
            if (classKind == ClassKind.ANNOTATION_CLASS || classKind == ClassKind.ENUM_CLASS) {
                null
            } else {
                classOrObject.superTypeListEntries.map {
                    KSTypeReferenceImpl.getCached(it.typeReference!!, this)
                }.asSequence().ifEmpty {
                    sequenceOf(
                        KSTypeReferenceSyntheticImpl.getCached(ResolverAAImpl.instance.builtIns.anyType, this)
                    )
                }
            }
        } ?: analyze {
            val supers = ktClassOrObjectSymbol.superTypes.mapIndexed { index, type ->
                KSTypeReferenceResolvedImpl.getCached(type.abbreviationOrSelf, this@KSClassDeclarationImpl, index)
            }
            // AA is returning additional kotlin.Any for java classes, explicitly extending kotlin.Any will result in
            // compile error, therefore filtering by name should work.
            // TODO: reconsider how to model super types for interface.
            if (supers.size > 1) {
                supers.filterNot { it.resolve().declaration.qualifiedName?.asString() == "kotlin.Any" }
            } else {
                supers
            }.asSequence()
        }
    }

    override val isCompanionObject: Boolean by lazy {
        ktClassOrObjectSymbol.classKind == KaClassKind.COMPANION_OBJECT
    }

    override fun getSealedSubclasses(): Sequence<KSClassDeclaration> {
        if (!modifiers.contains(Modifier.SEALED)) return emptySequence()
        recordGetSealedSubclasses(this)
        return (ktClassOrObjectSymbol as? KaNamedClassSymbol)?.let {
            analyze {
                it.sealedClassInheritors.map { getCached(it) }.asSequence()
            }
        } ?: emptySequence()
    }

    override fun getAllFunctions(): Sequence<KSFunctionDeclaration> {
        ktClassOrObjectSymbol.superTypes.forEach { recordLookup(it, this) }
        recordLookupForGetAllFunctions(ktClassOrObjectSymbol.superTypes)
        return ktClassOrObjectSymbol.getAllFunctions()
    }

    override fun getAllProperties(): Sequence<KSPropertyDeclaration> {
        ktClassOrObjectSymbol.superTypes.forEach { recordLookup(it, this) }
        recordLookupForGetAllProperties(ktClassOrObjectSymbol.superTypes)
        return ktClassOrObjectSymbol.getAllProperties()
    }

    override fun asType(typeArguments: List<KSTypeArgument>): KSType {
        errorTypeOnInconsistentArguments(
            arguments = typeArguments,
            placeholdersProvider = { asStarProjectedType().arguments },
            withCorrectedArguments = ::asType,
            errorType = ::KSErrorType,
        )?.let { error -> return error }
        return analyze {
            ktClassOrObjectSymbol.tryResolveToTypePhase()
            if (typeArguments.isEmpty()) {
                // Resolving a class symbol also resolves its type parameters.
                typeParameters.map { buildTypeParameterType((it as KSTypeParameterImpl).ktTypeParameterSymbol) }
                    .let { typeParameterTypes ->
                        buildClassType(ktClassOrObjectSymbol) {
                            typeParameterTypes.forEach { argument(it) }
                        }
                    }
            } else {
                buildClassType(ktClassOrObjectSymbol) {
                    typeArguments.forEach { argument(it.toKtTypeProjection()) }
                }
            }.let { KSTypeImpl.getCached(it) }
        }
    }

    @OptIn(KaExperimentalApi::class, KaImplementationDetail::class)
    override fun asStarProjectedType(): KSType {
        return analyze {
            KSTypeImpl.getCached(
                useSiteSession.buildClassType(ktClassOrObjectSymbol.tryResolveToTypePhase()) {
                    var current: KSNode? = this@KSClassDeclarationImpl
                    while (current is KSClassDeclarationImpl) {
                        current.ktClassOrObjectSymbol.typeParameters.forEach { _ ->
                            argument(
                                KaBaseStarTypeProjection(
                                    current.ktClassOrObjectSymbol.token
                                )
                            )
                        }
                        current = if (Modifier.INNER in current.modifiers) {
                            current.parent
                        } else {
                            null
                        }
                    }
                }
            )
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitClassDeclaration(this, data)
    }

    override val declarations: Sequence<KSDeclaration> by lazyMemoizedSequence {
        // FAST PATH: Use PSI directly for Java classes
        if (isExperimentalPsiResolutionEnabled) {
            ktClassOrObjectSymbol.psi?.let { psi ->
                if (psi is PsiClass && canUsePsiResolution()) {
                    return@lazyMemoizedSequence psi.getDeclarationsFromPsi()
                }
            }
        }

        // SLOW PATH: Original AA fallback for Kotlin classes (or if ASM failed)
        getDeclarationsFromAnalysisApi()
    }

    private fun canUsePsiResolution(): Boolean {
        return (origin == Origin.JAVA || origin == Origin.JAVA_LIB) &&
            // Java annotations, enums, and types that map to Kotlin types (e.g. java.lang.String -> kotlin.String) have
            // additional properties added by the Analysis API that don't actually exist in the Java type so just skip
            // them to avoid this complexity.
            classKind != ClassKind.ANNOTATION_CLASS &&
            classKind != ClassKind.ENUM_CLASS &&
            !ktClassOrObjectSymbol.isOrInheritsFromKotlinAlteredType()
    }

    private fun PsiClass.getDeclarationsFromPsi(): Sequence<KSDeclaration> {
        // 1. Fields
        val psiFields = fields.asSequence()
            .map { KSPropertyDeclarationJavaImpl.getCached(psi = it, parent = this@KSClassDeclarationImpl) }

        // 2. Methods (Non-constructors)
        val psiMethods = methods.asSequence()
            // Mimic FIR dropping malformed constructors that don't match class name, e.g. `class Foo { Bar() {} }`
            .filter { !it.isConstructor }
            // Mimic FIR dropping Object overrides in Java interfaces (e.g. `@Override boolean equals(Object)`).
            .filter { !(isInterface && it.isObjectOverride()) }
            .map { KSFunctionDeclarationImpl.getCached(psi = it, parent = this@KSClassDeclarationImpl) }

        // 3. Constructors
        // IntelliJ's PSI lacks implicit constructors. If none are explicitly declared,
        // we must fetch the synthesized constructor from the Analysis API.
        val psiConstructors = if (constructors.isEmpty() && !isInterface) {
            analyze {
                ktClassOrObjectSymbol.declaredMemberScope.constructors.map {
                    KSFunctionDeclarationImpl.getCached(it)
                }
            }
        } else {
            constructors.asSequence()
                .map { KSFunctionDeclarationImpl.getCached(psi = it, parent = this@KSClassDeclarationImpl) }
        }

        // 4. Inner Classes
        val psiInnerClasses = innerClasses.asSequence()
            .mapNotNull { psiInnerClass ->
                analyze {
                    val name = psiInnerClass.name?.let { Name.identifier(it) } ?: return@analyze null
                    val instanceSymbols = ktClassOrObjectSymbol.declaredMemberScope.classifiers(name)
                    val staticSymbols = ktClassOrObjectSymbol.staticDeclaredMemberScope.classifiers(name)
                    (instanceSymbols + staticSymbols)
                        .filterIsInstance<KaNamedClassSymbol>()
                        .find { it.psi == psiInnerClass || it.psi?.isEquivalentTo(psiInnerClass) == true }
                        ?.let { KSClassDeclarationImpl.getCached(it) }
                }
            }

        // 5. Partition members into static and instance members to emulate FIR's ordering.
        val (psiStaticMembers, psiInstanceMembers) = (psiFields + psiMethods + psiInnerClasses).partition {
            Modifier.JAVA_STATIC in it.modifiers ||
                // Nested classes in KSP don't get the static modifier.
                (it is KSClassDeclaration && Modifier.INNER !in it.modifiers)
        }

        // Note: We could return declarations in source order by iterating over PsiClass.declarations directly.
        // However, this ordering matches how things are done using the Analysis API to keep things consistent.
        return psiInstanceMembers.asSequence() + psiConstructors + psiStaticMembers.asSequence()
    }

    private fun getDeclarationsFromAnalysisApi(): Sequence<KSDeclaration> {
        val decls = ktClassOrObjectSymbol.declarations()
            // The Analysis API is known to leak certain inherited members, so we filter those cases out here.
            .filter { !it.isLeakedInheritedMember(this) }

        return if ((origin == Origin.JAVA || origin == Origin.JAVA_LIB) && classKind != ClassKind.ANNOTATION_CLASS) {
            decls.flatMap { decl ->
                if (decl is KSPropertyDeclarationImpl && decl.ktPropertySymbol is KaSyntheticJavaPropertySymbol) {
                    sequenceOf(decl.getter, decl.setter).mapNotNull { accessor ->
                        KSFunctionDeclarationImpl.getCached(
                            (accessor as? KSPropertyAccessorImpl)?.ktPropertyAccessorSymbol
                                ?: return@mapNotNull null
                        )
                    }
                } else {
                    sequenceOf(decl)
                }
            }
        } else decls
    }

    override fun defer(): Restorable? {
        return ktClassOrObjectSymbol.defer(::getCached)
    }
}

internal fun KaClassSymbol.toModifiers(): Set<Modifier> {
    val result = mutableSetOf<Modifier>()
    if (this is KaNamedClassSymbol) {
        result.add(modality.toModifier())
        if (visibility != KaSymbolVisibility.PACKAGE_PRIVATE) {
            result.add(visibility.toModifier())
        }
        if (isFun) {
            result.add(Modifier.FUN)
        }
        if (isInline) {
            result.add(Modifier.INLINE)
        }
        if (isData) {
            result.add(Modifier.DATA)
        }
        if (isExternal) {
            result.add(Modifier.EXTERNAL)
        }
        if (isInner) {
            result.add(Modifier.INNER)
        }
    }
    if (classKind == KaClassKind.ENUM_CLASS) {
        result.add(Modifier.ENUM)
    }
    return result
}
