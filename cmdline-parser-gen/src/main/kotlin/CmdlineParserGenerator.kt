import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.OutputStream

private fun OutputStream.appendText(str: String) {
    this.write(str.toByteArray())
}

private fun OutputStream.appendLine(str: String = "") {
    appendText(str + System.lineSeparator())
}

// modueName => -module-name
private fun String.camelToOptionName(): String = fold(StringBuilder()) { acc, c ->
    acc.let {
        val lower = c.lowercase()
        acc.append(if (acc.isEmpty() || c.isUpperCase()) "-$lower" else lower)
    }
}.toString()

class CmdlineParserGenerator(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger,
    val options: Map<String, String>
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotationName = "com.google.devtools.ksp.processing.KSPArgParserGen"
        val kspConfigBuilders =
            resolver.getSymbolsWithAnnotation(annotationName)

        kspConfigBuilders.filterIsInstance<KSClassDeclaration>().forEach { builderClass ->
            val parserName = builderClass.annotations.single {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationName
            }.arguments.single().value as String
            val configClass = builderClass.parentDeclaration as KSClassDeclaration
            val builderName = "${configClass.simpleName.asString()}.${builderClass.simpleName.asString()}"
            codeGenerator.createNewFile(
                Dependencies(false, builderClass.containingFile!!),
                builderClass.packageName.asString(),
                parserName
            ).use { os ->
                os.appendLine("package ${builderClass.packageName.asString()}")
                os.appendLine()
                os.appendLine(
                    "fun $parserName(args: Array<String>): Pair<${configClass.simpleName.asString()}, List<String>> {"
                )
                os.appendLine("  val processorClasspath = mutableListOf<String>()")
                os.appendLine("  return Pair($builderName().apply {")
                os.appendLine("    var i = 0")
                os.appendLine("    while (i < args.size) {")
                os.appendLine("      val arg = args[i++]")
                os.appendLine("      when {")

                builderClass.getAllProperties().filter { it.setter != null }.forEach { prop ->
                    val type = prop.type.resolve()
                    val typeName = type.declaration.simpleName.asString()
                    val propName = prop.simpleName.asString()
                    val optionName = propName.camelToOptionName()
                    val optionNameLen = optionName.length
                    when (typeName) {
                        "String", "Boolean", "File" -> {
                            os.appendLine(
                                "        arg == \"$optionName\" -> " +
                                    "$propName = parse$typeName(getArg(args, i++))"
                            )
                            os.appendLine(
                                "        arg.startsWith(\"$optionName=\") -> " +
                                    "$propName = parse$typeName(arg.substring(${optionNameLen + 1}))"
                            )
                        }
                        "List", "Map" -> {
                            val elementTypeName =
                                type.arguments.last().type!!.resolve().declaration.simpleName.asString()
                            os.appendLine(
                                "        arg == \"$optionName\" -> " +
                                    "$propName = parse$typeName(getArg(args, i++), ::parse$elementTypeName)"
                            )
                            os.appendLine(
                                "        arg.startsWith(\"$optionName=\") -> " +
                                    "$propName = parse$typeName(arg.substring(${optionNameLen + 1}), " +
                                    "::parse$elementTypeName)"
                            )
                        }
                        else -> {
                            throw IllegalArgumentException("Unknown type of option `$propName: ${prop.type}`")
                        }
                    }
                }

                // Free args are processor classpath
                os.appendLine("        else -> {")
                os.appendLine("          processorClasspath.addAll(parseList(arg, ::parseString))")
                os.appendLine("        }")
                os.appendLine("      }")
                os.appendLine("    }")
                os.appendLine("  }.build(), processorClasspath)")
                os.appendLine("}")
            }

            codeGenerator.createNewFile(
                Dependencies(false, builderClass.containingFile!!),
                builderClass.packageName.asString(),
                parserName + "Help"
            ).use { os ->
                os.appendLine("package ${builderClass.packageName.asString()}")
                os.appendLine()
                os.appendLine(
                    "fun ${parserName}Help(): String = \"\"\""
                )
                builderClass.getAllProperties().filter { it.setter != null }.forEach { prop ->
                    val type = prop.type.resolve()
                    val typeName = type.toString()
                    val propName = prop.simpleName.asString()
                    val optionName = propName.camelToOptionName()
                    val prefix = if (Modifier.LATEINIT in prop.modifiers) "*" else " "
                    os.appendLine("$prefix   $optionName=$typeName")
                }
                os.appendLine("*   <processor classpath>")
                os.appendLine("\"\"\"")
            }
        }
        return emptyList()
    }
}

class CmdlineParserGeneratorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return CmdlineParserGenerator(environment.codeGenerator, environment.logger, environment.options)
    }
}
