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

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.processing.impl.KSNameImpl
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.KtStarTypeProjection
import org.jetbrains.kotlin.analysis.api.components.buildClassType
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.psi.KtObjectDeclaration

class KSClassDeclarationImpl private constructor(internal val ktClassOrObjectSymbol: KtClassOrObjectSymbol) :
    KSClassDeclaration,
    AbstractKSDeclarationImpl(ktClassOrObjectSymbol),
    KSExpectActual by KSExpectActualImpl(ktClassOrObjectSymbol) {
    companion object : KSObjectCache<KtClassOrObjectSymbol, KSClassDeclarationImpl>() {
        fun getCached(ktClassOrObjectSymbol: KtClassOrObjectSymbol) =
            cache.getOrPut(ktClassOrObjectSymbol) { KSClassDeclarationImpl(ktClassOrObjectSymbol) }
    }

    override val qualifiedName: KSName? by lazy {
        ktClassOrObjectSymbol.classIdIfNonLocal?.asFqNameString()?.let { KSNameImpl.getCached(it) }
    }

    override val classKind: ClassKind by lazy {
        when (ktClassOrObjectSymbol.classKind) {
            KtClassKind.CLASS -> ClassKind.CLASS
            KtClassKind.ENUM_CLASS -> ClassKind.ENUM_CLASS
            KtClassKind.ANNOTATION_CLASS -> ClassKind.ANNOTATION_CLASS
            KtClassKind.INTERFACE -> ClassKind.INTERFACE
            KtClassKind.COMPANION_OBJECT, KtClassKind.ANONYMOUS_OBJECT, KtClassKind.OBJECT -> ClassKind.OBJECT
        }
    }

    override val primaryConstructor: KSFunctionDeclaration? by lazy {
        if (ktClassOrObjectSymbol.origin == KtSymbolOrigin.JAVA) {
            null
        } else {
            analyze {
                ktClassOrObjectSymbol.getMemberScope().getConstructors().singleOrNull { it.isPrimary }?.let {
                    KSFunctionDeclarationImpl.getCached(it)
                }
            }
        }
    }

    override val superTypes: Sequence<KSTypeReference> by lazy {
        analyze {
            val supers = ktClassOrObjectSymbol.superTypes.mapIndexed { index, type ->
                KSTypeReferenceImpl.getCached(type, this@KSClassDeclarationImpl, index)
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
        (ktClassOrObjectSymbol.psi as? KtObjectDeclaration)?.isCompanion() ?: false
    }

    override fun getSealedSubclasses(): Sequence<KSClassDeclaration> {
        TODO("Not yet implemented")
    }

    override fun getAllFunctions(): Sequence<KSFunctionDeclaration> {
        return ktClassOrObjectSymbol.getAllFunctions()
    }

    override fun getAllProperties(): Sequence<KSPropertyDeclaration> {
        return ktClassOrObjectSymbol.getAllProperties()
    }

    override fun asType(typeArguments: List<KSTypeArgument>): KSType {
        return analyze {
            analysisSession.buildClassType(ktClassOrObjectSymbol) {
                typeArguments.forEach { argument(it.toKtTypeProjection()) }
            }.let { KSTypeImpl.getCached(it) }
        }
    }

    override fun asStarProjectedType(): KSType {
        return analyze {
            KSTypeImpl.getCached(
                analysisSession.buildClassType(ktClassOrObjectSymbol) {
                    var current: KSNode? = this@KSClassDeclarationImpl
                    while (current is KSClassDeclarationImpl) {
                        current.ktClassOrObjectSymbol.typeParameters.forEach {
                            argument(
                                KtStarTypeProjection(
                                    (current as KSClassDeclarationImpl).ktClassOrObjectSymbol.token
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

    override val declarations: Sequence<KSDeclaration> by lazy {
        val decls = ktClassOrObjectSymbol.declarations()
        if (origin == Origin.JAVA && classKind != ClassKind.ANNOTATION_CLASS) {
            buildList {
                decls.forEach { decl ->
                    if (decl is KSPropertyDeclarationImpl && decl.ktPropertySymbol is KtSyntheticJavaPropertySymbol) {
                        decl.getter?.let {
                            add(
                                KSFunctionDeclarationImpl.getCached(
                                    (it as KSPropertyAccessorImpl).ktPropertyAccessorSymbol
                                )
                            )
                        }
                        decl.setter?.let {
                            add(
                                KSFunctionDeclarationImpl.getCached(
                                    (it as KSPropertyAccessorImpl).ktPropertyAccessorSymbol
                                )
                            )
                        }
                    } else {
                        add(decl)
                    }
                }
            }.asSequence()
        } else decls
    }
}

internal fun KtClassOrObjectSymbol.toModifiers(): Set<Modifier> {
    val result = mutableSetOf<Modifier>()
    if (this is KtNamedClassOrObjectSymbol) {
        result.add(modality.toModifier())
        result.add(visibility.toModifier())
        if (isInline) {
            result.add(Modifier.INLINE)
        }
        if (isData) {
            result.add(Modifier.DATA)
        }
        if (isExternal) {
            result.add(Modifier.EXTERNAL)
        }
    }
    if (classKind == KtClassKind.ENUM_CLASS) {
        result.add(Modifier.ENUM)
    }
    return result
}
