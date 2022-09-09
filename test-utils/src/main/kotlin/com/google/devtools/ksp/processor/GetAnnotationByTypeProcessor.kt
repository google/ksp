package com.google.devtools.ksp.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

annotation class KotlinAnnotationWithInnerDefaults(
    val innerAnnotationVal: InnerAnnotation = InnerAnnotation(innerAnnotationDefault = 7)
) {
    annotation class InnerAnnotation(
        val innerAnnotationDefault: Int,
        val moreInnerAnnotation: MoreInnerAnnotation = MoreInnerAnnotation("OK")
    ) {
        annotation class MoreInnerAnnotation(val moreInnerAnnotationDefault: String)
    }
}

class GetAnnotationByTypeProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    private val annotationKClass = KotlinAnnotationWithInnerDefaults::class

    override fun toResult(): List<String> {
        return results
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val decl = resolver.getAllFiles().single().declarations
            .single { it.simpleName.asString() == "A" } as KSClassDeclaration
        val anno = decl.getAnnotationsByType(annotationKClass).first()
        results.add(anno.innerAnnotationVal.toString())
        return emptyList()
    }
}
