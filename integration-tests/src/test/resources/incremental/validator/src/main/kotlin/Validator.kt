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


class Validator : SymbolProcessor {
    lateinit var codeGenerator: CodeGenerator
    lateinit var logger: KSPLogger

    override fun init(options: Map<String, String>, kotlinVersion: KotlinVersion, codeGenerator: CodeGenerator, logger: KSPLogger) {
        this.codeGenerator = codeGenerator
        this.logger = logger
    }

    override fun finish() = Unit

    override fun process(resolver: Resolver) {
        val validator = object : KSDefaultVisitor<OutputStreamWriter, Unit>() {
            override fun defaultHandler(node: KSNode, data: OutputStreamWriter) = Unit

            override fun visitDeclaration(declaration: KSDeclaration, data: OutputStreamWriter) {
                data.write(declaration.qualifiedName?.asString() ?: declaration.simpleName.asString())
                declaration.validate()
            }
        }

        val files = resolver.getAllFiles()
        files.forEach { file ->
            logger.warn("${file.packageName.asString()}/${file.fileName}")
            val output = OutputStreamWriter(codeGenerator.createNewFile(Dependencies(false, file), file.packageName.asString(), file.fileName, "log"))
            file.declarations.forEach {
                it.accept(validator, output)
            }
            output.close()
        }
    }
}


