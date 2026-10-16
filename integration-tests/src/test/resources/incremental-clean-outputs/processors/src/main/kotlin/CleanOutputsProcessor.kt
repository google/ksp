import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class CleanOutputsProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private var round = 0
    private var pendingConsumer = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        round++
        val producers = resolver.getSymbolsWithAnnotation("fixture.GenerateModule").toList()
        val cached = resolver.getSymbolsWithAnnotation("fixture.CachedModule").count()
        environment.logger.info("round=$round producers=${producers.size} cachedCandidates=$cached")
        environment.logger.info("round=$round newFiles=${resolver.getNewFiles().map { it.fileName }.sorted().toList()}")
        environment.logger.info("round=$round allFiles=${resolver.getAllFiles().map { it.fileName }.sorted().toList()}")

        producers.forEach { producer ->
            producer as KSClassDeclaration
            val dependencies = Dependencies(false, producer.containingFile!!)
            val version = annotationValue(producer, "GenerateModule", "version") as Int
            environment.codeGenerator.createNewFile(dependencies, "fixture", "KotlinModule").writer().use {
                it.write("package fixture\n@CachedModule class KotlinModule // version=$version\n")
            }
            environment.codeGenerator.createNewFile(dependencies, "fixture", "JavaModule", "java").writer().use {
                it.write("package fixture;\n@CachedModule public class JavaModule {} // version=$version\n")
            }
        }

        val deferred = mutableListOf<KSAnnotated>()
        resolver.getSymbolsWithAnnotation("fixture.ConsumeModules").forEach { consumer ->
            consumer as KSClassDeclaration
            val expectPresent = annotationValue(consumer, "ConsumeModules", "expectPresent") as Boolean
            if (!checkModules(resolver, expectPresent)) {
                pendingConsumer = true
                deferred.add(consumer)
            } else {
                pendingConsumer = false
                // Deliberately associate these outputs only with the consumer. Looking up a generated
                // module must not require adding its unchanged producer as an originating file.
                val dependencies = Dependencies(false, consumer.containingFile!!)
                environment.codeGenerator.createNewFile(dependencies, "fixture", "ConsumerResult").writer().use {
                    it.write("package fixture\n")
                    if (expectPresent) {
                        it.write("class ConsumerResult(val kotlinModule: KotlinModule, val javaModule: JavaModule)\n")
                    } else {
                        it.write("class ConsumerResult\n")
                    }
                }
                // Force another round, including cache invalidation, after resolving restored sources.
                environment.codeGenerator.createNewFile(dependencies, "fixture", "NextRound").writer().use {
                    it.write("package fixture\n@CheckNextRound($expectPresent) class NextRound\n")
                }
            }
        }
        resolver.getSymbolsWithAnnotation("fixture.CheckNextRound").forEach { trigger ->
            trigger as KSClassDeclaration
            val expectPresent = annotationValue(trigger, "CheckNextRound", "expectPresent") as Boolean
            if (!checkModules(resolver, expectPresent)) {
                environment.logger.error("Generated modules unavailable in a later round", trigger)
            }
            environment.logger.info("checked next round")
        }
        return deferred
    }

    private fun checkModules(resolver: Resolver, expectPresent: Boolean): Boolean {
        val kotlinModule = resolver.getClassDeclarationByName(resolver.getKSNameFromString("fixture.KotlinModule"))
        val javaModule = resolver.getClassDeclarationByName(resolver.getKSNameFromString("fixture.JavaModule"))
        environment.logger.info("round=$round kotlinPresent=${kotlinModule != null} javaPresent=${javaModule != null}")
        return (kotlinModule != null) == expectPresent && (javaModule != null) == expectPresent
    }

    private fun annotationValue(declaration: KSClassDeclaration, annotation: String, argument: String): Any? =
        declaration.annotations.single { it.shortName.asString() == annotation }
            .arguments.single { it.name?.asString() == argument }.value

    override fun finish() {
        if (pendingConsumer) {
            environment.logger.error("Generated modules unavailable to dirty consumer")
        }
    }
}

class CleanOutputsProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = CleanOutputsProcessor(environment)
}
