package com.google.devtools.ksp.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*

@KspExperimental
class DeclarationOrderProcessor : AbstractTestProcessor() {
    private val result = mutableListOf<String>()
    override fun toResult() = result

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val classNames = listOf(
            "lib.KotlinClass", "lib.JavaClass",
            "KotlinClass", "JavaClass"
        )
        val companionClsses = listOf("KotlinCompanion", "lib.KotlinCompanion")
        val containers = classNames.map {
            checkNotNull(resolver.getClassDeclarationByName(it)) {
                "cannot find $it"
            }
        } + companionClsses.map {
            checkNotNull(resolver.getClassDeclarationByName(it)) {
                "cannot find $it"
            }.declarations.single {
                it is KSClassDeclaration && it.isCompanionObject
            } as KSClassDeclaration
        }
        containers.forEach { klass ->
            result.add(klass.qualifiedName!!.asString())
            result.addAll(
                resolver.getDeclarationsInSourceOrder(klass).filterIsInstance<KSPropertyDeclaration>().map {
                    it.toSignature(resolver)
                }
            )
            result.addAll(
                resolver.getDeclarationsInSourceOrder(klass).filterIsInstance<KSFunctionDeclaration>()
                    .filter { !it.isConstructor() }.map {
                        it.toSignature(resolver)
                    }
            )
        }
        result.addAll(
            resolver.getDeclarationsInSourceOrder(resolver.getClassDeclarationByName("kotlin.Any")!!).map {
                it.toSignature(resolver)
            }
        )
        return emptyList()
    }

    private fun KSDeclaration.toSignature(
        resolver: Resolver
    ) = "${simpleName.asString()}:${resolver.mapToJvmSignature(this)}"
}
