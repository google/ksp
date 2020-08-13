/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.ksp.isLocal
import org.jetbrains.kotlin.ksp.isOpen
import org.jetbrains.kotlin.ksp.isVisibleFrom
import org.jetbrains.kotlin.ksp.processing.impl.ResolverImpl
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.*
import org.jetbrains.kotlin.ksp.symbol.impl.binary.KSPropertyGetterDescriptorImpl
import org.jetbrains.kotlin.ksp.symbol.impl.binary.KSPropertySetterDescriptorImpl
import org.jetbrains.kotlin.ksp.symbol.impl.synthetic.KSPropertyGetterSyntheticImpl
import org.jetbrains.kotlin.ksp.symbol.impl.synthetic.KSPropertySetterSyntheticImpl
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.resolve.OverridingUtil

class KSPropertyDeclarationImpl private constructor(val ktProperty: KtProperty) : KSPropertyDeclaration, KSDeclarationImpl(),
    KSExpectActual by KSExpectActualImpl(ktProperty) {
    companion object : KSObjectCache<KtProperty, KSPropertyDeclarationImpl>() {
        fun getCached(ktProperty: KtProperty) = cache.getOrPut(ktProperty) { KSPropertyDeclarationImpl(ktProperty) }
    }

    override val origin = Origin.KOTLIN

    override val location: Location by lazy {
        ktProperty.toLocation()
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
        if (this.isLocal()) {
            null
        } else {
            val getter = ktProperty.accessors.filter { it.isGetter }.singleOrNull()
            if (getter != null) {
                KSPropertyGetterImpl.getCached(getter)
            } else {
                KSPropertyGetterSyntheticImpl.getCached(this)
            }
        }
    }

    override val setter: KSPropertySetter? by lazy {
        if (this.isLocal() || !ktProperty.isVar) {
            null
        } else {
            val setter = ktProperty.accessors.filter { it.isSetter }.singleOrNull()
            if (setter != null) {
                KSPropertySetterImpl.getCached(setter)
            } else {
                KSPropertySetterSyntheticImpl.getCached(this)
            }
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
            KSTypeReferenceDeferredImpl.getCached {
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

    override fun overrides(overridee: KSPropertyDeclaration): Boolean {
        if (!this.modifiers.contains(Modifier.OVERRIDE))
            return false
        if (!overridee.isOpen())
            return false
        if (!overridee.isVisibleFrom(this))
            return false
        if (overridee.origin == Origin.JAVA)
            return false
        val superDescriptor = ResolverImpl.instance.resolvePropertyDeclaration(overridee) ?: return false
        val subDescriptor = ResolverImpl.instance.resolveDeclaration(ktProperty) as PropertyDescriptor
        return OverridingUtil.DEFAULT.isOverridableBy(
                superDescriptor, subDescriptor, null
        ).result == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }
}