import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

class TestProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: Map<String, String>,
    private val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation("com.example.ann.MyAnn")
            .filterIsInstance<KSFunctionDeclaration>()
            .forEach { func ->
                val arg = func.annotations.first().arguments.first().value.toString()
                if (!arg.startsWith("REPLACE"))
                    throw IllegalStateException(arg)
            }

        return emptyList()
    }
}

class TestProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        TestProcessor(environment.codeGenerator, environment.options, environment.logger)
}
