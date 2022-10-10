package com.google.devtools.ksp.impl.symbol.kotlin.synthetic

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.name.ClassId

val ExtensionFunctionTypeAnnotation = KtAnnotationApplication(
    ClassId.fromString(ExtensionFunctionType::class.qualifiedName!!),
    null,
    null,
    emptyList()
)
