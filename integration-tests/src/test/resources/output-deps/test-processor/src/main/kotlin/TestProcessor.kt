import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import java.io.File
import java.io.OutputStream
import java.io.OutputStreamWriter

class TestProcessor : SymbolProcessor {
    lateinit var codeGenerator: CodeGenerator
    lateinit var logger: KSPLogger
    var processed = false

    override fun init(options: Map<String, String>, kotlinVersion: KotlinVersion, codeGenerator: CodeGenerator, logger: KSPLogger) {
        this.codeGenerator = codeGenerator
        this.logger = logger
    }

    override fun finish() = Unit

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if(processed) {
            return emptyList()
        }
        fun outputForAnno(anno: String) {
            val annoFiles = resolver.getSymbolsWithAnnotation(anno).map { (it as KSDeclaration).containingFile!! }
            codeGenerator.createNewFile(Dependencies(false, *annoFiles.toTypedArray()), "", anno, "log").use { output ->
                OutputStreamWriter(output).use { writer ->
                    writer.write(annoFiles.map { it.fileName }.joinToString(", "))
                }
            }
        }

        outputForAnno("p1.Anno1")
        outputForAnno("p1.Anno2")

        resolver.getNewFiles().forEach { file ->
            val test_file = File("/tmp/ksp-test.log")
            test_file.appendText("${file.packageName.asString()}/${file.fileName}\n")
            logger.warn("${file.packageName.asString()}/${file.fileName}")
        }
        processed = true
        return emptyList()
    }
}


