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
import groovy.json.JsonSlurper
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import org.junit.Test
import org.w3c.dom.Node
import org.w3c.dom.NodeList

private const val KSP_GROUP_ID = "com.google.devtools.ksp"
private const val SNAPSHOT_RESOURCE = "published-metadata.txt"

class PublishedMetadataTest {
    private val testConfig = TestConfig.read()
    private val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    private val xpath = XPathFactory.newInstance().newXPath()

    @Test
    fun checkPublishedMetadata() {
        val kspVersion = testConfig.kspVersion
        val groupDir = testConfig.mavenRepoDir.resolve(KSP_GROUP_ID.replace('.', '/'))
        val dirs =
            groupDir
                .listFiles()
                .orEmpty()
                .filter { it.resolve(kspVersion).isDirectory }
                .sortedBy { it.name }
        assertThat(dirs).isNotEmpty()
        val actual =
            dirs.joinToString("\n") {
                describeArtifact(it.name, it.resolve(kspVersion), kspVersion)
            }

        if (System.getProperty("updateTestSnapshots") != null) {
            val relPath =
                "gradle-plugin/src/test/resources/com/google/devtools/ksp/gradle/$SNAPSHOT_RESOURCE"
            testConfig.kspProjectDir
                .resolve(relPath)
                .apply { parentFile.mkdirs() }
                .writeText(actual, Charsets.UTF_8)
            return
        }

        val expected =
            checkNotNull(javaClass.getResource(SNAPSHOT_RESOURCE)).readText().replace("\r\n", "\n")
        val cmd =
            "./gradlew :gradle-plugin:test --tests \"*PublishedMetadataTest\" -DupdateTestSnapshots=true"
        assertWithMessage("Published Maven/Gradle metadata changed. If intentional, run: $cmd")
            .that(actual)
            .isEqualTo(expected)
    }

    private fun describeArtifact(artifactId: String, versionDir: File, kspVersion: String): String {
        val resolved =
            if (kspVersion.endsWith("-SNAPSHOT")) {
                val meta = docBuilder.parse(versionDir.resolve("maven-metadata.xml"))
                val ts = meta.eval("/metadata/versioning/snapshot/timestamp")
                val bn = meta.eval("/metadata/versioning/snapshot/buildNumber")
                "${kspVersion.removeSuffix("-SNAPSHOT")}-$ts-$bn"
            } else kspVersion
        val pom = docBuilder.parse(versionDir.resolve("$artifactId-$resolved.pom"))
        val project = xpath.evaluate("/project", pom, XPathConstants.NODE) as Node
        val groupId = project.eval("groupId")
        val version = normVer(groupId, project.eval("version"), kspVersion)
        val packaging = project.eval("packaging")
        val deps =
            xpath.evaluate("dependencies/dependency", project, XPathConstants.NODESET) as NodeList
        return buildString {
            appendLine("$groupId:${project.eval("artifactId")}:$version")
            if (packaging.isNotEmpty()) appendLine("  packaging=$packaging")
            appendLine("  pomName=${project.eval("name")}")
            appendLine("  description=${project.eval("description")}")
            if (deps.length > 0) {
                appendLine("  dependencies:")
                for (i in 0 until deps.length) {
                    val d = deps.item(i)
                    val g = d.eval("groupId")
                    appendLine(
                        "    $g:${d.eval("artifactId")}:${normVer(g, d.eval("version"), kspVersion)} (${d.eval("scope")})"
                    )
                }
            }
            val moduleFile = versionDir.resolve("$artifactId-$resolved.module")
            if (moduleFile.isFile) append(describeModuleVariants(moduleFile, kspVersion))
        }
    }

    private fun describeModuleVariants(moduleFile: File, kspVersion: String): String {
        val root = JsonSlurper().parse(moduleFile, Charsets.UTF_8.name()) as Map<*, *>
        val variants = (root["variants"] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>()
        if (variants.isEmpty()) return ""
        return buildString {
            appendLine("  variants:")
            for (variant in variants) {
                appendLine("    ${variant["name"]}:")
                val attrs =
                    (variant["attributes"] as? Map<*, *>).orEmpty().entries.sortedBy {
                        it.key.toString()
                    }
                if (attrs.isNotEmpty()) {
                    appendLine("      attributes:")
                    attrs.forEach { (k, v) -> appendLine("        $k=$v") }
                }
                for (key in listOf("dependencies", "dependencyConstraints")) {
                    val deps = (variant[key] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>()
                    if (deps.isNotEmpty()) {
                        appendLine("      $key:")
                        deps.forEach { appendLine("        ${formatModuleDep(it, kspVersion)}") }
                    }
                }
            }
        }
    }

    private fun formatModuleDep(dep: Map<*, *>, kspVersion: String): String {
        val group = dep["group"].toString()
        val ver = (dep["version"] as? Map<*, *>).orEmpty()
        val clauses = buildList {
            for (k in listOf("requires", "strictly", "prefers")) {
                ver[k]?.let { add("$k ${normVer(group, it.toString(), kspVersion)}") }
            }
            (ver["rejects"] as? List<*>)?.let { list ->
                add(
                    "rejects " +
                        list.joinToString(" | ") { normVer(group, it.toString(), kspVersion) }
                )
            }
        }
        val suffix =
            if (clauses.size == 1 && clauses[0].startsWith("requires "))
                ":${clauses[0].removePrefix("requires ")}"
            else if (clauses.isNotEmpty()) " {${clauses.joinToString("; ")}}" else ""
        return "$group:${dep["module"]}$suffix"
    }

    private fun Node.eval(expr: String): String = xpath.evaluate(expr, this)

    private fun normVer(groupId: String, version: String, kspVersion: String): String =
        if (groupId == KSP_GROUP_ID && version == kspVersion) "\$KSP_VERSION" else version
}
