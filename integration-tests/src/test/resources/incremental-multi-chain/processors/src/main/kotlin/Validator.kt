import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate

class Validator : SymbolProcessor {
    lateinit var codeGenerator: CodeGenerator
    lateinit var logger: KSPLogger

    fun init(
        options: Map<String, String>,
        kotlinVersion: KotlinVersion,
        codeGenerator: CodeGenerator,
        logger: KSPLogger,
    ) {
        this.codeGenerator = codeGenerator
        this.logger = logger
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getNewFiles().forEach {
            logger.warn("validating ${it.fileName}")
            it.validate()
        }
        return emptyList()
    }
}

class ValidatorProvider : SymbolProcessorProvider {
    override fun create(
        env: SymbolProcessorEnvironment,
    ): SymbolProcessor {
        return Validator().apply {
            init(env.options, env.kotlinVersion, env.codeGenerator, env.logger)
        }
    }
}
