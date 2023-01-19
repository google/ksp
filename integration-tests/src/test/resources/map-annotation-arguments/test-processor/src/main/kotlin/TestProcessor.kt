import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*

class TestProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {
    val expected = mapOf(
        "unboxedChar" to "Char",
        "boxedChar" to "(Char..Char?)",
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val j = resolver.getClassDeclarationByName("com.example.AnnotationTest")!!
        j.annotations.forEach { annotation ->
            annotation.arguments.forEach {
                val key = it.name?.asString()
                val value = it.value.toString()
                if (expected[key] != value) {
                    logger.error("$key: ${expected[key]} != $value")
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
