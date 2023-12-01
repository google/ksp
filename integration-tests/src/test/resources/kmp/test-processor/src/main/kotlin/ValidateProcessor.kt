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
        toValidate.forEach {
            if (!it.validate()) {
                logger.error("$it.validate(): not ok")
            }
        }
        if ((toValidate.firstOrNull() as? KSClassDeclaration)?.asStarProjectedType()?.isError == true) {
            logger.error("$toValidate.asStarProjectedType(): not ok")
        }
        return emptyList()
    }
}

class ValidateProcessorProvider : SymbolProcessorProvider {
    override fun create(env: SymbolProcessorEnvironment): SymbolProcessor {
        return ValidateProcessor(env.codeGenerator, env.logger)
    }
}
