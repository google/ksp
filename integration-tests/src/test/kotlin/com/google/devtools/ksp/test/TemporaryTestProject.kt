package com.google.devtools.ksp.test

import org.junit.rules.TemporaryFolder
import java.io.File

class TemporaryTestProject(projectName: String, baseProject: String? = null) : TemporaryFolder() {
    private val testProjectSrc = File("src/test/resources", projectName)
    private val baseProjectSrc = baseProject?.let { File("src/test/resources", baseProject) }

    override fun before() {
        super.before()

        baseProjectSrc?.copyRecursively(root)
        testProjectSrc.copyRecursively(root, true)

        val kotlinVersion = System.getProperty("kotlinVersion")
        val kspVersion = System.getProperty("kspVersion")
        val agpVersion = System.getProperty("agpVersion")
        val testRepo = System.getProperty("testRepo")
        val gradleProperties = File(root, "gradle.properties")
        gradleProperties.appendText("\nkotlinVersion=$kotlinVersion")
        gradleProperties.appendText("\nkspVersion=$kspVersion")
        gradleProperties.appendText("\nagpVersion=$agpVersion")
        gradleProperties.appendText("\ntestRepo=$testRepo")
        // Uncomment this to debug compiler and compiler plugin.
        // gradleProperties.appendText("\nkotlin.compiler.execution.strategy=in-process")
    }

    fun restore(file: String) {
        fun copySafe(src: File, dst: File) {
            if (src.exists())
                src.copyTo(dst, true)
        }
        baseProjectSrc?.let {
            copySafe(File(baseProjectSrc, file), File(root, file))
        }
        copySafe(File(testProjectSrc, file), File(root, file))
    }
}
