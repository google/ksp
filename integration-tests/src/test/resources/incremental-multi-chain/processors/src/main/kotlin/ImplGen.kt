import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.OutputStreamWriter

class ImplGen : SymbolProcessor {
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
        resolver.getSymbolsWithAnnotation("NeedsImpl").forEach { decl ->
            decl as KSClassDeclaration
            val file = decl.containingFile!!
            val baseName = decl.simpleName.asString()
            val implName = baseName + "Impl"
            OutputStreamWriter(
                codeGenerator.createNewFile(
                    Dependencies(false, file),
                    "", implName
                )
            ).use {
                it.write("@Impl class $implName : $baseName\n")
            }
        }
        return emptyList()
    }
}

class ImplGenProvider : SymbolProcessorProvider {
    override fun create(
        env: SymbolProcessorEnvironment,
    ): SymbolProcessor {
        return ImplGen().apply {
            init(env.options, env.kotlinVersion, env.codeGenerator, env.logger)
        }
    }
}
