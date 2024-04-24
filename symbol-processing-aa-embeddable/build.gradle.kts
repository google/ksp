import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.gradle.jvm.tasks.Jar
import shadow.org.apache.tools.zip.ZipEntry
import shadow.org.apache.tools.zip.ZipOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipFile

evaluationDependsOn(":kotlin-analysis-api")

val signingKey: String? by project
val signingPassword: String? by project

val aaKotlinBaseVersion: String by project
val aaIntellijVersion: String by project

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    `maven-publish`
    signing
}

val packedJars by configurations.creating

dependencies {
    packedJars(project(":kotlin-analysis-api", "shadow")) { isTransitive = false }
}

tasks.withType<Jar> {
    archiveClassifier.set("real")
}

val prefixesToRelocate = listOf(
    "com.fasterxml.",
    "org.codehaus.",
    "com.github.benmanes.caffeine.",
    "com.google.common.",
    "com.google.devtools.ksp.common.",
    "com.google.errorprone.",
    "com.google.gwt.",
    "com.google.j2objc.",
    "com.intellij.",
    "com.sun.jna.",
    "gnu.trove.",
    "it.unimi.dsi.",
    "javaslang.",
    "javax.inject.",
    "kotlinx.collections.immutable.",
    "kotlinx.coroutines.",
    "org.apache.log4j.",
    "org.checkerframework.",
    "org.intellij.",
    "org.jetbrains.",
    "org.jdom.",
    "org.picocontainer.",
    "one.util.",
    "net.jpountz.",
    "net.rubygrapefruit.",
    "FirNativeForwardDeclarationGetClassCallChecker",
).map {
    Pair(it, "ksp." + it)
}

class AAServiceTransformer : com.github.jengelman.gradle.plugins.shadow.transformers.Transformer {
    private val entries = HashMap<String, String>()

    // Names of extension points needs to be relocated, because ShadowJar does that, too.
    private val regex = Regex("\"((org\\.jetbrains\\.kotlin\\.|com\\.intellij\\.).+)\"")

    override fun canTransformResource(element: FileTreeElement): Boolean {
        return element.name.startsWith("META-INF/analysis-api/") ||
            element.name == "META-INF/extensions/compiler.xml"
    }

    override fun getName(): String {
        return "AAServiceTransformer"
    }

    override fun hasTransformedResource(): Boolean {
        return entries.size > 0
    }

    override fun modifyOutputStream(os: ZipOutputStream, preserveFileTimestamps: Boolean) {
        fun putOneEntry(path: String, content: String) {
            val entry = ZipEntry(path)
            entry.time = TransformerContext.getEntryTimestamp(preserveFileTimestamps, entry.time)
            os.putNextEntry(entry)
            os.write(content.toByteArray())
            os.closeEntry()
        }

        entries.forEach { path, original ->
            val patched = original.replace(regex, "\"ksp.$1\"")
            when {
                path.startsWith("META-INF/analysis-api/") -> {
                    val more = patched.replace(
                        "\"/META-INF/extensions/compiler.xml\"",
                        "\"/META-INF/extensions/ksp_compiler.xml\""
                    )
                    putOneEntry(path, more)
                }
                path == "META-INF/extensions/compiler.xml" -> {
                    // Keep compiler.xml the same, and patch ksp_compiler.xml.
                    putOneEntry(path, original)
                    putOneEntry("META-INF/extensions/ksp_compiler.xml", patched)
                }
                else -> {
                    putOneEntry(path, patched)
                }
            }
        }
    }

    override fun transform(context: TransformerContext) {
        val path = context.path
        val content = InputStreamReader(context.`is`).readText()
        entries.put(path, content)
    }
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    // ShadowJar picks up the `compile` configuration by default and pulls stdlib in.
    // Therefore, specifying another configuration instead.
    configurations = listOf(packedJars)
    prefixesToRelocate.forEach { (f, t) ->
        relocate(f, t) {
            // Do not rename this hardcoded string in XmlReader.
            exclude("com.intellij.projectService")
        }
    }
    mergeServiceFiles()
    exclude("META-INF/compiler.version")
    exclude("META-INF/*.kotlin_module")

    this.transform(AAServiceTransformer())

    // All bundled dependencies should be renamed.
    doLast {
        val violatingFiles = mutableListOf<String>()
        archiveFile.get().asFile.let {
            for (e in ZipFile(it).entries()) {
                if (e.name.endsWith(".class") and !validPackages.contains(e.name))
                    violatingFiles.add(e.name)
            }
        }
        if (violatingFiles.isNotEmpty()) {
            error(
                "Detected unrelocated classes that may cause conflicts: " +
                    violatingFiles.joinToString(System.lineSeparator())
            )
        }
    }
}

val prefixesToRelocateStripped = prefixesToRelocate.map {
    Pair(it.first.trim('.'), it.second.trim('.'))
}

// TODO: match with Trie
fun String.replaceWithKsp() =
    prefixesToRelocateStripped.fold(this) { acc, (f, t) ->
        acc.replace("package $f", "package $t")
            .replace("import $f", "import $t")
    }

val DEP_SOURCES_DIR = "$buildDir/source-jar"

val validPaths = prefixesToRelocate.map {
    it.second.split('.').filter { it.isNotEmpty() }.joinToString("/")
} + listOf(
    "com/google/devtools/ksp",
    "META-INF",
    "ksp/FirNativeForwardDeclarationGetClassCallChecker.class",
)

class Trie(paths: List<String>) {
    class TrieNode(val key: String)

    private val terminals = mutableSetOf<TrieNode>()

    private val m = mutableMapOf<Pair<TrieNode?, String>, TrieNode>().apply {
        paths.forEach { path ->
            var p: TrieNode? = null
            for (d in path.split("/")) {
                p = getOrPut(Pair(p, d)) { TrieNode(d) }
            }
            terminals.add(p!!)
        }
    }

    fun contains(s: String): Boolean {
        var p: TrieNode? = null
        for (d in s.split("/")) {
            p = m.get(Pair(p, d))?.also {
                if (it in terminals)
                    return true
            } ?: return false
        }
        return true
    }
}

val validPackages = Trie(validPaths)

tasks {
    val copyDeps by creating(Copy::class) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        project(":kotlin-analysis-api").configurations.getByName("depSourceJars").resolve().forEach {
            from(zipTree(it))
        }
        from(project(":common-util").sourceSets.main.get().allSource)
        into("$DEP_SOURCES_DIR/ksp")
    }
    val sourcesJar by creating(Jar::class) {
        dependsOn(copyDeps)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveClassifier.set("sources")
        from(project(":kotlin-analysis-api").sourceSets.main.get().allSource)
        from(DEP_SOURCES_DIR)
        filter { it.replaceWithKsp() }
    }

    publish {
        dependsOn(shadowJar)
        dependsOn(sourcesJar)
        dependsOn(project(":kotlin-analysis-api").tasks["dokkaJavadocJar"])
    }
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            artifactId = "symbol-processing-aa-embeddable"
            artifact(project(":kotlin-analysis-api").tasks["dokkaJavadocJar"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["shadowJar"])
            pom {
                name.set("com.google.devtools.ksp:symbol-processing-aa-embeddable")
                description.set("KSP implementation on Kotlin Analysis API")
                withXml {
                    fun groovy.util.Node.addDependency(
                        groupId: String,
                        artifactId: String,
                        version: String,
                        scope: String = "runtime"
                    ) {
                        appendNode("dependency").apply {
                            appendNode("groupId", groupId)
                            appendNode("artifactId", artifactId)
                            appendNode("version", version)
                            appendNode("scope", scope)
                        }
                    }

                    asNode().appendNode("dependencies").apply {
                        addDependency("org.jetbrains.kotlin", "kotlin-stdlib", aaKotlinBaseVersion)
                        addDependency("com.google.devtools.ksp", "symbol-processing-api", version)
                        addDependency("com.google.devtools.ksp", "symbol-processing-common-deps", version)
                    }
                }
            }
        }
    }
}

signing {
    isRequired = hasProperty("signingKey")
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(extensions.getByType<PublishingExtension>().publications)
}

abstract class WriteVersionSrcTask @Inject constructor(
    @get:Input val kotlinVersion: String,
    @get:OutputDirectory val outputResDir: File
) : DefaultTask() {
    @TaskAction
    fun generate() {
        val metaInfDir = File(outputResDir, "META-INF")
        metaInfDir.mkdirs()
        File(metaInfDir, "ksp.compiler.version").writeText(kotlinVersion)
    }
}

val kspVersionDir = File(project.buildDir, "generated/ksp-versions/META-INF")
val writeVersionSrcTask = tasks.register<WriteVersionSrcTask>(
    "generateKSPVersions",
    aaKotlinBaseVersion,
    kspVersionDir
)

kotlin {
    sourceSets {
        main {
            resources.srcDir(writeVersionSrcTask.map { it.outputResDir })
        }
    }
}
