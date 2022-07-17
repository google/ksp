import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import java.io.OutputStreamWriter

class TestProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    private var invoked = false

    private fun String.sourceSetBelow(startDirectoryName: String): String =
        substringAfter("/$startDirectoryName/").substringBefore("/kotlin/").substringAfterLast('/')

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val allFileNamesSorted =
            resolver.getAllFiles().map { "${it.filePath.sourceSetBelow("src")}:${it.fileName}" }.toList().sorted()
        val newFileNamesSorted =
            resolver.getNewFiles().map { "${it.filePath.sourceSetBelow("src")}:${it.fileName}" }.toList().sorted()
        logger.warn("all files: $allFileNamesSorted")
        logger.warn("new files: $newFileNamesSorted")

        if (invoked) {
            return emptyList()
        }
        invoked = true

        environment.options.toSortedMap().forEach { (key, value) ->
            logger.warn("option: '$key' -> '$value'")
        }

        val options = environment.options.toSortedMap().map { (key, value) -> "'$key' -> '$value'" }

        codeGenerator.createNewFile(
            Dependencies(aggregating = true, *resolver.getAllFiles().toList().toTypedArray()),
            "com.example",
            "Generated",
            "kt"
        ).use { output ->
            val outputSourceSet = codeGenerator.generatedFile.first().toString().sourceSetBelow("ksp")

            OutputStreamWriter(output).use { writer ->
                writer.write(
                    """
                        package com.example
                        
                        object GeneratedFor${outputSourceSet.replaceFirstChar { it.uppercaseChar() }} {
                            const val allFiles = "$allFileNamesSorted"
                            const val newFiles = "$newFileNamesSorted"
                            const val options = "$options"
                            const val outputSourceSet = "$outputSourceSet"
                        }
                    
                    """.trimIndent()
                )
            }
        }

        return emptyList()
    }
}

class TestProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return TestProcessor(environment.codeGenerator, environment.logger, environment)
    }
}
