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

import com.google.devtools.ksp.InternalKSPException
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.common.lazyMemoizedSequence
import com.google.devtools.ksp.common.toKSModifiers
import com.google.devtools.ksp.impl.ResolverAAImpl
import com.google.devtools.ksp.impl.recordLookupForPropertyOrMethod
import com.google.devtools.ksp.impl.recordLookupWithSupertypes
import com.google.devtools.ksp.impl.symbol.java.KSAnnotationJavaImpl
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FunctionKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunction
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDestructuringDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.analysis.api.types.abbreviationOrSelf
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction

sealed class KSFunctionDeclarationImpl : KSFunctionDeclaration, AbstractKSDeclarationImpl() {
    companion object {
        // Factory for AA Slow Path
        fun getCached(symbol: KaFunctionSymbol): KSFunctionDeclarationImpl =
            KSFunctionDeclarationAAImpl.getCached(symbol)

        // Factory for PSI Fast Path
        fun getCached(psi: PsiMethod, parent: KSClassDeclarationImpl): KSFunctionDeclarationImpl =
            KSFunctionDeclarationPsiImpl.getCached(psi, parent)
    }

    abstract val ktFunctionSymbol: KaFunctionSymbol

    override val ktDeclarationSymbol: KaFunctionSymbol get() = ktFunctionSymbol

    // Manual delegation for KSExpectActual to avoid eager evaluation in the class header
    private val expectActualImpl by lazy { KSExpectActualImpl(ktFunctionSymbol) }
    override val isActual: Boolean get() = expectActualImpl.isActual
    override val isExpect: Boolean get() = expectActualImpl.isExpect
    override fun findActuals(): Sequence<KSDeclaration> = expectActualImpl.findActuals()
    override fun findExpects(): Sequence<KSDeclaration> = expectActualImpl.findExpects()

    override val functionKind: FunctionKind by lazy {
        when (ktFunctionSymbol.location) {
            KaSymbolLocation.CLASS -> {
                if ((ktFunctionSymbol as? KaNamedFunctionSymbol)?.isStatic == true) {
                    FunctionKind.STATIC
                } else {
                    FunctionKind.MEMBER
                }
            }

            KaSymbolLocation.TOP_LEVEL -> FunctionKind.TOP_LEVEL
            else -> throw InternalKSPException(
                "Unexpected location ${ktFunctionSymbol.location}",
                this.location,
                ktFunctionSymbol.javaClass
            )
        }
    }

    override val isAbstract: Boolean by lazy {
        (ktFunctionSymbol as? KaNamedFunctionSymbol)?.modality == KaSymbolModality.ABSTRACT
    }

    override val modifiers: Set<Modifier> by lazy {
        if (isConstructor()) {
            val ksClassDeclaration = parentDeclaration as KSClassDeclaration
            if (ksClassDeclaration.classKind == ClassKind.ENUM_CLASS) {
                setOf(Modifier.FINAL, Modifier.PRIVATE)
            } else if (isSyntheticConstructor() && ksClassDeclaration.isPublic()) {
                setOf(Modifier.FINAL, Modifier.PUBLIC)
            } else {
                super.modifiers + Modifier.FINAL
            }
        } else {
            super.modifiers
        }
    }

    override val extensionReceiver: KSTypeReference? by lazy {
        analyze {
            if (!ktFunctionSymbol.isExtension) {
                null
            } else {
                (ktFunctionSymbol.psiIfSource() as? KtFunction)?.receiverTypeReference
                    ?.let {
                        // receivers are modeled as parameter in AA therefore annotations are stored in
                        // the corresponding receiver parameter, need to pass it to the `KSTypeReferenceImpl`
                        KSTypeReferenceImpl.getCached(
                            it,
                            this@KSFunctionDeclarationImpl,
                            ktFunctionSymbol.receiverParameter?.annotations ?: emptyList()
                        )
                    }
                    ?: ktFunctionSymbol.receiverType?.abbreviationOrSelf?.let {
                        KSTypeReferenceResolvedImpl.getCached(
                            it,
                            this@KSFunctionDeclarationImpl,
                            -1,
                            ktFunctionSymbol.receiverParameter?.annotations ?: emptyList()
                        )
                    }
            }
        }
    }

    override val returnType: KSTypeReference? by lazy {
        (ktFunctionSymbol.psiIfSource() as? KtFunction)?.typeReference?.let { KSTypeReferenceImpl.getCached(it, this) }
            ?: analyze {
                // Constructors
                if (ktFunctionSymbol is KaConstructorSymbol) {
                    ((parentDeclaration as KSClassDeclaration).asStarProjectedType() as KSTypeImpl).type
                } else {
                    ktFunctionSymbol.returnType.abbreviationOrSelf
                }.let { KSTypeReferenceResolvedImpl.getCached(it, this@KSFunctionDeclarationImpl) }
            }
    }

    override val parameters: List<KSValueParameter> by lazy {
        ktFunctionSymbol.valueParameters.map { KSValueParameterImpl.getCached(it, this) }
    }

    override fun findOverridee(): KSDeclaration? {
        closestClassDeclaration()?.asStarProjectedType()?.let {
            recordLookupWithSupertypes((it as KSTypeImpl).type)
        }
        recordLookupForPropertyOrMethod(this)
        return analyze {
            if (ktFunctionSymbol is KaPropertyAccessorSymbol) {
                (parentDeclaration as? KSPropertyDeclarationImpl)?.ktPropertySymbol
            } else {
                ktFunctionSymbol
            }?.directlyOverriddenSymbols?.firstOrNull()?.fakeOverrideOriginal?.toKSDeclaration()
        }?.also { recordLookupForPropertyOrMethod(it) }
    }

    override fun asMemberOf(containing: KSType): KSFunction {
        return ResolverAAImpl.instance.computeAsMemberOf(this, containing)
    }

    override val simpleName: KSName by lazy {
        when (val ktFunctionSymbol = this.ktFunctionSymbol) {
            is KaNamedFunctionSymbol -> KSNameImpl.getCached(ktFunctionSymbol.name.asString())
            is KaPropertyAccessorSymbol -> when (val psi = ktFunctionSymbol.psi) {
                is PsiMethod -> KSNameImpl.getCached(psi.name)
                else -> {
                    // 1. Try to get the callable id
                    // 2. Try to synthetically construct the getter or setter as e.g., "propertyName.getter()"
                    // 3. Fall back to <no name>
                    val name = ktFunctionSymbol.callableId?.callableName?.asString()
                        ?: analyze { ktFunctionSymbol.containingSymbol?.name?.asString() }?.let { propertyName ->
                            buildString {
                                append(propertyName)
                                append(".")
                                when (ktFunctionSymbol) {
                                    is KaPropertyGetterSymbol -> append("getter")
                                    is KaPropertySetterSymbol -> append("setter")
                                }
                                append("()")
                            }
                        } ?: "<no name>"
                    KSNameImpl.getCached(name)
                }
            }

            is KaConstructorSymbol -> KSNameImpl.getCached("<init>")
            else -> throw InternalKSPException(
                "Unexpected function symbol type ${ktFunctionSymbol.javaClass}",
                this.location,
                ktFunctionSymbol.javaClass
            )
        }
    }

    override val qualifiedName: KSName? by lazy {
        ktFunctionSymbol.callableId?.asSingleFqName()?.asString()?.let { KSNameImpl.getCached(it) }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitFunctionDeclaration(this, data)
    }

    override val declarations: Sequence<KSDeclaration> by lazyMemoizedSequence {
        val psi = ktFunctionSymbol.psi as? KtFunction ?: return@lazyMemoizedSequence emptySequence()
        if (!psi.hasBlockBody()) {
            emptySequence()
        } else {
            psi.bodyBlockExpression?.statements?.asSequence()?.filterIsInstance<KtDeclaration>()?.flatMap {
                analyze {
                    when (val symbol = it.symbol) {
                        is KaDestructuringDeclarationSymbol -> {
                            symbol.entries.mapNotNull { it.toKSDeclaration() }
                        }

                        else -> listOfNotNull(symbol.toKSDeclaration())
                    }
                }
            } ?: emptySequence()
        }
    }

    override val origin: Origin by lazy {
        // Override origin for java synthetic constructors.
        if (
            ktFunctionSymbol.origin == KaSymbolOrigin.JAVA_SOURCE &&
            (ktFunctionSymbol.psi == null || ktFunctionSymbol.psi is PsiClass)
        ) {
            Origin.SYNTHETIC
        } else {
            super.origin
        }
    }

    private fun isSyntheticConstructor(): Boolean {
        return isConstructor() && (
            origin == Origin.SYNTHETIC ||
                (origin == Origin.JAVA && ktFunctionSymbol.psi == null || ktFunctionSymbol.psi is PsiClass)
            )
    }

    override val annotations: Sequence<KSAnnotation>
        get() = if (isSyntheticConstructor()) {
            emptySequence()
        } else {
            super.annotations
        }

    override fun toString(): String {
        // TODO: fix origin for implicit Java constructor in AA
        // TODO: should we change the toString() behavior for synthetic constructors?
        return if (isSyntheticConstructor()) {
            "synthetic constructor for ${this.parentDeclaration}"
        } else {
            this.simpleName.asString()
        }
    }

    override val docString: String? by lazy {
        if (isSyntheticConstructor()) {
            parentDeclaration?.docString
        } else {
            super.docString
        }
    }

    override fun defer(): Restorable? {
        return ktFunctionSymbol.defer(::getCached)
    }

    val isGetter: Boolean by lazy {
        ktFunctionSymbol is KaPropertyGetterSymbol
    }

    val isSetter: Boolean by lazy {
        ktFunctionSymbol is KaPropertySetterSymbol
    }
}

private class KSFunctionDeclarationAAImpl(
    override val ktFunctionSymbol: KaFunctionSymbol
) : KSFunctionDeclaration, KSFunctionDeclarationImpl() {
    companion object : KSObjectCache<KaFunctionSymbol, KSFunctionDeclarationAAImpl>() {
        fun getCached(symbol: KaFunctionSymbol) = cache.getOrPut(symbol) {
            KSFunctionDeclarationAAImpl(symbol)
        }
    }
}

private class KSFunctionDeclarationPsiImpl(
    val psiMethod: PsiMethod,
    val parentClass: KSClassDeclarationImpl
) : KSFunctionDeclaration, KSFunctionDeclarationImpl() {
    companion object : KSObjectCache<PsiMethod, KSFunctionDeclarationPsiImpl>() {
        fun getCached(psiMethod: PsiMethod, parentClass: KSClassDeclarationImpl) = cache.getOrPut(psiMethod) {
            KSFunctionDeclarationPsiImpl(psiMethod, parentClass)
        }
    }

    override val ktFunctionSymbol: KaFunctionSymbol by lazy {
        analyze { findKaFunctionSymbol(psiMethod, parentClass) }
            ?: throw InternalKSPException(
                "Failed to resolve KaFunctionSymbol for method ${parentClass.simpleName.asString()}.${psiMethod.name}",
                psiMethod.toLocation(),
                psiMethod.javaClass,
            )
    }

    override val simpleName: KSName by lazy {
        if (psiMethod.isConstructor) KSNameImpl.getCached("<init>") else KSNameImpl.getCached(psiMethod.name)
    }

    override val origin: Origin by lazy {
        when (parent) {
            // If our lazy parent resolution determined that this method belongs to a Kotlin property,
            // it means FIR considers it a synthetic property accessor.
            is KSPropertyDeclaration -> Origin.SYNTHETIC
            else -> parentClass.origin
        }
    }

    override val containingFile: KSFile? get() = parentClass.containingFile

    override val packageName: KSName get() = parentClass.packageName

    override val isAbstract: Boolean by lazy { psiMethod.hasModifierProperty(PsiModifier.ABSTRACT) }

    override val functionKind: FunctionKind by lazy {
        if (psiMethod.hasModifierProperty(PsiModifier.STATIC)) FunctionKind.STATIC else FunctionKind.MEMBER
    }

    override val modifiers: Set<Modifier> by lazy {
        if (origin == Origin.JAVA) {
            return@lazy psiMethod.toKSModifiers()
        }

        val psiModifiers = psiMethod.toKSModifiers().toMutableSet()

        // Emulate AA: Strip JVM-specific modifiers from compiled Java methods
        psiModifiers.remove(Modifier.JAVA_TRANSIENT)
        psiModifiers.remove(Modifier.JAVA_VOLATILE)
        psiModifiers.remove(Modifier.JAVA_SYNCHRONIZED)
        psiModifiers.remove(Modifier.JAVA_NATIVE)
        psiModifiers.remove(Modifier.JAVA_STRICT)
        psiModifiers.remove(Modifier.JAVA_DEFAULT)

        when {
            psiMethod.isConstructor -> {
                // Constructors are always final
                psiModifiers.add(Modifier.FINAL)
            }
            psiMethod.hasModifierProperty(PsiModifier.STATIC) -> {
                // Emulate AA: Static methods in compiled Java libraries are treated as final in Kotlin
                psiModifiers.add(Modifier.FINAL)
            }
            !psiMethod.hasModifierProperty(PsiModifier.FINAL) &&
                !psiMethod.hasModifierProperty(PsiModifier.PRIVATE) -> {
                // Non-final, non-static, non-private Java instance methods are open in Kotlin
                psiModifiers.add(Modifier.OPEN)
            }
        }

        return@lazy psiModifiers
    }

    override val annotations: Sequence<KSAnnotation> by lazyMemoizedSequence {
        psiMethod.annotations.asSequence().map { KSAnnotationJavaImpl.getCached(it, this) }
    }

    // Java doesn't have extension receivers.
    override val extensionReceiver: KSTypeReference? = null
}

/**
 * Resolves the [KaFunctionSymbol] that corresponds to the given [PsiMethod] within the [parent] class.
 *
 * This method searches for a symbol whose underlying PSI matches the provided [psiMethod] using
 * exact identity or equivalence. It handles several Kotlin Analysis API (FIR) interop edge cases:
 *
 * 1. **Performance Optimization (KT-85692):** For non-constructor methods, it queries `memberScope`
 *    and `staticMemberScope` instead of `declaredMemberScope`. Querying the declared member scope
 *    on Java classes triggers a severe performance bottleneck in the Analysis API, as it lacks a
 *    dedicated declared enhancement scope for Java classes and falls back to resolving the entire
 *    inherited use-site scope recursively.
 * 2. **Property Accessor Fallback:** If a Java method follows getter/setter conventions (e.g., `getFoo()`)
 *    and overrides a Kotlin property (`val foo`), FIR hides the method from the standard function scope
 *    and exposes it as a property symbol instead. If the initial function match fails, this method
 *    falls back to searching for property accessors via `findKaPropertyAccessorSymbols`.
 *
 * @param psiMethod The Java PSI method to resolve.
 * @param parent The enclosing class declaration proxy.
 * @return The resolved [KaFunctionSymbol], or null if the symbol could not be found.
 */
fun KaSession.findKaFunctionSymbol(psiMethod: PsiMethod, parentClass: KSClassDeclarationImpl): KaFunctionSymbol? {
    fun Sequence<KaSymbol>.firstMatchingPsiMethod(psi: PsiMethod): KaFunctionSymbol? {
        return filterIsInstance<KaFunctionSymbol>().find { it.psi == psi || it.psi?.isEquivalentTo(psi) == true }
    }
    val parentSymbol = parentClass.ktClassOrObjectSymbol
    return if (psiMethod.isConstructor) {
        parentSymbol.declaredMemberScope.constructors.firstMatchingPsiMethod(psiMethod)
    } else {
        val methodName = Name.identifier(psiMethod.name)
        // Note: Technically, declaredMemberScope and staticDeclaredMemberScope could be used here since the psi method
        // is declared in the parent. However, those methods trigger the performance issue mentioned in
        // https://youtrack.jetbrains.com/issue/KT-85692, so stick with memberScope/staticMemberScope instead.
        parentSymbol.memberScope.callables(methodName).firstMatchingPsiMethod(psiMethod)
            ?: parentSymbol.staticMemberScope.callables(methodName).firstMatchingPsiMethod(psiMethod)
            // If the above match failed, it means that FIR hid the method because it overrides a Kotlin property (e.g.
            // `getX` -> `x`), so we need to find the property and get the KaFunctionSymbol from it instead.
            ?: findKaPropertyAccessorSymbols(psiMethod, parentClass).firstMatchingPsiMethod(psiMethod)
    }
}

private fun KaSession.findKaPropertyAccessorSymbols(
    psiMethod: PsiMethod,
    parentClass: KSClassDeclarationImpl
): Sequence<KaPropertyAccessorSymbol> {
    val methodName = psiMethod.name
    val namesToSearch = when {
        // A standard Java getter like `getFoo()` overrides a Kotlin property named `foo` or `Foo`.
        methodName.startsWith("get") && methodName.length > 3 -> {
            val accessorBase = methodName.substring(3)
            val propertyName = accessorBase.replaceFirstChar { it.lowercaseChar() }
            sequenceOf(propertyName, accessorBase)
        }
        // A Java boolean getter like `isFoo()` can override a Kotlin property named `foo` or `Foo`,
        // OR a property explicitly named `isFoo`. All are valid in Kotlin interoperability.
        methodName.startsWith("is") && methodName.length > 2 -> {
            val accessorBase = methodName.substring(2)
            val propertyName = accessorBase.replaceFirstChar { it.lowercaseChar() }
            sequenceOf(propertyName, accessorBase, methodName)
        }
        // A Java setter like `setFoo()` can override a standard Kotlin property named `foo` or `Foo`,
        // OR it can override a boolean property explicitly named `isFoo`.
        methodName.startsWith("set") && methodName.length > 3 -> {
            val accessorBase = methodName.substring(3)
            val propertyName = accessorBase.replaceFirstChar { it.lowercaseChar() }
            sequenceOf(propertyName, accessorBase, "is$accessorBase")
        }
        else -> return emptySequence()
    }
    return namesToSearch
        .flatMap { parentClass.ktClassOrObjectSymbol.memberScope.callables(Name.identifier(it)) }
        .filterIsInstance<KaPropertySymbol>()
        .flatMap { sequenceOf(it.getter, it.setter) }
        .filterNotNull()
}

internal fun KaFunctionSymbol.toModifiers(): Set<Modifier> {
    val result = mutableSetOf<Modifier>()
    when (this) {
        is KaConstructorSymbol -> {
            if (visibility != KaSymbolVisibility.PACKAGE_PRIVATE) {
                result.add(visibility.toModifier())
            }
            result.add(Modifier.FINAL)
        }

        is KaNamedFunctionSymbol -> {
            if (visibility != KaSymbolVisibility.PACKAGE_PRIVATE) {
                result.add(visibility.toModifier())
            }
            if (!isStatic || modality != KaSymbolModality.OPEN) {
                result.add(modality.toModifier())
            }
            if (isExternal) {
                result.add(Modifier.EXTERNAL)
            }
            if (isInfix) {
                result.add(Modifier.INFIX)
            }
            if (isInline) {
                result.add(Modifier.INLINE)
            }
            if (isStatic) {
                result.add(Modifier.JAVA_STATIC)
                result.add(Modifier.FINAL)
            }
            if (isSuspend) {
                result.add(Modifier.SUSPEND)
            }
            if (isOperator) {
                result.add(Modifier.OPERATOR)
            }
            if (isOperator) {
                result.add(Modifier.OVERRIDE)
            }
        }

        is KaPropertyAccessorSymbol -> {
            if (visibility != KaSymbolVisibility.PACKAGE_PRIVATE) {
                result.add(visibility.toModifier())
            }
            if (modality != KaSymbolModality.OPEN) {
                result.add(modality.toModifier())
            }
        }

        else -> Unit
    }
    return result
}
