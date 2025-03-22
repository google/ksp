import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getFunctionDeclarationsByName
import com.google.devtools.ksp.getPropertyDeclarationByName
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import java.io.OutputStreamWriter

class Validator : SymbolProcessor {
    lateinit var codeGenerator: CodeGenerator
    lateinit var logger: KSPLogger
    var processed = false

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
        if (processed) {
            return emptyList()
        }

        val validator = object : KSDefaultVisitor<OutputStreamWriter, Unit>() {
            override fun defaultHandler(node: KSNode, data: OutputStreamWriter) = Unit

            override fun visitDeclaration(declaration: KSDeclaration, data: OutputStreamWriter) {
                data.write(declaration.qualifiedName?.asString() ?: declaration.simpleName.asString())
                declaration.validate()
            }
        }

        // for each file, create an output from it and everything reachable from it.
        val files = resolver.getAllFiles()
        files.forEach { file ->
            logger.warn("${file.packageName.asString()}/${file.fileName}")
            val output = OutputStreamWriter(
                codeGenerator.createNewFile(
                    Dependencies(true, file),
                    file.packageName.asString(),
                    file.fileName,
                    "log"
                )
            )
            file.declarations.forEach {
                it.accept(validator, output)
            }
            output.close()
        }

        processed = true
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
