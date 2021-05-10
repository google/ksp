import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.File
import java.io.OutputStream

fun OutputStream.appendText(str: String) {
    this.write(str.toByteArray())
}

class TestProcessor(options: Map<String, String>, val codeGenerator: CodeGenerator) : SymbolProcessor {
    val file: OutputStream = codeGenerator.createNewFile(Dependencies(false), "", "TestProcessor", "log")

    init {
        file.appendText("TestProcessor: init($options)\n")

        val javaFile = codeGenerator.createNewFile(Dependencies(false), "", "GeneratedFromProvider", "java")
        javaFile.appendText("class GeneratedFromProvider {}")
    }

    var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) {
            return emptyList()
        }

        val fileKt = codeGenerator.createNewFile(Dependencies(false), "", "HelloFromProvider", "java")

        fileKt.appendText("public class HelloFromProvider{\n")
        fileKt.appendText("  public int foo() { return 5678; }\n")
        fileKt.appendText("}")

        invoked = true
        return emptyList()
    }

    class Provider : SymbolProcessorProvider {
        override fun create(
            environment: SymbolProcessorEnvironment
        ): SymbolProcessor = TestProcessor(environment.options, environment.codeGenerator)
    }
}
