/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package com.google.devtools.ksp.symbol.impl.kotlin

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.getClassType
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.memoized
import com.google.devtools.ksp.processing.impl.KSTypeReferenceSyntheticImpl
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.*
import com.google.devtools.ksp.symbol.impl.binary.getAllFunctions
import com.google.devtools.ksp.symbol.impl.binary.getAllProperties
import com.google.devtools.ksp.symbol.impl.binary.sealedSubclassesSequence
import com.google.devtools.ksp.symbol.impl.synthetic.KSConstructorSyntheticImpl
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections

class KSClassDeclarationImpl private constructor(val ktClassOrObject: KtClassOrObject) :
    KSClassDeclaration,
    KSDeclarationImpl(ktClassOrObject),
    KSExpectActual by KSExpectActualImpl(ktClassOrObject) {
    companion object : KSObjectCache<KtClassOrObject, KSClassDeclarationImpl>() {
        fun getCached(ktClassOrObject: KtClassOrObject) =
            cache.getOrPut(ktClassOrObject) { KSClassDeclarationImpl(ktClassOrObject) }
    }

    override val classKind: ClassKind by lazy {
        ktClassOrObject.getClassType()
    }

    override val isCompanionObject by lazy {
        (ktClassOrObject is KtObjectDeclaration) && (ktClassOrObject.isCompanion())
    }

    override fun getSealedSubclasses(): Sequence<KSClassDeclaration> {
        return if (Modifier.SEALED in modifiers) {
            ResolverImpl.instance!!.incrementalContext.recordGetSealedSubclasses(this)
            descriptor.sealedSubclassesSequence()
        } else {
            emptySequence()
        }
    }

    override fun getAllFunctions(): Sequence<KSFunctionDeclaration> = descriptor.getAllFunctions()

    override fun getAllProperties(): Sequence<KSPropertyDeclaration> = descriptor.getAllProperties()

    override val declarations: Sequence<KSDeclaration> by lazy {
        val propertiesFromConstructor = primaryConstructor?.parameters
            ?.asSequence()
            ?.filter { it.isVar || it.isVal }
            ?.map { KSPropertyDeclarationParameterImpl.getCached((it as KSValueParameterImpl).ktParameter) }
            ?: emptySequence()
        var result = propertiesFromConstructor.plus(ktClassOrObject.declarations.asSequence().getKSDeclarations())
        primaryConstructor?.let { primaryConstructor: KSFunctionDeclaration ->
            // if primary constructor is from source, it won't show up in declarations
            // hence add it as well.
            if (primaryConstructor.origin == Origin.KOTLIN) {
                result = sequenceOf(primaryConstructor).plus(result)
            }
        }
        if (classKind != ClassKind.INTERFACE) {
            // check if we need to add a synthetic constructor
            val hasConstructor = result.any {
                it is KSFunctionDeclaration && it.isConstructor()
            }
            if (hasConstructor) {
                result.memoized()
            } else {
                (result + KSConstructorSyntheticImpl.getCached(this)).memoized()
            }
        } else {
            result.memoized()
        }
    }

    override val primaryConstructor: KSFunctionDeclaration? by lazy {
        ktClassOrObject.primaryConstructor?.let { KSFunctionDeclarationImpl.getCached(it) }
            ?: if ((classKind == ClassKind.CLASS || classKind == ClassKind.ENUM_CLASS) &&
                ktClassOrObject.declarations.none { it is KtSecondaryConstructor }
            )
                KSConstructorSyntheticImpl.getCached(this) else null
    }

    override val superTypes: Sequence<KSTypeReference> by lazy {
        val resolver = ResolverImpl.instance!!
        ktClassOrObject.superTypeListEntries
            .asSequence()
            .map { KSTypeReferenceImpl.getCached(it.typeReference!!) }
            .ifEmpty {
                sequenceOf(
                    KSTypeReferenceSyntheticImpl.getCached(
                        resolver.builtIns.anyType,
                        this
                    )
                )
            }
            .memoized()
    }

    private val descriptor: ClassDescriptor by lazy {
        (ResolverImpl.instance!!.resolveDeclaration(ktClassOrObject) as ClassDescriptor)
    }

    override fun asType(typeArguments: List<KSTypeArgument>): KSType {
        return descriptor.defaultType.replaceTypeArguments(typeArguments)?.let {
            getKSTypeCached(it, typeArguments)
        } ?: KSErrorType
    }

    override fun asStarProjectedType(): KSType {
        return getKSTypeCached(descriptor.defaultType.replaceArgumentsWithStarProjections())
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitClassDeclaration(this, data)
    }
}
