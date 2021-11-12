import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*

class TestProcessor(val codeGenerator: CodeGenerator) : SymbolProcessor {

    var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) {
            return emptyList()
        }

        codeGenerator.createNewFile(Dependencies(false), "", "HelloSwift", "swift")

        invoked = true
        return emptyList()
    }

    class Provider : SymbolProcessorProvider {
        override fun create(
            environment: SymbolProcessorEnvironment
        ): SymbolProcessor = TestProcessor(environment.codeGenerator)
    }
}
