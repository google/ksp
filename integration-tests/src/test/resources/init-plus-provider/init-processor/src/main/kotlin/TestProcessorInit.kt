import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.File
import java.io.OutputStream

fun OutputStream.appendText(str: String) {
    this.write(str.toByteArray())
}

class TestProcessorInit : SymbolProcessor {
    lateinit var codeGenerator: CodeGenerator
    lateinit var file: OutputStream
    var invoked = false

    override fun init(options: Map<String, String>, kotlinVersion: KotlinVersion, codeGenerator: CodeGenerator, logger: KSPLogger) {
        this.codeGenerator = codeGenerator
        file = codeGenerator.createNewFile(Dependencies(false), "", "TestProcessorInit", "log")
        file.appendText("TestProcessorInit: init($options)\n")

        val javaFile = codeGenerator.createNewFile(Dependencies(false), "", "GeneratedFromInit", "java")
        javaFile.appendText("class GeneratedFromInit {}")
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) {
            return emptyList()
        }

        val fileKt = codeGenerator.createNewFile(Dependencies(false), "", "HelloFromInit", "java")

        fileKt.appendText("public class HelloFromInit{\n")
        fileKt.appendText("  public int foo() { return 1234; }\n")
        fileKt.appendText("}")

        invoked = true
        return emptyList()
    }
}
