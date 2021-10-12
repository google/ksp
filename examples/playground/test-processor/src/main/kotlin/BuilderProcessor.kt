import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.File
import java.io.OutputStream

fun OutputStream.appendText(str: String) {
    this.write(str.toByteArray())
}
class BuilderProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("com.example.annotation.Builder")
        val ret = symbols.filter { !it.validate() }.toList()
        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(BuilderVisitor(), Unit) }
        return ret
    }

    inner class BuilderVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            classDeclaration.primaryConstructor!!.accept(this, data)
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            val parent = function.parentDeclaration as KSClassDeclaration
            val packageName = parent.containingFile!!.packageName.asString()
            val className = "${parent.simpleName.asString()}Builder"
            val file = codeGenerator.createNewFile(Dependencies(true, function.containingFile!!), packageName , className)
            file.appendText("package $packageName\n\n")
            file.appendText("import HELLO\n\n")
            file.appendText("class $className{\n")
            function.parameters.forEach {
                val name = it.name!!.asString()
                val typeName = StringBuilder(it.type.resolve().declaration.qualifiedName?.asString() ?: "<ERROR>")
                val typeArgs = it.type.element!!.typeArguments
                if (it.type.element!!.typeArguments.isNotEmpty()) {
                    typeName.append("<")
                    typeName.append(
                            typeArgs.map {
                                val type = it.type?.resolve()
                                "${it.variance.label} ${type?.declaration?.qualifiedName?.asString() ?: "ERROR"}" +
                                        if (type?.nullability == Nullability.NULLABLE) "?" else ""
                            }.joinToString(", ")
                    )
                    typeName.append(">")
                }
                file.appendText("    private var $name: $typeName? = null\n")
                file.appendText("    internal fun with${name.capitalize()}($name: $typeName): $className {\n")
                file.appendText("        this.$name = $name\n")
                file.appendText("        return this\n")
                file.appendText("    }\n\n")
            }
            file.appendText("    internal fun build(): ${parent.qualifiedName!!.asString()} {\n")
            file.appendText("        return ${parent.qualifiedName!!.asString()}(")
            file.appendText(
                function.parameters.map {
                    "${it.name!!.asString()}!!"
                }.joinToString(", ")
            )
            file.appendText(")\n")
            file.appendText("    }\n")
            file.appendText("}\n")
            file.close()
        }
    }

}

class BuilderProcessorProvider : SymbolProcessorProvider {
    override fun create(
        env: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return BuilderProcessor(env.codeGenerator, env.logger)
    }
}