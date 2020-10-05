package com.google.devtools.ksp.symbol.impl.kotlin

import com.google.devtools.ksp.symbol.KSFunctionType
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.impl.binary.KSTypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.returnTypeOrNothing

class KSFunctionTypeImpl(val descriptor: CallableDescriptor) : KSFunctionType {
    override val returnType by lazy(LazyThreadSafetyMode.PUBLICATION) {
        descriptor.returnType?.let(::getKSTypeCached)
    }
    override val parametersTypes: List<KSType> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        descriptor.valueParameters.map {
            getKSTypeCached(it.type)
        }
    }
    override val typeParameters: List<KSTypeParameter> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        descriptor.typeParameters.map {
            KSTypeParameterDescriptorImpl.getCached(it)
        }
    }
}