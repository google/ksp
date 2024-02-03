import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*

class TestProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getNewFiles().sortedBy { it.fileName }.forEach { f ->
            logger.warn("Processing ${f.fileName}")
            f.declarations.forEach {
                if (it is KSClassDeclaration) {
                    val subs = it.getSealedSubclasses().map { it.simpleName.asString() }.toList()
                    logger.warn("${it.simpleName.asString()} : $subs")
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
