import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*


class NormalProcessor : SymbolProcessor {
    lateinit var codeGenerator: CodeGenerator
    lateinit var logger: KSPLogger
    var rounds = 0


    override fun finish() {
    }

    override fun onError() {
        logger.error("NormalProcessor called error on $rounds")
    }

    override fun init(options: Map<String, String>, kotlinVersion: KotlinVersion, codeGenerator: CodeGenerator, logger: KSPLogger) {
        this.logger = logger
        this.codeGenerator = codeGenerator
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        rounds++
        if (rounds == 1) {
            codeGenerator.createNewFile(Dependencies.ALL_FILES, "test", "normal", "log")
        }
        return emptyList()
    }
}


