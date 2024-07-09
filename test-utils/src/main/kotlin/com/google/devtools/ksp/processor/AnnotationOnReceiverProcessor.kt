package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class AnnotationOnReceiverProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        listOf("Test", "TestLib").forEach {
            resolver.getClassDeclarationByName(it)!!.let { cls ->
                cls.getDeclaredFunctions().forEach { method ->
                    method.extensionReceiver.let { receiver ->
                        if (receiver != null) {
                            results.add(
                                receiver.annotations.map {
                                    it.annotationType.toString() + it.arguments.toString()
                                }.joinToString()
                            )
                        }
                    }
                }
                cls.getDeclaredProperties().forEach { prop ->
                    prop.extensionReceiver.let { receiver ->
                        if (receiver != null) {
                            results.add(
                                receiver.annotations.map {
                                    it.annotationType.toString() + it.arguments.toString()
                                }.joinToString()
                            )
                        }
                    }
                }
            }
        }
        return emptyList()
    }
}
