import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate

class ValidateProcessor(env: SymbolProcessorEnvironment) : SymbolProcessor {
    var logger: KSPLogger = env.logger

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val javaClass = resolver.getClassDeclarationByName("com.example.JavaClass")!!
        val nestedClass = javaClass.declarations.filterIsInstance<KSClassDeclaration>()
            .single { it.simpleName.asString() == "NestedClass" }
        val provideString = nestedClass.declarations.filterIsInstance<KSFunctionDeclaration>()
            .single { it.simpleName.asString() == "provideString" }

        if (provideString.returnType!!.resolve().isError) {
            logger.error("provideString.returnType: not ok")
        }
        if (nestedClass.asStarProjectedType().isError == true) {
            logger.error("$javaClass.asStarProjectedType(): not ok")
        }
        if (!nestedClass.validate()) {
            logger.error("Failed to validate $nestedClass")
        }
        if (!javaClass.validate()) {
            logger.error("Failed to validate $javaClass")
        }
        return emptyList()
    }
}

class ValidateProcessorProvider : SymbolProcessorProvider {
    override fun create(
        env: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return ValidateProcessor(env)
    }
}
