/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ksp.processing.impl.ResolverImpl
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections

class KSClassDeclarationImpl private constructor(val ktClassOrObject: KtClassOrObject) : KSClassDeclaration,
    KSExpectActual by KSExpectActualImpl(ktClassOrObject) {
    companion object : KSObjectCache<KtClassOrObject, KSClassDeclarationImpl>() {
        fun getCached(ktClassOrObject: KtClassOrObject) = cache.getOrPut(ktClassOrObject) { KSClassDeclarationImpl(ktClassOrObject) }
    }

    override val origin = Origin.KOTLIN

    override val location: Location by lazy {
        ktClassOrObject.toLocation()
    }

    override val annotations: List<KSAnnotation> by lazy {
        ktClassOrObject.annotationEntries.map { KSAnnotationImpl.getCached(it) }
    }

    override val classKind: ClassKind by lazy {
        ktClassOrObject.getClassType()
    }

    override val containingFile: KSFile by lazy {
        KSFileImpl.getCached(ktClassOrObject.containingKtFile)
    }

    override val isCompanionObject by lazy {
        (ktClassOrObject is KtObjectDeclaration) && (ktClassOrObject.isCompanion())
    }

    override fun getAllFunctions(): List<KSFunctionDeclaration> {
        return descriptor.unsubstitutedMemberScope.getDescriptorsFiltered(DescriptorKindFilter.FUNCTIONS).toList()
            .filter { (it as FunctionDescriptor).visibility != Visibilities.INVISIBLE_FAKE }
            .map { (it as FunctionDescriptor).toKSFunctionDeclaration() }
    }

    override val declarations: List<KSDeclaration> by lazy {
        val propertiesFromConstructor = primaryConstructor?.parameters
            ?.filter { it.isVar || it.isVal }
            ?.map { KSPropertyDeclarationParameterImpl.getCached((it as KSVariableParameterImpl).ktParameter) } ?: emptyList()
        val result = ktClassOrObject.declarations.getKSDeclarations().toMutableList()
        result.addAll(propertiesFromConstructor)
        result
    }

    override val modifiers: Set<Modifier> by lazy {
        ktClassOrObject.toKSModifiers()
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        ktClassOrObject.findParentDeclaration()
    }

    override val primaryConstructor: KSFunctionDeclaration? by lazy {
        ktClassOrObject.primaryConstructor?.let { KSFunctionDeclarationImpl.getCached(it) }
    }

    override val qualifiedName: KSName by lazy {
        if (ktClassOrObject.fqName == null) {
            KSNameImpl.getCached("")
        } else {
            KSNameImpl.getCached(ktClassOrObject.fqName!!.asString())
        }
    }

    override val simpleName: KSName by lazy {
        if (ktClassOrObject.name == null) {
            KSNameImpl.getCached("")
        } else {
            KSNameImpl.getCached(ktClassOrObject.name!!)
        }
    }

    override val superTypes: List<KSTypeReference> by lazy {
        ktClassOrObject.superTypeListEntries.map { KSTypeReferenceImpl.getCached(it.typeReference!!) }
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        ktClassOrObject.typeParameters.map {
            KSTypeParameterImpl.getCached(
                it,
                ktClassOrObject
            )
        }
    }

    private val descriptor: ClassDescriptor by lazy {
        (ResolverImpl.instance.resolveDeclaration(ktClassOrObject) as ClassDescriptor)
    }

    override fun asType(typeArguments: List<KSTypeArgument>): KSType {
        return KSTypeImpl.getCached(descriptor.defaultType.replaceTypeArguments(typeArguments), typeArguments)
    }

    override fun asStarProjectedType(): KSType {
        return KSTypeImpl.getCached(descriptor.defaultType.replaceArgumentsWithStarProjections())
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitClassDeclaration(this, data)
    }
}
