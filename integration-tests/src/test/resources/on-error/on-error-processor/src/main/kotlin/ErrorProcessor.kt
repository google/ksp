import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.File
import java.io.OutputStream
import kotlin.math.round


class ErrorProcessor : SymbolProcessor {
    lateinit var codeGenerator: CodeGenerator
    lateinit var logger: KSPLogger
    lateinit var file: OutputStream
    var rounds = 0


    override fun finish() {
    }

    override fun onError() {
    }

    override fun init(options: Map<String, String>, kotlinVersion: KotlinVersion, codeGenerator: CodeGenerator, logger: KSPLogger) {
        this.logger = logger
        this.codeGenerator = codeGenerator
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        rounds++
        if (rounds == 2) {
            logger.error("Error processor: errored at ${rounds}")
        } else {
            codeGenerator.createNewFile(Dependencies.ALL_FILES, "test", "error", "log")
        }
        return emptyList()
    }
}


