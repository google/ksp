package com.google.devtools.ksp.symbol.impl.kotlin

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSFunctionType
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter

class KSFunctionTypeDeclarationImpl(
    private val declaration: KSFunctionDeclaration
) : KSFunctionType {
    override val returnType: KSType? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        declaration.returnType?.resolve()
    }

    override val parametersTypes: List<KSType?> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        declaration.parameters.map {
            it.type?.resolve()
        }
    }
    override val typeParameters: List<KSTypeParameter>
        get() = declaration.typeParameters

    override val extensionReceiverType: KSType?
        get() = declaration.extensionReceiver?.resolve()
}