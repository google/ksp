/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.ksp.processing.impl.ResolverImpl
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.*
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.resolve.calls.inference.returnTypeOrNothing

class KSPropertyDeclarationImpl(val ktProperty: KtProperty) : KSPropertyDeclaration {
    companion object {
        private val cache = mutableMapOf<KtProperty, KSPropertyDeclarationImpl>()

        fun getCached(ktProperty: KtProperty) = cache.getOrPut(ktProperty) { KSPropertyDeclarationImpl(ktProperty) }
    }

    override val annotations: List<KSAnnotation> by lazy {
        ktProperty.annotationEntries.map { KSAnnotationImpl.getCached(it) }
    }

    override val containingFile: KSFile by lazy {
        KSFileImpl.getCached(ktProperty.containingKtFile)
    }

    override val extensionReceiver: KSTypeReference? by lazy {
        if (ktProperty.isExtensionDeclaration()) {
            KSTypeReferenceImpl.getCached(ktProperty.receiverTypeReference!!)
        } else {
            null
        }
    }

    override val getter: KSPropertyGetter? by lazy {
        val getter = ktProperty.accessors.filter { it.isGetter }.singleOrNull()
        if (getter != null) {
            KSPropertyGetterImpl.getCached(getter)
        } else {
            null
        }
    }

    override val setter: KSPropertySetter? by lazy {
        val setter = ktProperty.accessors.filter { it.isSetter }.singleOrNull()
        if (setter != null) {
            KSPropertySetterImpl.getCached(setter)
        } else {
            null
        }
    }

    override val modifiers: Set<Modifier> by lazy {
        ktProperty.toKSModifiers()
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        ktProperty.findParentDeclaration()
    }

    override val qualifiedName: KSName? by lazy {
        ktProperty.fqName?.asString()?.let { KSNameImpl.getCached(it) }
    }

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(ktProperty.name!!)
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        ktProperty.typeParameters.map { KSTypeParameterImpl.getCached(it, ktProperty) }
    }

    override val type: KSTypeReference? by lazy {
        if (ktProperty.typeReference != null) {
            KSTypeReferenceImpl.getCached(ktProperty.typeReference!!)
        } else {
            KSTypeReferenceDeferredImpl {
                val desc = ResolverImpl.instance.resolveDeclaration(ktProperty) as? VariableDescriptorWithAccessors
                if (desc == null) {
                    // TODO: Add error type to allow forward reference being taken care of.
                    null
                } else {
                    KSTypeImpl.getCached(desc.type)
                }
            }
        }
    }

    override fun isDelegated(): Boolean = ktProperty.hasDelegate()


    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }
}