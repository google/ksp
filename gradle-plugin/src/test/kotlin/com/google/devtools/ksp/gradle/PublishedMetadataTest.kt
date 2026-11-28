/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.devtools.ksp.gradle

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.devtools.ksp.gradle.testing.TestConfig
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import org.junit.Test
import org.w3c.dom.Node
import org.w3c.dom.NodeList

private const val KSP_GROUP_ID = "com.google.devtools.ksp"
private const val GOLDEN = "published-metadata.json"

/**
 * Compares the POMs and Gradle Module Metadata of all published KSP artifacts against [GOLDEN].
 *
 * `./gradlew :gradle-plugin:checkPublishedMetadata` runs this test, and `updatePublishedMetadata`
 * runs it with the system property `updateTestSnapshots=true`, which makes it write the golden.
 */
class PublishedMetadataTest {
    private val testConfig = TestConfig.read()
    private val kspVersion = testConfig.kspVersion
    private val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    private val xpath = XPathFactory.newInstance().newXPath()

    @Test
    fun checkPublishedMetadata() {
        val groupDir = testConfig.mavenRepoDir.resolve(KSP_GROUP_ID.replace('.', '/'))
        val artifactDirs = groupDir.listFiles().orEmpty().sortedBy { it.name }
        val versionDirs = artifactDirs.map { it.resolve(kspVersion) }.filter { it.isDirectory }
        assertThat(versionDirs).isNotEmpty()
        val artifacts = versionDirs.map { readArtifact(it) }
        val json = JsonOutput.prettyPrint(JsonOutput.toJson(artifacts.map { it.toJson() }))
        val actual = json.replace("\r\n", "\n") + "\n"

        if (System.getProperty("updateTestSnapshots") == "true") {
            val packageDir = javaClass.packageName.replace('.', '/')
            val golden = "gradle-plugin/src/test/resources/$packageDir/$GOLDEN"
            testConfig.kspProjectDir.resolve(golden).apply { parentFile.mkdirs() }.writeText(actual)
            return
        }

        val expected = checkNotNull(javaClass.getResource(GOLDEN)).readText().replace("\r\n", "\n")
        assertWithMessage(
                "Published Maven/Gradle metadata changed. If intentional, run " +
                    "./gradlew :gradle-plugin:updatePublishedMetadata"
            )
            .that(actual)
            .isEqualTo(expected)
    }

    private fun readArtifact(versionDir: File): Artifact {
        val artifactId = versionDir.parentFile.name
        val version =
            if (kspVersion.endsWith("-SNAPSHOT")) {
                val metadata = docBuilder.parse(versionDir.resolve("maven-metadata.xml"))
                val timestamp = metadata.eval("/metadata/versioning/snapshot/timestamp")
                val buildNumber = metadata.eval("/metadata/versioning/snapshot/buildNumber")
                "${kspVersion.removeSuffix("-SNAPSHOT")}-$timestamp-$buildNumber"
            } else kspVersion
        val pom = docBuilder.parse(versionDir.resolve("$artifactId-$version.pom"))
        val project = xpath.evaluate("/project", pom, XPathConstants.NODE) as Node
        val dependencies =
            xpath.evaluate("dependencies/dependency", project, XPathConstants.NODESET) as NodeList
        val moduleFile = versionDir.resolve("$artifactId-$version.module")
        return Artifact(
            coordinates = project.coordinates(),
            packaging = project.eval("packaging").ifEmpty { null },
            name = project.eval("name"),
            description = project.eval("description"),
            dependencies =
                (0 until dependencies.length).map { readPomDependency(dependencies.item(it)) },
            variants = if (moduleFile.isFile) readVariants(moduleFile) else emptyList(),
        )
    }

    private fun readPomDependency(dependency: Node) =
        PomDependency(dependency.coordinates(), dependency.eval("scope").ifEmpty { null })

    private fun readVariants(moduleFile: File): List<Variant> {
        val module = JsonSlurper().parse(moduleFile, Charsets.UTF_8.name()) as Map<*, *>
        return module.listOfMaps("variants").map { variant ->
            val attributes = (variant["attributes"] as? Map<*, *>).orEmpty()
            Variant(
                name = variant["name"].toString(),
                attributes = attributes.mapKeys { it.key.toString() }.toSortedMap(),
                dependencies = variant.listOfMaps("dependencies").map { readModuleDependency(it) },
                dependencyConstraints =
                    variant.listOfMaps("dependencyConstraints").map { readModuleDependency(it) },
            )
        }
    }

    private fun readModuleDependency(dependency: Map<*, *>): ModuleDependency {
        val group = dependency["group"].toString()
        val version = (dependency["version"] as? Map<*, *>).orEmpty()
        // In a fixed order, so that the golden is deterministic.
        val constraints =
            listOf("requires", "strictly", "prefers", "rejects")
                .filter { it in version }
                .associateWith { key ->
                    when (val value = version[key]) {
                        is List<*> -> value.map { normalize(group, it.toString()) }
                        else -> normalize(group, value.toString())
                    }
                }
        return ModuleDependency("$group:${dependency["module"]}", constraints)
    }

    private fun Map<*, *>.listOfMaps(key: String): List<Map<*, *>> =
        (this[key] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>()

    private fun Node.eval(expr: String): String = xpath.evaluate(expr, this)

    /** `group:artifact:version` of a POM project or dependency. */
    private fun Node.coordinates(): String {
        val group = eval("groupId")
        return "$group:${eval("artifactId")}:${normalize(group, eval("version"))}"
    }

    /** Replaces the version of KSP's own artifacts with `$KSP_VERSION`. */
    private fun normalize(group: String, version: String): String =
        if (group == KSP_GROUP_ID && version == kspVersion) "\$KSP_VERSION" else version
}

// toJson() fixes the order of the keys and leaves out empty values. JsonOutput writes the JSON.

private data class Artifact(
    val coordinates: String,
    val packaging: String?,
    val name: String,
    val description: String,
    val dependencies: List<PomDependency>,
    val variants: List<Variant>,
) {
    fun toJson() =
        jsonObject(
            "coordinates" to coordinates,
            "packaging" to packaging,
            "name" to name,
            "description" to description,
            "dependencies" to dependencies.map { it.toJson() },
            "variants" to variants.map { it.toJson() },
        )
}

private data class PomDependency(val coordinates: String, val scope: String?) {
    fun toJson() = jsonObject("coordinates" to coordinates, "scope" to scope)
}

private data class Variant(
    val name: String,
    val attributes: Map<String, Any?>,
    val dependencies: List<ModuleDependency>,
    val dependencyConstraints: List<ModuleDependency>,
) {
    fun toJson() =
        jsonObject(
            "name" to name,
            "attributes" to attributes,
            "dependencies" to dependencies.map { it.toJson() },
            "dependencyConstraints" to dependencyConstraints.map { it.toJson() },
        )
}

private data class ModuleDependency(val module: String, val version: Map<String, Any>) {
    fun toJson() = jsonObject("module" to module, "version" to version)
}

private fun jsonObject(vararg entries: Pair<String, Any?>): Map<String, Any?> =
    entries.filterNot { (_, value) -> value.isNullOrEmpty() }.toMap(LinkedHashMap())

private fun Any?.isNullOrEmpty() =
    this == null || this is Collection<*> && isEmpty() || this is Map<*, *> && isEmpty()
