import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.OutputStream

class TestProcessor : SymbolProcessor {
    lateinit var codeGenerator: CodeGenerator
    lateinit var file: OutputStream
    lateinit var logger: KSPLogger
    var invoked = false

    fun init(
        options: Map<String, String>,
        kotlinVersion: KotlinVersion,
        codeGenerator: CodeGenerator,
        logger: KSPLogger
    ) {
        this.logger = logger
        this.codeGenerator = codeGenerator
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) {
            return emptyList()
        }

        resolver.getClassDeclarationByName("com.example.NullabilityTest")?.let { tc ->
            tc.declarations.filterIsInstance<KSFunctionDeclaration>().forEach { decl ->
                val declName = decl.simpleName.asString()
                val returnType = decl.returnType!!.resolve()
                logger.warn("[Nullability check] $declName: $returnType")
            }
        }
        invoked = true
        return emptyList()
    }
}

class TestProcessorProvider : SymbolProcessorProvider {
    override fun create(
        env: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return TestProcessor().apply {
            init(env.options, env.kotlinVersion, env.codeGenerator, env.logger)
        }
    }
}
