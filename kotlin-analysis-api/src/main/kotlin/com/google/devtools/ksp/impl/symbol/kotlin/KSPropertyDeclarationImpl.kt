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

import com.google.devtools.ksp.BinaryClassInfoCache
import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.impl.ResolverAAImpl
import com.google.devtools.ksp.processing.impl.KSNameImpl
import com.google.devtools.ksp.symbol.*
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl

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
            .map { KSAnnotationImpl.getCached(it) }
            .filterNot { valueParameterAnnotation ->
                valueParameterAnnotation.annotationType.resolve().declaration.annotations.any { metaAnnotation ->
                    metaAnnotation.annotationType.resolve().declaration.qualifiedName
                        ?.asString() == "kotlin.annotation.Target" &&
                        (metaAnnotation.arguments.singleOrNull()?.value as? ArrayList<*>)?.any {
                        (it as? KSClassDeclaration)?.qualifiedName
                            ?.asString() == "kotlin.annotation.AnnotationTarget.VALUE_PARAMETER"
                    } ?: false
                }
            }
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
        ktPropertySymbol.receiverType?.let { KSTypeReferenceImpl.getCached(it, this@KSPropertyDeclarationImpl) }
    }

    override val type: KSTypeReference by lazy {
        KSTypeReferenceImpl.getCached(ktPropertySymbol.returnType, this@KSPropertyDeclarationImpl)
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
        TODO("Not yet implemented")
    }

    override fun asMemberOf(containing: KSType): KSType {
        TODO("Not yet implemented")
    }

    override val qualifiedName: KSName? by lazy {
        val name = ktPropertySymbol.callableIdIfNonLocal?.asSingleFqName()?.asString()
            ?: ("${parentDeclaration?.qualifiedName?.asString()}.${this.simpleName.asString()}")
        KSNameImpl.getCached(name)
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
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
    result.add(visibility.toModifier())
    if (isOverride) {
        result.add(Modifier.OVERRIDE)
    }
    if (isStatic) {
        Modifier.JAVA_STATIC
    }
    result.add(modality.toModifier())

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
