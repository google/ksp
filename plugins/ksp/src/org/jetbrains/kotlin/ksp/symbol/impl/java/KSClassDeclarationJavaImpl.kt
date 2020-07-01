/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.java

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ksp.processing.impl.ResolverImpl
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.binary.KSFunctionDeclarationDescriptorImpl
import org.jetbrains.kotlin.ksp.symbol.impl.findParentDeclaration
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSNameImpl
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSTypeImpl
import org.jetbrains.kotlin.ksp.symbol.impl.replaceTypeArguments
import org.jetbrains.kotlin.ksp.symbol.impl.toKSFunctionDeclaration
import org.jetbrains.kotlin.ksp.symbol.impl.toKSModifiers
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections

class KSClassDeclarationJavaImpl(val psi: PsiClass) : KSClassDeclaration {
    companion object {
        private val cache = mutableMapOf<PsiClass, KSClassDeclarationJavaImpl>()

        fun getCached(psi: PsiClass) = cache.getOrPut(psi) { KSClassDeclarationJavaImpl(psi) }
    }

    override val origin = Origin.JAVA

    override val annotations: List<KSAnnotation> by lazy {
        psi.annotations.map { KSAnnotationJavaImpl.getCached(it) }
    }

    override val classKind: ClassKind by lazy {
        when {
            psi.isAnnotationType || psi.isInterface -> ClassKind.INTERFACE
            psi.isEnum -> ClassKind.ENUM
            else -> ClassKind.CLASS
        }
    }

    override val containingFile: KSFile? by lazy {
        KSFileJavaImpl.getCached(psi.containingFile as PsiJavaFile)
    }

    override val isCompanionObject = false

    // Could the resolution ever fail?
    private val descriptor: ClassDescriptor? by lazy {
        ResolverImpl.javaDescriptorResolver.resolveClass(JavaClassImpl(psi))
    }

    override fun getAllFunctions(): List<KSFunctionDeclaration> {
        return descriptor?.let {
            it.unsubstitutedMemberScope.getDescriptorsFiltered(DescriptorKindFilter.FUNCTIONS)
                .toList()
                .filter { (it as FunctionDescriptor).visibility != Visibilities.INVISIBLE_FAKE }
                .map { (it as FunctionDescriptor).toKSFunctionDeclaration() }
        } ?: emptyList()
    }

    override val declarations: List<KSDeclaration> by lazy {
        psi.fields.map { KSPropertyDeclarationJavaImpl.getCached(it) } +
                psi.innerClasses.map { KSClassDeclarationJavaImpl.getCached(it) } +
                psi.constructors.map { KSFunctionDeclarationJavaImpl.getCached(it) } +
                psi.methods.map { KSFunctionDeclarationJavaImpl.getCached(it) }
    }

    override val modifiers: Set<Modifier> by lazy {
        val modifiers = mutableSetOf<Modifier>()
        modifiers.addAll(psi.toKSModifiers())
        if (psi.isAnnotationType) {
            modifiers.add(Modifier.ANNOTATION)
        }
        if (psi.isEnum) {
            modifiers.add(Modifier.ENUM)
        }
        modifiers
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        psi.findParentDeclaration()
    }

    override val primaryConstructor: KSFunctionDeclaration? by lazy {
        if (psi.constructors.isNotEmpty()) {
            KSFunctionDeclarationJavaImpl.getCached(psi.constructors.first())
        } else {
            null
        }
    }

    override val qualifiedName: KSName by lazy {
        KSNameImpl.getCached(psi.qualifiedName!!)
    }

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(psi.name!!)
    }

    override val superTypes: List<KSTypeReference> by lazy {
        psi.superTypes.map { KSTypeReferenceJavaImpl.getCached(it) }
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        psi.typeParameters.map { KSTypeParameterJavaImpl.getCached(it) }
    }

    override fun asType(typeArguments: List<KSTypeArgument>): KSType {
        return KSTypeImpl.getCached(descriptor!!.defaultType.replaceTypeArguments(typeArguments), typeArguments)
    }

    override fun asStarProjectedType(): KSType {
        return KSTypeImpl.getCached(descriptor!!.defaultType.replaceArgumentsWithStarProjections())
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitClassDeclaration(this, data)
    }
}