
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*

class RewriteProcessor : SymbolProcessor {
    lateinit var codeGenerator: CodeGenerator

    fun init(
        options: Map<String, String>,
        kotlinVersion: KotlinVersion,
        codeGenerator: CodeGenerator,
        logger: KSPLogger
    ) {
        this.codeGenerator = codeGenerator
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val fileKt = codeGenerator.createNewFile(Dependencies(false), "hello", "HELLO", "java")
        return emptyList()
    }
}

class RewriteProcessorProvider : SymbolProcessorProvider {
    override fun create(
        env: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return RewriteProcessor().apply {
            init(env.options, env.kotlinVersion, env.codeGenerator, env.logger)
        }
    }
}
