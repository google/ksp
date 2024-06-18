package com.google.devtools.ksp.impl.symbol.kotlin.synthetic

import com.google.devtools.ksp.impl.ResolverAAImpl
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaAnnotationImpl
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinAlwaysAccessibleLifetimeToken
import org.jetbrains.kotlin.name.ClassId

fun getExtensionFunctionTypeAnnotation(index: Int) = KaAnnotationImpl(
    ClassId.fromString(ExtensionFunctionType::class.qualifiedName!!),
    null,
    null,
    false,
    lazyOf(emptyList()),
    index,
    null,
    KotlinAlwaysAccessibleLifetimeToken(ResolverAAImpl.ktModule.project!!)
)
