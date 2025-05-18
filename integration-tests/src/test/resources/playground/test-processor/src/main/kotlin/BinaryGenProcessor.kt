import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.OutputStream
import kotlin.io.encoding.*

class BinaryGenProcessor(val env: SymbolProcessorEnvironment) : SymbolProcessor {
    val codeGenerator: CodeGenerator = env.codeGenerator
    val logger: KSPLogger = env.logger

    // public class BinaryClass {}
    val content = "yv66vgAAADQADQoAAgADBwAEDAAFAAYBABBqYXZhL2xhbmcvT2JqZWN0AQAGPGluaXQ+AQADKClW" +
        "BwAIAQALQmluYXJ5Q2xhc3MBAARDb2RlAQAPTGluZU51bWJlclRhYmxlAQAKU291cmNlRmlsZQEA" +
        "EEJpbmFyeUNsYXNzLmphdmEAIQAHAAIAAAAAAAEAAQAFAAYAAQAJAAAAHQABAAEAAAAFKrcAAbEA" +
        "AAABAAoAAAAGAAEAAAABAAEACwAAAAIADA=="

    @OptIn(KspExperimental::class, ExperimentalEncodingApi::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val needGenerated = resolver.getNewFiles().any {
            it.fileName == "NeedGenerated.kt"
        }

        if (needGenerated) {
            codeGenerator.createNewFile(Dependencies(false), "", "BinaryClass", "class").use { outFile ->
                val decoded = Base64.decode(content)
                outFile.write(decoded)
            }
        }

        return emptyList()
    }
}

class BinaryGenProcessorProvider : SymbolProcessorProvider {
    override fun create(
        env: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return BinaryGenProcessor(env)
    }
}
