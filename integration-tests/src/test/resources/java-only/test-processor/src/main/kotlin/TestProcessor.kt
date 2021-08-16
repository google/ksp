import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.OutputStreamWriter

class TestProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {
    var rounds = 0
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (++rounds == 1) {
            codeGenerator.createNewFile(Dependencies(false), "com.example", "Bar", "kt").use { output ->
                OutputStreamWriter(output).use { writer ->
                    writer.write("package com.example\n\n")
                    writer.write("interface Bar\n")
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
