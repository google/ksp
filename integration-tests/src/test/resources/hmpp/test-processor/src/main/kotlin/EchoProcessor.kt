import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*

class EchoProcessor(val codeGenerator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) {
            return emptyList()
        }
        invoked = true

        val allInputs = resolver.getAllFiles().map { it.fileName.split(".").first() }.sorted().joinToString("_")

        logger.warn("EchoProcessor: $allInputs")

        codeGenerator.createNewFile(Dependencies(true), "", "($allInputs)").close()

        return emptyList()
    }
}

class EchoProcessorProvider : SymbolProcessorProvider {
    override fun create(env: SymbolProcessorEnvironment): SymbolProcessor {
        return EchoProcessor(env.codeGenerator, env.logger)
    }
}
