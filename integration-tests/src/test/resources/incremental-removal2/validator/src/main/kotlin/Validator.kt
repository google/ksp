import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStreamWriter

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

    var generated = false
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated)
            return emptyList()
        generated = true
        val allFiles = resolver.getAllFiles().toList()
        val k1 = allFiles.single { it.fileName == "K1.kt" }
        OutputStreamWriter(
            codeGenerator.createNewFile(
                Dependencies(true, k1),
                "", "Files"
            )
        ).use { os ->
            os.write("package p1\n\n")
            os.write("val files = listOf(\"${k1.fileName}\")\n")
        }
        allFiles.forEach {
            logger.warn("Input: ${it.fileName}")
        }
        return emptyList()
    }
}

class TestProcessorProvider : SymbolProcessorProvider {
    override fun create(
        env: SymbolProcessorEnvironment,
    ): SymbolProcessor {
        return Validator().apply {
            init(env.options, env.kotlinVersion, env.codeGenerator, env.logger)
        }
    }
}
