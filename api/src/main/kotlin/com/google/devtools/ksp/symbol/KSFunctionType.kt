package com.google.devtools.ksp.symbol

interface KSFunctionType {
    val returnType: KSType?
    val parametersTypes: List<KSType?>
    val typeParameters: List<KSTypeParameter>
}