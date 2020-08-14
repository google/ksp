/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.KSObjectCache
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSNameImpl
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSTypeImpl
import org.jetbrains.kotlin.ksp.symbol.impl.replaceTypeArguments
import org.jetbrains.kotlin.ksp.symbol.impl.toKSModifiers
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.descriptors.ClassKind as KtClassKind

class KSClassDeclarationDescriptorImpl private constructor(val descriptor: ClassDescriptor) : KSClassDeclaration,
    KSDeclarationDescriptorImpl(descriptor),
    KSExpectActual by KSExpectActualDescriptorImpl(descriptor) {
    companion object : KSObjectCache<ClassDescriptor, KSClassDeclarationDescriptorImpl>() {
        fun getCached(descriptor: ClassDescriptor) = cache.getOrPut(descriptor) { KSClassDeclarationDescriptorImpl(descriptor) }
    }

    override val origin by lazy {
        if (this.parentDeclaration?.origin != Origin.CLASS) Origin.SYNTHETIC else Origin.CLASS
    }

    override val location: Location = NonExistLocation

    override val annotations: List<KSAnnotation> by lazy {
        descriptor.annotations.map { KSAnnotationDescriptorImpl.getCached(it) }
    }

    override val classKind: ClassKind by lazy {
        when (descriptor.kind) {
            KtClassKind.INTERFACE -> ClassKind.INTERFACE
            KtClassKind.CLASS -> ClassKind.CLASS
            KtClassKind.OBJECT -> ClassKind.OBJECT
            KtClassKind.ENUM_CLASS -> ClassKind.ENUM_CLASS
            KtClassKind.ENUM_ENTRY -> ClassKind.ENUM_ENTRY
            KtClassKind.ANNOTATION_CLASS -> ClassKind.ANNOTATION_CLASS
        }
    }

    override val containingFile: KSFile? = null

    override val isCompanionObject by lazy {
        descriptor.isCompanionObject
    }

    override fun getAllFunctions(): List<KSFunctionDeclaration> {
        return descriptor.unsubstitutedMemberScope.getDescriptorsFiltered(DescriptorKindFilter.FUNCTIONS).toList()
            .filter { (it as FunctionDescriptor).visibility != Visibilities.INVISIBLE_FAKE }
            .map { KSFunctionDeclarationDescriptorImpl.getCached(it as FunctionDescriptor) }
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        val containingDescriptor = descriptor.parents.first()
        when (containingDescriptor) {
            is ClassDescriptor -> KSClassDeclarationDescriptorImpl.getCached(
                containingDescriptor
            )
            is FunctionDescriptor -> KSFunctionDeclarationDescriptorImpl.getCached(
                containingDescriptor
            )
            else -> null
        } as KSDeclaration?
    }

    override val primaryConstructor: KSFunctionDeclaration? by lazy {
        descriptor.unsubstitutedPrimaryConstructor?.let { KSFunctionDeclarationDescriptorImpl.getCached(it) }
    }

    override val qualifiedName: KSName by lazy {
        KSNameImpl.getCached(descriptor.fqNameSafe.asString())
    }

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(descriptor.name.asString())
    }

    override val superTypes: List<KSTypeReference> by lazy {
        descriptor.defaultType.constructor.supertypes.map {
            KSTypeReferenceDescriptorImpl.getCached(
                it
            )
        }
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        descriptor.declaredTypeParameters.map {
            KSTypeParameterDescriptorImpl.getCached(
                it
            )
        }
    }

    override val declarations: List<KSDeclaration> by lazy {
        listOf(descriptor.unsubstitutedMemberScope.getDescriptorsFiltered(), descriptor.staticScope.getDescriptorsFiltered()).flatten()
            .filter {
                it is MemberDescriptor
                        && it.visibility != Visibilities.INHERITED
                        && it.visibility != Visibilities.INVISIBLE_FAKE
            }
            .map {
                when (it) {
                    is PropertyDescriptor -> KSPropertyDeclarationDescriptorImpl.getCached(it)
                    is FunctionDescriptor -> KSFunctionDeclarationDescriptorImpl.getCached(it)
                    is ClassDescriptor -> getCached(it)
                    else -> throw IllegalStateException()
                }
            }
    }

    override val modifiers: Set<Modifier> by lazy {
        val modifiers = mutableSetOf<Modifier>()
        modifiers.addAll(descriptor.toKSModifiers())
        if (descriptor.isData) {
            modifiers.add(Modifier.DATA)
        }
        if (descriptor.isInline) {
            modifiers.add(Modifier.INLINE)
        }
        if (descriptor.kind == KtClassKind.ANNOTATION_CLASS) {
            modifiers.add(Modifier.ANNOTATION)
        }
        modifiers
    }

    override fun asType(typeArguments: List<KSTypeArgument>): KSType =
        KSTypeImpl.getCached(descriptor.defaultType.replaceTypeArguments(typeArguments), typeArguments)

    override fun asStarProjectedType(): KSType {
        return KSTypeImpl.getCached(descriptor.defaultType.replaceArgumentsWithStarProjections())
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitClassDeclaration(this, data)
    }
}