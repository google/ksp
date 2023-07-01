import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStream

fun OutputStream.appendText(str: String) {
    this.write(str.toByteArray())
}

class TestProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger,
) : SymbolProcessor {
    var round = 0

    fun chk(decl: KSClassDeclaration) {
        val t = decl.asStarProjectedType()
        val v = decl.validate()
        logger.warn("ROUND $round: ${decl.qualifiedName?.asString()}: $t: $v")
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (round > 0) {
            resolver.getNewFiles().forEach {
                it.declarations.filterIsInstance<KSClassDeclaration>().forEach {
                    chk(it)
                    it.declarations.filterIsInstance<KSClassDeclaration>().forEach {
                        if (it.packageName.asString() == "cached.index") {
                            if (!it.validate()) {
                                throw Exception("Invalid class: ${it.qualifiedName?.asString()}")
                            }
                            chk(it)
                        }
                    }
                }
            }
        }
        if (round < 5) {
            codeGenerator.createNewFile(Dependencies(false), "cached.index", "O$round", "java").use {
                it.appendText("package cached.index;\n")
                it.appendText("public class O$round {\n")
                it.appendText("  public static class N$round {\n")
                it.appendText("  }\n")
                it.appendText("}\n")
            }
            round += 1
        }
        return emptyList()
    }
}

class TestProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return TestProcessor(environment.codeGenerator, environment.logger)
    }
}
