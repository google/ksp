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

import com.google.devtools.ksp.processing.impl.KSObjectCache
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.*
import com.google.devtools.ksp.symbol.impl.binary.KSPropertyGetterDescriptorImpl
import com.google.devtools.ksp.symbol.impl.binary.KSPropertySetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.resolve.BindingContext

class KSPropertyDeclarationImpl private constructor(val ktProperty: KtProperty) :
    KSPropertyDeclaration,
    KSDeclarationImpl(ktProperty),
    KSExpectActual by KSExpectActualImpl(ktProperty) {
    companion object : KSObjectCache<KtProperty, KSPropertyDeclarationImpl>() {
        fun getCached(ktProperty: KtProperty) = cache.getOrPut(ktProperty) { KSPropertyDeclarationImpl(ktProperty) }
    }

    private val propertyDescriptor by lazy {
        ResolverImpl.instance!!.resolveDeclaration(ktProperty) as? PropertyDescriptor
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        ktProperty.filterUseSiteTargetAnnotations().map { KSAnnotationImpl.getCached(it) }
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

    override val hasBackingField: Boolean by lazy {
        // taken from: https://github.com/JetBrains/kotlin/blob/master/compiler/light-classes/src/org/jetbrains/kotlin/asJava/classes/ultraLightMembersCreator.kt#L104
        when {
            ktProperty.initializer != null -> true
            ktProperty.hasModifier(KtTokens.LATEINIT_KEYWORD) -> true
            else -> {
                val context = ResolverImpl.instance!!.bindingTrace.bindingContext
                val descriptor = ResolverImpl.instance!!.resolveDeclaration(ktProperty)
                descriptor is PropertyDescriptor && context[BindingContext.BACKING_FIELD_REQUIRED, descriptor] == true
            }
        }
    }

    override val getter: KSPropertyGetter? by lazy {
        ktProperty.getter?.let {
            KSPropertyGetterImpl.getCached(it)
        } ?: propertyDescriptor?.getter?.let {
            KSPropertyGetterDescriptorImpl.getCached(it)
        }
    }

    override val setter: KSPropertySetter? by lazy {
        ktProperty.setter?.let {
            KSPropertySetterImpl.getCached(it)
        } ?: propertyDescriptor?.setter?.let {
            KSPropertySetterDescriptorImpl.getCached(it)
        }
    }

    override val type: KSTypeReference by lazy {
        if (ktProperty.typeReference != null) {
            KSTypeReferenceImpl.getCached(ktProperty.typeReference!!)
        } else {
            KSTypeReferenceDeferredImpl.getCached(this) {
                val desc = propertyDescriptor as? VariableDescriptorWithAccessors
                if (desc == null) {
                    KSErrorType()
                } else {
                    getKSTypeCached(desc.type)
                }
            }
        }
    }

    override fun isDelegated(): Boolean = ktProperty.hasDelegate()

    override fun findOverridee(): KSPropertyDeclaration? {
        return propertyDescriptor?.findClosestOverridee()?.toKSPropertyDeclaration()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }

    override fun asMemberOf(containing: KSType): KSType =
        ResolverImpl.instance!!.asMemberOf(this, containing)
}

internal fun KtAnnotated.filterUseSiteTargetAnnotations(): Sequence<KtAnnotationEntry> {
    return this.annotationEntries.asSequence().filter { property ->
        property.useSiteTarget?.getAnnotationUseSiteTarget()?.let {
            it != AnnotationUseSiteTarget.PROPERTY_GETTER && it != AnnotationUseSiteTarget.PROPERTY_SETTER &&
                it != AnnotationUseSiteTarget.SETTER_PARAMETER && it != AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
        } ?: true
    }
}
