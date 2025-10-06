import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStream

fun OutputStream.appendText(str: String) {
    this.write(str.toByteArray())
}

class BuilderProcessor : SymbolProcessor {
    lateinit var codeGenerator: CodeGenerator
    lateinit var logger: KSPLogger

    fun init(
        options: Map<String, String>,
        kotlinVersion: KotlinVersion,
        codeGenerator: CodeGenerator,
        logger: KSPLogger,
    ) {
        this.codeGenerator = codeGenerator
        this.logger = logger
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("com.example.annotation.Builder")
        val ret = symbols.filter { !it.validate() }
        symbols
            .filter { it.validate() }
            .forEach { it.accept(BuilderVisitor(), Unit) }
        return ret.toList()
    }

    inner class BuilderVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val constructor = classDeclaration.primaryConstructor
                ?: classDeclaration.getConstructors().maxByOrNull { it.parameters.size }

            if (constructor == null) {
                logger.error("No suitable constructor found for ${classDeclaration.qualifiedName?.asString()}", classDeclaration)
                return
            }

            constructor.accept(this, data)
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            val parent = function.parentDeclaration as KSClassDeclaration
            val packageName = parent.containingFile!!.packageName.asString()
            val className = "${parent.simpleName.asString()}Builder"

            val file = codeGenerator.createNewFile(
                Dependencies(true, function.containingFile!!), packageName, className
            )
            file.appendText("package $packageName\n\n")
            file.appendText("class $className{\n")
            function.parameters.forEach {
                val name = it.name!!.asString()
                val typeName = StringBuilder(it.type.resolve().declaration.qualifiedName?.asString() ?: "<ERROR>")
                val typeArgs = it.type.element!!.typeArguments
                if (it.type.element!!.typeArguments.toList().isNotEmpty()) {
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

class TestProcessorProvider2 : SymbolProcessorProvider {
    override fun create(
        env: SymbolProcessorEnvironment,
    ): SymbolProcessor {
        return BuilderProcessor().apply {
            init(env.options, env.kotlinVersion, env.codeGenerator, env.logger)
        }
    }
}
