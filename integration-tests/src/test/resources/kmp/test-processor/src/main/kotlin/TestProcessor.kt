import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSTopDownVisitor
import java.io.OutputStreamWriter

class TestProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger,
    val env: SymbolProcessorEnvironment
) : SymbolProcessor {
    var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val allFiles = resolver.getAllFiles().map { it.fileName }
        logger.warn(allFiles.toList().toString())
        if (invoked) {
            return emptyList()
        }
        invoked = true

        logger.warn("language version: ${env.kotlinVersion}")
        logger.warn("api version: ${env.apiVersion}")
        logger.warn("compiler version: ${env.compilerVersion}")
        val platforms = env.platforms.map { it.toString() }
        logger.warn("platforms: $platforms")
        val list = resolver.getClassDeclarationByName("kotlin.collections.List")
        logger.warn("List has superTypes: ${list!!.superTypes.count() > 0}")

        codeGenerator.createNewFile(
            Dependencies(true, *resolver.getAllFiles().toList().toTypedArray()),
            "",
            "Foo",
            "kt"
        ).use { output ->
            OutputStreamWriter(output).use { writer ->
                writer.write("package com.example\n\n")
                writer.write("class Foo {\n")

                val visitor = ClassVisitor()
                resolver.getAllFiles().forEach {
                    it.accept(visitor, writer)
                }

                writer.write("}\n")
            }
        }

        allFiles.forEach {
            val fn = it.replace(".", "_dot_")
            codeGenerator.createNewFile(Dependencies(false), "", fn, "kt").use { output ->
                OutputStreamWriter(output).use { writer ->
                    writer.write("// empty\n")
                }
            }
        }
        return emptyList()
    }
}

class ClassVisitor : KSTopDownVisitor<OutputStreamWriter, Unit>() {
    override fun defaultHandler(node: KSNode, data: OutputStreamWriter) {
    }

    override fun visitClassDeclaration(
        classDeclaration: KSClassDeclaration,
        data: OutputStreamWriter
    ) {
        super.visitClassDeclaration(classDeclaration, data)
        val symbolName = classDeclaration.simpleName.asString().toLowerCase()
        data.write("    val $symbolName = true\n")
    }
}

class TestProcessorProvider : SymbolProcessorProvider {
    override fun create(env: SymbolProcessorEnvironment): SymbolProcessor {
        return TestProcessor(env.codeGenerator, env.logger, env)
    }
}
