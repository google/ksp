import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate

class ValidateProcessor(val codeGenerator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) {
            return emptyList()
        }
        invoked = true

        val toValidate = resolver.getSymbolsWithAnnotation("com.example.MyAnnotation")
        if (toValidate.toList().size == 0) {
            logger.error("not ok, zero element")
        }
        if (!toValidate.all { it.validate() }) {
            logger.error("not ok")
        }
        return emptyList()
    }
}

class ValidateProcessorProvider : SymbolProcessorProvider {
    override fun create(env: SymbolProcessorEnvironment): SymbolProcessor {
        return ValidateProcessor(env.codeGenerator, env.logger)
    }
}
