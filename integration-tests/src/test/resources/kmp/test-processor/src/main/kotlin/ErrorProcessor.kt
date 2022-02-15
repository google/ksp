import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.OutputStream

class ErrorProcessor : SymbolProcessor {
    lateinit var codeGenerator: CodeGenerator
    lateinit var logger: KSPLogger
    lateinit var file: OutputStream
    lateinit var exception: String

    fun init(
        options: Map<String, String>,
        kotlinVersion: KotlinVersion,
        codeGenerator: CodeGenerator,
        logger: KSPLogger
    ) {
        exception = if (options.containsKey("exception")) {
            options["exception"]!!
        } else {
            ""
        }
        if (exception == "init") {
            throw Exception("Test Exception in init")
        }
        this.logger = logger
        this.codeGenerator = codeGenerator
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (exception == "createTwice") {
            codeGenerator.createNewFile(Dependencies.ALL_FILES, "create", "Twice").write("".toByteArray())
            return emptyList()
        }
        if (exception == "process") {
            throw Exception("Test Exception in process")
        }
        return emptyList()
    }

    override fun finish() {
        if (exception == "finish") {
            throw Exception("Test Exception in finish")
        }
    }

    override fun onError() {
        if (exception == "error") {
            throw Exception("Test Exception in error")
        }
    }
}

class ErrorProcessorProvider : SymbolProcessorProvider {
    override fun create(
        env: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return ErrorProcessor().apply {
            init(env.options, env.kotlinVersion, env.codeGenerator, env.logger)
        }
    }
}
