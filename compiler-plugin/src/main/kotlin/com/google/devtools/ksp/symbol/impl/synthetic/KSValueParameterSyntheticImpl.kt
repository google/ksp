package com.google.devtools.ksp.symbol.impl.synthetic

import com.google.devtools.ksp.ExceptionMessage
import com.google.devtools.ksp.processing.impl.findAnnotationFromUseSiteTarget
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.NonExistLocation
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.binary.KSAnnotationDescriptorImpl
import com.google.devtools.ksp.symbol.impl.binary.KSTypeReferenceDescriptorImpl
import com.google.devtools.ksp.symbol.impl.kotlin.KSNameImpl
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.components.isVararg

class KSValueParameterSyntheticImpl(val owner: KSAnnotated?, resolve: () -> ValueParameterDescriptor?) :
    KSValueParameter {

    companion object :
        KSObjectCache<Pair<KSAnnotated?, () -> ValueParameterDescriptor?>, KSValueParameterSyntheticImpl>() {
        fun getCached(owner: KSAnnotated? = null, resolve: () -> ValueParameterDescriptor?) =
            KSValueParameterSyntheticImpl.cache.getOrPut(Pair(owner, resolve)) {
                KSValueParameterSyntheticImpl(owner, resolve)
            }
    }

    private val descriptor by lazy {
        resolve() ?: throw IllegalStateException("Failed to resolve for synthetic value parameter, $ExceptionMessage")
    }

    override val name: KSName? by lazy {
        KSNameImpl.getCached(descriptor.name.asString())
    }

    override val type: KSTypeReference by lazy {
        KSTypeReferenceDescriptorImpl.getCached(descriptor.type, origin, this)
    }

    override val isVararg: Boolean = descriptor.isVararg

    override val isNoInline: Boolean = descriptor.isNoinline

    override val isCrossInline: Boolean = descriptor.isCrossinline

    override val isVal: Boolean = !descriptor.isVar

    override val isVar: Boolean = descriptor.isVar

    override val hasDefault: Boolean = descriptor.hasDefaultValue()

    override val annotations: Sequence<KSAnnotation> by lazy {
        descriptor.annotations.asSequence()
            .map { KSAnnotationDescriptorImpl.getCached(it, this) }.plus(this.findAnnotationFromUseSiteTarget())
    }

    override val origin: Origin = Origin.SYNTHETIC

    override val location: Location = NonExistLocation

    override val parent: KSNode? = owner

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueParameter(this, data)
    }

    override fun toString(): String {
        return name?.asString() ?: "_"
    }
}
