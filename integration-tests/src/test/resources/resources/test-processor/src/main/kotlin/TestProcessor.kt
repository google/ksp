import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*

class TestProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
): SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.warn("Processing resources")
        for (resource in resolver.getAllResources()) {
            logger.warn("$resource: ${resolver.getResource(resource)!!.bufferedReader().use { it.readText() }}")
        }
        return emptyList()
    }
}

class TestProcessorProvider : SymbolProcessorProvider {
    override fun create(env: SymbolProcessorEnvironment): SymbolProcessor = TestProcessor(env.codeGenerator, env.logger)
}
