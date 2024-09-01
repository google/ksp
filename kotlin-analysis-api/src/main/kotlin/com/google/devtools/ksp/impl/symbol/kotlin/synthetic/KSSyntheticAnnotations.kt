package com.google.devtools.ksp.impl.symbol.kotlin.synthetic

import com.google.devtools.ksp.impl.ResolverAAImpl
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaAnnotationImpl
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinAlwaysAccessibleLifetimeToken
import org.jetbrains.kotlin.name.ClassId

@OptIn(KaImplementationDetail::class)
fun getExtensionFunctionTypeAnnotation() = KaAnnotationImpl(
    ClassId.fromString(ExtensionFunctionType::class.qualifiedName!!),
    null,
    null,
    lazyOf(emptyList()),
    null,
    KotlinAlwaysAccessibleLifetimeToken(ResolverAAImpl.ktModule.project)
)
