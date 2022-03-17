package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class HelloWorldProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val fooKt = resolver.getAllFiles().single()
        results.add(fooKt.fileName)

        // FIXME: symbols escape contexts ¯\_(ツ)_/¯
        // This doesn't work; AA complains "... is inaccessible: Called outside analyse method"
        /*
        val bar = resolver.getAllFiles().single { it.fileName == "Foo.kt" }
            .declarations.single { it.simpleName.asString() == "Foo" }
            .safeAs<KSClassDeclaration>()!!.superTypes.single().resolve().declaration
            .safeAs<KSClassDeclaration>()!!

        results.add(bar.simpleName.asString())
         */

        // This is OK if making KSFileImpl.ktFile public:
        /*
        val ktFile = fooKt.safeAs<KSFileImpl>()!!.ktFile
        analyseWithReadAction(ktFile) {
            val fileSymbol = ktFile.getFileSymbol()
            val members = fileSymbol.getFileScope().getAllSymbols()
            val classes = members.filterIsInstance<KtClassOrObjectSymbol>()
            classes.first().superTypes.filter { it.toString() == "Bar" }.firstOrNull()?.let {
                results.add(it.toString())
            }
        }
         */

        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }
}
