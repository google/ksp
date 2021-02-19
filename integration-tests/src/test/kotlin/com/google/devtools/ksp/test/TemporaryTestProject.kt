package com.google.devtools.ksp.test

import org.junit.rules.TemporaryFolder
import java.io.File

class TemporaryTestProject(projectName: String) : TemporaryFolder() {
    private val testProjectSrc = File("src/test/resources", projectName)

    override fun before() {
        super.before()

        testProjectSrc.copyRecursively(root)

        val kotlinVersion = System.getProperty("kotlinVersion")
        val kspVersion = System.getProperty("kspVersion")
        val agpVersion = System.getProperty("agpVersion")
        val testRepo = System.getProperty("testRepo")
        val gradleProperties = File(root, "gradle.properties")
        gradleProperties.appendText("\nkotlinVersion=$kotlinVersion")
        gradleProperties.appendText("\nkspVersion=$kspVersion")
        gradleProperties.appendText("\nagpVersion=$agpVersion")
        gradleProperties.appendText("\ntestRepo=$testRepo")
    }

    fun restore(file: String) {
        File(testProjectSrc, file).copyTo(File(root, file), true)
    }
}