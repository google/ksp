package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.symbol.KSFunction
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter

class KSFunctionErrorImpl(
    private val declaration: KSFunctionDeclaration
) : KSFunction {
    override val isError: Boolean = true

    override val returnType: KSType
        get() = KSErrorType.fromReferenceBestEffort(declaration.returnType)

    override val parameterTypes: List<KSType?>
        get() = declaration.parameters.map {
            KSErrorType.fromReferenceBestEffort(it.type)
        }
    override val typeParameters: List<KSTypeParameter>
        get() = emptyList()

    override val extensionReceiverType: KSType?
        get() = declaration.extensionReceiver?.let {
            KSErrorType.fromReferenceBestEffort(it)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KSFunctionErrorImpl

        if (declaration != other.declaration) return false

        return true
    }

    override fun hashCode(): Int {
        return declaration.hashCode()
    }
}
