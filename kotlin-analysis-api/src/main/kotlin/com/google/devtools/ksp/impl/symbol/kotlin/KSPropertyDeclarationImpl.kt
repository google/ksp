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
import com.google.devtools.ksp.impl.ResolverAAImpl
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.impl.symbol.util.BinaryClassInfoCache
import com.google.devtools.ksp.processing.impl.KSNameImpl
import com.google.devtools.ksp.symbol.*
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.psi.KtProperty

class KSPropertyDeclarationImpl private constructor(internal val ktPropertySymbol: KtPropertySymbol) :
    KSPropertyDeclaration,
    AbstractKSDeclarationImpl(ktPropertySymbol),
    KSExpectActual by KSExpectActualImpl(ktPropertySymbol) {
    companion object : KSObjectCache<KtPropertySymbol, KSPropertyDeclarationImpl>() {
        fun getCached(ktPropertySymbol: KtPropertySymbol) =
            cache.getOrPut(ktPropertySymbol) { KSPropertyDeclarationImpl(ktPropertySymbol) }
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        ktPropertySymbol.annotations.asSequence()
            .filter { !it.isUseSiteTargetAnnotation() }
            .map { KSAnnotationImpl.getCached(it, this) }
            .plus(
                if (ktPropertySymbol.isFromPrimaryConstructor) {
                    (parentDeclaration as? KSClassDeclaration)?.primaryConstructor?.parameters
                        ?.singleOrNull { it.name == simpleName }?.annotations ?: emptySequence()
                } else {
                    emptySequence()
                }
            ).filterNot { valueParameterAnnotation ->
                valueParameterAnnotation.annotationType.resolve().declaration.annotations.any { metaAnnotation ->
                    metaAnnotation.annotationType.resolve().declaration.qualifiedName
                        ?.asString() == "kotlin.annotation.Target" &&
                        (metaAnnotation.arguments.singleOrNull()?.value as? ArrayList<*>)?.any {
                        (it as? KSClassDeclaration)?.qualifiedName
                            ?.asString() == "kotlin.annotation.AnnotationTarget.VALUE_PARAMETER"
                    } ?: false
                }
            }.plus(
                ktPropertySymbol.backingFieldSymbol?.annotations
                    ?.map { KSAnnotationImpl.getCached(it) } ?: emptyList()
            )
    }

    override val getter: KSPropertyGetter? by lazy {
        if (ktPropertySymbol.psi is PsiClass) {
            null
        } else {
            ktPropertySymbol.getter?.let { KSPropertyGetterImpl.getCached(this, it) }
        }
    }

    override val setter: KSPropertySetter? by lazy {
        if (ktPropertySymbol.psi is PsiClass) {
            null
        } else {
            ktPropertySymbol.setter?.let { KSPropertySetterImpl.getCached(this, it) }
        }
    }

    override val extensionReceiver: KSTypeReference? by lazy {
        (ktPropertySymbol.psiIfSource() as? KtProperty)?.receiverTypeReference
            ?.let { KSTypeReferenceImpl.getCached(it, this) }
            ?: ktPropertySymbol.receiverType
                ?.let { KSTypeReferenceResolvedImpl.getCached(it, this@KSPropertyDeclarationImpl) }
    }

    override val type: KSTypeReference by lazy {
        (ktPropertySymbol.psiIfSource() as? KtProperty)?.typeReference?.let { KSTypeReferenceImpl.getCached(it, this) }
            ?: KSTypeReferenceResolvedImpl.getCached(ktPropertySymbol.returnType, this@KSPropertyDeclarationImpl)
    }

    override val isMutable: Boolean by lazy {
        !ktPropertySymbol.isVal
    }

    override val hasBackingField: Boolean by lazy {
        if (origin == Origin.KOTLIN_LIB || origin == Origin.JAVA_LIB) {
            val fileManager = ResolverAAImpl.instance.javaFileManager
            val parentClass = this.findParentOfType<KSClassDeclaration>()
            val classId = (parentClass as KSClassDeclarationImpl).ktClassOrObjectSymbol.classIdIfNonLocal!!
            val virtualFileContent = analyze {
                (fileManager.findClass(classId, analysisScope) as JavaClassImpl).virtualFile!!.contentsToByteArray()
            }
            BinaryClassInfoCache.getCached(classId, virtualFileContent).fieldAccFlags.containsKey(simpleName.asString())
        } else {
            ktPropertySymbol.hasBackingField
        }
    }

    override fun isDelegated(): Boolean {
        return ktPropertySymbol.isDelegatedProperty
    }

    override fun findOverridee(): KSPropertyDeclaration? {
        return analyze {
            ktPropertySymbol.getDirectlyOverriddenSymbols().firstOrNull()
                ?.unwrapFakeOverrides?.toKSDeclaration() as? KSPropertyDeclaration
        }
    }

    override fun asMemberOf(containing: KSType): KSType {
        return ResolverAAImpl.instance.computeAsMemberOf(this, containing)
    }

    override val qualifiedName: KSName? by lazy {
        ktPropertySymbol.callableIdIfNonLocal?.asSingleFqName()?.asString()?.let { KSNameImpl.getCached(it) }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }

    override fun defer(): Restorable? {
        return ktPropertySymbol.defer(::getCached)
    }
}

internal fun KtAnnotationApplication.isUseSiteTargetAnnotation(): Boolean {
    return this.useSiteTarget?.let {
        it == AnnotationUseSiteTarget.PROPERTY_GETTER ||
            it == AnnotationUseSiteTarget.PROPERTY_SETTER ||
            it == AnnotationUseSiteTarget.SETTER_PARAMETER ||
            it == AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
    } ?: false
}

internal fun KtPropertySymbol.toModifiers(): Set<Modifier> {
    val result = mutableSetOf<Modifier>()
    if (visibility != JavaVisibilities.PackageVisibility) {
        result.add(visibility.toModifier())
    }
    if (isOverride) {
        result.add(Modifier.OVERRIDE)
    }
    if (isStatic) {
        result.add(Modifier.JAVA_STATIC)
        result.add(Modifier.FINAL)
    }
    // Analysis API returns open for static members which should be ignored.
    if (!isStatic || modality != Modality.OPEN) {
        result.add(modality.toModifier())
    }

    if (this is KtKotlinPropertySymbol) {
        if (isLateInit) {
            result.add(Modifier.LATEINIT)
        }
        if (isConst) {
            result.add(Modifier.CONST)
        }
    }
    return result
}
