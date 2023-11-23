import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStreamWriter

class TestProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {
    // FIXME: use getSymbolsWithAnnotation after it is fixed.
    var rounds = 0
    override fun process(resolver: Resolver): List<KSAnnotated> {
        rounds++
        logger.warn("$rounds: ${resolver.getNewFiles().sortedBy { it.fileName }.toList()}")

        if (rounds == 1) {
            codeGenerator.createNewFile(Dependencies(false), "", "Foo", "kt").use { output ->
                OutputStreamWriter(output).use { writer ->
                    writer.write("package com.example\n\n")
                    writer.write("open class Foo : Goo()\n")
                }
            }
        }

        if (rounds == 2) {
            codeGenerator.createNewFile(Dependencies(false), "", "Goo", "kt").use { output ->
                OutputStreamWriter(output).use { writer ->
                    writer.write("package com.example\n\n")
                    writer.write("open class Goo : Baz()\n")
                }
            }
        }

        resolver.getNewFiles().forEach {
            it.validate()
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
