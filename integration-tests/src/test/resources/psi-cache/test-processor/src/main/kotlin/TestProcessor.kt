import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate


class TestProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {
    var rounds = 0
    override fun process(resolver: Resolver): List<KSAnnotated> {
        rounds++

        val syms = resolver.getSymbolsWithAnnotation("com.example.Anno").toList()

        syms.forEach {
            val v = it.validate()
            if (rounds == 2 && v == false) {
                logger.error("validation failed: $it")
            }
        }

        if (rounds == 1) {
            codeGenerator.createNewFile(Dependencies(true), "com.example", "Foo1").use {
                it.write("package com.example\n\ninterface Foo1\n".toByteArray())
            }
            codeGenerator.createNewFile(Dependencies(true), "com.example", "Foo2", "java").use {
                it.write("package com.example;\n\npublic interface Foo2{}\n".toByteArray())
            }
            return syms.toList()
        }

        return emptyList()
    }
}

class TestProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return TestProcessor(environment.codeGenerator, environment.logger)
    }
}
