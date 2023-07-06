package com.google.devtools.ksp.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSVisitorVoid

class PackageAnnotationProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()
    override fun toResult(): List<String> {
        return result
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getAllFiles().forEach {
            it.accept(
                object : KSVisitorVoid() {
                    override fun visitFile(file: KSFile, data: Unit) {
                        result.add(
                            "${file.fileName}:${resolver.getPackageAnnotations(file.packageName.asString())
                                .joinToString { it.toString() }}"
                        )
                    }
                },
                Unit
            )
        }
        result.sort()
        result.add(resolver.getPackagesWithAnnotation("PackageAnnotation").joinToString())
        return emptyList()
    }
}
