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

import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.findClosestOverridee
import com.google.devtools.ksp.symbol.impl.synthetic.KSPropertyGetterSyntheticImpl
import com.google.devtools.ksp.symbol.impl.synthetic.KSPropertySetterSyntheticImpl
import com.google.devtools.ksp.symbol.impl.toKSExpression
import com.google.devtools.ksp.symbol.impl.toKSPropertyDeclaration
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.js.translate.declaration.hasCustomGetter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration

class KSPropertyDeclarationImpl private constructor(val ktProperty: KtProperty) : KSPropertyDeclaration, KSDeclarationImpl(ktProperty),
    KSExpectActual by KSExpectActualImpl(ktProperty) {
    companion object : KSObjectCache<KtProperty, KSPropertyDeclarationImpl>() {
        fun getCached(ktProperty: KtProperty) = cache.getOrPut(ktProperty) { KSPropertyDeclarationImpl(ktProperty) }
    }

    override val extensionReceiver: KSTypeReference? by lazy {
        if (ktProperty.isExtensionDeclaration()) {
            KSTypeReferenceImpl.getCached(ktProperty.receiverTypeReference!!)
        } else {
            null
        }
    }

    override val isMutable: Boolean by lazy {
        ktProperty.isVar
    }

    private fun isInterfaceProperty(): Boolean {
        return (this.parentDeclaration as? KSClassDeclaration)?.classKind == ClassKind.INTERFACE
    }

    private fun shouldCreateSyntheticAccessor(): Boolean {
        return !this.isPrivate() && (
          !this.isLocal() && !this.modifiers.contains(Modifier.ABSTRACT)
            && !isInterfaceProperty()
            || (isInterfaceProperty() && ktProperty.accessors.isNotEmpty())
          )
    }

    override val getter: KSPropertyGetter? by lazy {
        if (!shouldCreateSyntheticAccessor()) {
            null
        } else {
            val getter = ktProperty.accessors.singleOrNull { it.isGetter }
            // FIXME: I don't think we should generate a getter without declaring it, because that's unnecessary. We just need to consider the same parsing as the source file.
//            if (getter != null) {
//                KSPropertyGetterImpl.getCached(getter)
//            } else {
//                KSPropertyGetterSyntheticImpl.getCached(this)
//            }
            getter?.let(KSPropertyGetterImpl::getCached)
        }
    }

    override val setter: KSPropertySetter? by lazy {
        if (!shouldCreateSyntheticAccessor() || !ktProperty.isVar) {
            null
        } else {
            val setter = ktProperty.accessors.singleOrNull { it.isSetter }
            // FIXME: I don't think we should generate a setter without declaring it, because that's unnecessary. We just need to consider the same parsing as the source file.
//            if (setter != null) {
//                KSPropertySetterImpl.getCached(setter)
//            } else {
//                KSPropertySetterSyntheticImpl.getCached(this)
//            }
            setter?.let(KSPropertySetterImpl::getCached)
        }
    }

    override val initializer: KSExpression? by lazy {
        if (isInterfaceProperty() || !isInitialized) {
            null
        } else {
            ktProperty.initializer.toKSExpression()
        }
    }

    override val delegate: KSExpression? by lazy {
        if (isInterfaceProperty() && !isDelegated) {
            null
        } else {
            ktProperty.delegateExpression.toKSExpression()
        }
    }

    override val type: KSTypeReference by lazy {
        if (ktProperty.typeReference != null) {
            KSTypeReferenceImpl.getCached(ktProperty.typeReference!!)
        } else {
            KSTypeReferenceDeferredImpl.getCached {
                val desc = ResolverImpl.instance.resolveDeclaration(ktProperty) as? VariableDescriptorWithAccessors
                if (desc == null) {
                    KSErrorType
                } else {
                    getKSTypeCached(desc.type)
                }
            }
        }
    }

    override val isDelegated: Boolean by lazy {
        ktProperty.hasDelegate()
    }

    override val isInitialized: Boolean by lazy {
        ktProperty.hasInitializer()
    }

    override val text: String by lazy {
        ktProperty.text
    }

    override fun findOverridee(): KSPropertyDeclaration? {
        val propertyDescriptor = ResolverImpl.instance.resolvePropertyDeclaration(this)
        return propertyDescriptor?.findClosestOverridee()?.toKSPropertyDeclaration()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }

    override fun toString(): String = text
}