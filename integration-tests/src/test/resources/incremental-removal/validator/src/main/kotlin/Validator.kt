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

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation("p1.MyAnnotation").singleOrNull()?.let { decl ->
            decl as KSClassDeclaration
            val file = decl.containingFile!!
            OutputStreamWriter(
                codeGenerator.createNewFile(
                    Dependencies(false, file),
                    "p1", "Foo"
                )
            ).use {
                it.write("package p1\n\nclass Foo : Bar { override fun s() = \"generated\" }\n")
            }
        }
        resolver.getNewFiles().forEach {
            it.validate()
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
