package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*

class KSAnnotationImpl(private val annotationApplication: KtAnnotationApplication) : KSAnnotation {
    override val annotationType: KSTypeReference
        get() = TODO("Not yet implemented")
    override val arguments: List<KSValueArgument> by lazy {
        annotationApplication.arguments.map { KSValueArgumentImpl(it) }
    }
    override val shortName: KSName
        get() = TODO("Not yet implemented")
    override val useSiteTarget: AnnotationUseSiteTarget? by lazy {
        when (annotationApplication.useSiteTarget) {
            null -> null
            FILE -> AnnotationUseSiteTarget.FILE
            PROPERTY -> AnnotationUseSiteTarget.PROPERTY
            FIELD -> AnnotationUseSiteTarget.FIELD
            PROPERTY_GETTER -> AnnotationUseSiteTarget.GET
            PROPERTY_SETTER -> AnnotationUseSiteTarget.SET
            RECEIVER -> AnnotationUseSiteTarget.RECEIVER
            CONSTRUCTOR_PARAMETER -> AnnotationUseSiteTarget.PARAM
            SETTER_PARAMETER -> AnnotationUseSiteTarget.SETPARAM
            PROPERTY_DELEGATE_FIELD -> AnnotationUseSiteTarget.DELEGATE

        }
    }

    override val origin: Origin = Origin.KOTLIN

    override val location: Location by lazy {
        annotationApplication.psi?.toLocation() ?: NonExistLocation
    }

    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnnotation(this, data)
    }
}
