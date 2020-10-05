package com.google.devtools.ksp.symbol.impl.kotlin

import com.google.devtools.ksp.symbol.KSFunctionType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor

class KSFunctionTypeImpl(val descriptor: FunctionDescriptor) : KSFunctionType {
    override val returnType by lazy(LazyThreadSafetyMode.PUBLICATION) {
        descriptor.returnType?.let {
            KSTypeImpl.getCached(it)
        }
    }
}