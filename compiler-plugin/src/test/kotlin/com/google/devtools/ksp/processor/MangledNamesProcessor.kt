package com.google.devtools.ksp.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSTopDownVisitor

@KspExperimental
@Suppress("unused") // used by the test code
class MangledNamesProcessor : AbstractTestProcessor() {
    private val results = mutableListOf<String>()
    override fun toResult() = results

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val mangleSourceNames = mutableMapOf<String, String?>()
        resolver.getAllFiles().forEach {
            it.accept(MangledNamesVisitor(resolver), mangleSourceNames)
        }
        val mangledDependencyNames = LinkedHashMap<String, String?>()
        // also collect results from library dependencies to ensure we resolve module name property
        resolver.getClassDeclarationByName("libPackage.Foo")?.accept(
            MangledNamesVisitor(resolver), mangledDependencyNames
        )
        resolver.getClassDeclarationByName("libPackage.AbstractKotlinClass")?.accept(
            MangledNamesVisitor(resolver), mangledDependencyNames
        )
        resolver.getClassDeclarationByName("libPackage.MyInterface")?.accept(
            MangledNamesVisitor(resolver), mangledDependencyNames
        )
        results.addAll(
            mangleSourceNames.entries.map { (decl, name) ->
                "$decl -> $name"
            }
        )
        results.addAll(
            mangledDependencyNames.entries.map { (decl, name) ->
                "$decl -> $name"
            }
        )
        return emptyList()
    }

    private class MangledNamesVisitor(
        val resolver: Resolver
    ) : KSTopDownVisitor<MutableMap<String, String?>, Unit>() {
        override fun defaultHandler(node: KSNode, data: MutableMap<String, String?>) {
        }

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: MutableMap<String, String?>) {
            if (classDeclaration.modifiers.contains(Modifier.INLINE)) {
                // do not visit inline classes
                return
            }
            // put a header for readable output
            data[classDeclaration.qualifiedName!!.asString()] = "declarations"
            super.visitClassDeclaration(classDeclaration, data)
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: MutableMap<String, String?>) {
            if (function.simpleName.asString() in IGNORED_FUNCTIONS) return
            super.visitFunctionDeclaration(function, data)
            data[function.simpleName.asString()] = resolver.getJvmName(function)
        }

        override fun visitPropertyGetter(getter: KSPropertyGetter, data: MutableMap<String, String?>) {
            super.visitPropertyGetter(getter, data)
            data["get-${getter.receiver.simpleName.asString()}"] = resolver.getJvmName(getter)
        }

        override fun visitPropertySetter(setter: KSPropertySetter, data: MutableMap<String, String?>) {
            super.visitPropertySetter(setter, data)
            data["set-${setter.receiver.simpleName.asString()}"] = resolver.getJvmName(setter)
        }

        companion object {
            // do not report these functions as they are generated only in byte code and do not affect the test.
            val IGNORED_FUNCTIONS = listOf("equals", "hashCode", "toString")
        }
    }
}
