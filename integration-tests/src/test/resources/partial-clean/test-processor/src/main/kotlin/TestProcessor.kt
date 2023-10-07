import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.OutputStreamWriter

class TestProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {
    // FIXME: use getSymbolsWithAnnotation after it is fixed.
    var rounds = 0
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (rounds++ > 0)
            return emptyList()
        logger.warn("$rounds: ${resolver.getNewFiles().toList().sortedBy { it.fileName }}")

        resolver.getAllFiles().singleOrNull { it.fileName == "Bar.kt" }?.let {
            codeGenerator.createNewFile(
                Dependencies(false, it),
                "com.example",
                "Bar1",
                "kt"
            ).use { output ->
                OutputStreamWriter(output).use { writer ->
                    writer.write("package com.example\n\n")
                    writer.write("open class Bar1\n")
                }
            }
        }

        resolver.getAllFiles().single { it.fileName == "Baz.kt" }.let {
            it.declarations.filterIsInstance<KSClassDeclaration>().single().let {
                it.superTypes.mapNotNull {
                    it.resolve().declaration.containingFile
                }.single { it.fileName == "Bar.kt" }.let {
                    codeGenerator.createNewFile(
                        Dependencies(false, it),
                        "com.example",
                        "Bar2",
                        "kt"
                    ).use { output ->
                        OutputStreamWriter(output).use { writer ->
                            writer.write("package com.example\n\n")
                            writer.write("open class Bar2\n")
                        }
                    }
                }
            }
        }

        return emptyList()
    }
}

class TestProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return TestProcessor(environment.codeGenerator, environment.logger)
    }
}
