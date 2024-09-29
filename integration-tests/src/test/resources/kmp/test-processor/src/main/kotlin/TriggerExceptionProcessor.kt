import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate

class TriggerExceptionProcessor(val codeGenerator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) {
            return emptyList()
        }
        invoked = true

        val symbols = resolver.getSymbolsWithAnnotation("com.example.TriggerExceptionAnnotation")
        if (symbols.any()) {
            logger.error("Exception triggered")
            error("Exception triggered")
        }

        codeGenerator.createNewFile(
            Dependencies(true),
            "",
            "NoTrigger",
            "kt"
        )

        return emptyList()
    }
}

class TriggerExceptionProvider : SymbolProcessorProvider {
    override fun create(env: SymbolProcessorEnvironment): SymbolProcessor {
        return TriggerExceptionProcessor(env.codeGenerator, env.logger)
    }
}
