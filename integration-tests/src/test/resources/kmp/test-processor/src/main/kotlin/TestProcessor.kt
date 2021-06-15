import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSTopDownVisitor
import java.io.OutputStreamWriter


class TestProcessor(val codeGenerator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val allFiles = resolver.getAllFiles().map { it.fileName }
        logger.warn(allFiles.toList().toString())
        if (invoked) {
            return emptyList()
        }
        invoked = true

        codeGenerator.createNewFile(Dependencies(false), "", "Foo", "kt").use { output ->
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
        return TestProcessor(env.codeGenerator, env.logger)
    }
}
