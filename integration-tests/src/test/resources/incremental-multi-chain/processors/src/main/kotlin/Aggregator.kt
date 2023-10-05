import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.OutputStreamWriter

class Aggregator : SymbolProcessor {
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
        val impls = resolver.getSymbolsWithAnnotation("Impl").map { it as KSDeclaration }.toList()
        if (impls.isNotEmpty()) {
            val names = impls.map { it.simpleName.asString() }.sorted()
            OutputStreamWriter(
                codeGenerator.createNewFile(
                    Dependencies(true), "", "AllImpls"
                )
            ).use {
                it.write("class AllImpls {\n")
                it.write("  override fun toString() = \"$names\"\n")
                it.write("}\n")
            }
            codeGenerator.associate(impls.map { it.containingFile!! }.toList(), "", "AllImpls")
        }

        impls.forEach { decl ->
            decl as KSClassDeclaration
            val file = decl.containingFile!!
            val baseName = decl.simpleName.asString()
            val fileName = baseName + "Info"
            OutputStreamWriter(
                codeGenerator.createNewFile(
                    Dependencies(false, file),
                    "", fileName
                )
            ).use {
                it.write("// dummy file")
            }
        }
        return emptyList()
    }
}

class AggregatorProvider : SymbolProcessorProvider {
    override fun create(
        env: SymbolProcessorEnvironment,
    ): SymbolProcessor {
        return Aggregator().apply {
            init(env.options, env.kotlinVersion, env.codeGenerator, env.logger)
        }
    }
}
