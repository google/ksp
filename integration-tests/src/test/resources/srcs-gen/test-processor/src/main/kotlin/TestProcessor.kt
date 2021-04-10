import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.OutputStreamWriter


class TestProcessor : SymbolProcessor {
    lateinit var codeGenerator: CodeGenerator
    lateinit var logger: KSPLogger

    override fun finish() {
    }

    override fun init(options: Map<String, String>, kotlinVersion: KotlinVersion, codeGenerator: CodeGenerator, logger: KSPLogger) {
        this.codeGenerator = codeGenerator
        this.logger = logger
    }

    // FIXME: use getSymbolsWithAnnotation after it is fixed.
    var rounds = 0
    override fun process(resolver: Resolver): List<KSAnnotated> {
        rounds++
        logger.warn("$rounds: ${resolver.getNewFiles()}")

        // Would fail if "Bar.kt" isn't dirty.
        val barKt = resolver.getAllFiles().single { it.fileName == "Bar.kt" }
        val bazKt = resolver.getAllFiles().single { it.fileName == "Baz.kt" }

        if (rounds == 1) {
            codeGenerator.createNewFile(Dependencies(false), "", "Foo", "kt").use { output ->
                OutputStreamWriter(output).use { writer ->
                    writer.write("package com.example\n\n")
                    writer.write("open class Foo\n")
                }
            }
        }

        if (rounds == 2) {
            val fooKt = resolver.getAllFiles().single { it.fileName == "Foo.kt" }
            codeGenerator.createNewFile(Dependencies(false, fooKt, barKt), "", "FooBar", "kt").use { output ->
                OutputStreamWriter(output).use { writer ->
                    writer.write("package com.example\n\n")
                    writer.write("open class FooBar\n")
                }
            }
            codeGenerator.createNewFile(Dependencies(false, fooKt, bazKt), "", "FooBaz", "kt").use { output ->
                OutputStreamWriter(output).use { writer ->
                    writer.write("package com.example\n\n")
                    writer.write("open class FooBaz\n")
                }
            }
        }

        return emptyList()
    }
}