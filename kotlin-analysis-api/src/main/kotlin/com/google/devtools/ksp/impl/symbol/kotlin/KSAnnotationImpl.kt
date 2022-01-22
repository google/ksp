package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Origin
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication

class KSAnnotationImpl(private val annotationApplication: KtAnnotationApplication) : KSAnnotation {
    override val annotationType: KSTypeReference
        get() = TODO("Not yet implemented")
    override val arguments: List<KSValueArgument> by lazy {
        annotationApplication.arguments.map { KSValueArgumentImpl(it) }
    }
    override val shortName: KSName
        get() = TODO("Not yet implemented")
    override val useSiteTarget: AnnotationUseSiteTarget?
        get() = TODO("Not yet implemented")
    override val origin: Origin = Origin.KOTLIN

    override val location: Location
        get() = TODO("Not yet implemented")
    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnnotation(this, data)
    }
}
