import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStreamWriter

class TestBuildCacheProcessor : SymbolProcessor {
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
        resolver.getSymbolsWithAnnotation("p1.MyAnnotation").forEach { decl ->
            decl as KSClassDeclaration

            val pkg = decl.packageName.asString()
            val name = decl.simpleName.asString()
            val generated = name + "Generated"
            val output = codeGenerator.createNewFile(
                Dependencies(false, decl.containingFile!!),
                pkg, generated
            )
            OutputStreamWriter(output).use {
                it.write("package $pkg\n\nclass $generated(val className: String = $name::class.java.simpleName)\n")
            }
        }
        resolver.getNewFiles().forEach {
            it.validate()
        }
        return emptyList()
    }
}

class TestBuildCacheProcessorProvider : SymbolProcessorProvider {
    override fun create(
        env: SymbolProcessorEnvironment,
    ): SymbolProcessor {
        return TestBuildCacheProcessor().apply {
            init(env.options, env.kotlinVersion, env.codeGenerator, env.logger)
        }
    }
}
