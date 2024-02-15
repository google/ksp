import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar
import java.util.zip.ZipFile

evaluationDependsOn(":kotlin-analysis-api")

val kotlinBaseVersion: String by project
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
    "org.jetbrains.",
    "org.intellij.",
    "com.intellij.",
    "it.unimi.dsi.",
    "com.google.common.",
    "com.google.errorprone.",
    "com.google.gwt.",
    "com.google.j2objc.",
    "kotlin.sequences.",
    "kotlin.text.",
    "org.checkerframework.",
    "com.github.benmanes.caffeine.",
    "org.jdom.",
    "org.picocontainer.",
    "net.rubygrapefruit.",
    "com.fasterxml.",
    "org.codehaus.",
    "one.util.",
    "FirNativeForwardDeclarationGetClassCallChecker",
).map {
    Pair(it, "ksp." + it)
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    // ShadowJar picks up the `compile` configuration by default and pulls stdlib in.
    // Therefore, specifying another configuration instead.
    configurations = listOf(packedJars)
    prefixesToRelocate.forEach { (f, t) ->
        relocate(f, t)
    }
    mergeServiceFiles()
}

fun String.replaceWithKsp() =
    prefixesToRelocate.fold(this) { acc, (f, t) ->
        acc.replace("package $f", "package $t")
            .replace("import $f", "import $t")
    }

val DEP_SOURCES_DIR = "$buildDir/source-jar"

val validPaths = prefixesToRelocate.map {
    it.second.split('.').filter { it.isNotEmpty() }.joinToString("/")
} + listOf(
    "com/google/devtools/ksp",
    "kotlin",
    "kotlinx",
    "META-INF",
    "org/apache/log4j/package-info.class",
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
        into("$DEP_SOURCES_DIR/ksp")
    }
    val sourcesJar by creating(Jar::class) {
        dependsOn(copyDeps)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveClassifier.set("sources")
        from(project(":kotlin-analysis-api").sourceSets.main.get().allSource)
        from(project(":common-util").sourceSets.main.get().allSource)
        from(DEP_SOURCES_DIR)
        filter { it.replaceWithKsp() }
    }

    // All bundled dependencies should be renamed.
    val validate by creating(DefaultTask::class) {
        dependsOn(shadowJar)
        doLast {
            val violatingFiles = mutableListOf<String>()
            shadowJar.get().outputs.files.filter {
                it.extension == "jar"
            }.forEach {
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

    publish {
        dependsOn(shadowJar)
        dependsOn(sourcesJar)
        dependsOn(project(":kotlin-analysis-api").tasks["dokkaJavadocJar"])
        dependsOn(validate)
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
