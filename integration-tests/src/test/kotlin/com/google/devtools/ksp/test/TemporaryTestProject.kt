package com.google.devtools.ksp.test

import org.junit.rules.TemporaryFolder
import java.io.File

class TemporaryTestProject(
    projectName: String,
    baseProject: String? = null,
    val useK2: Boolean = false,
) : TemporaryFolder() {
    private val testProjectSrc = File("src/test/resources", projectName)
    private val baseProjectSrc = baseProject?.let { File("src/test/resources", baseProject) }
    private val gradleProperties: File
        get() = File(root, "gradle.properties")

    override fun before() {
        super.before()

        baseProjectSrc?.copyRecursively(root)
        testProjectSrc.copyRecursively(root, true)

        val kotlinVersion = System.getProperty("kotlinVersion")
        val kspVersion = System.getProperty("kspVersion")
        val agpVersion = System.getProperty("agpVersion")
        val testRepo = System.getProperty("testRepo").replace(File.separator, "/")
        appendProperty("kotlinVersion=$kotlinVersion")
        appendProperty("kspVersion=$kspVersion")
        appendProperty("agpVersion=$agpVersion")
        appendProperty("testRepo=$testRepo")
        appendProperty("org.gradle.unsafe.configuration-cache=true")
        appendProperty("kotlin.jvm.target.validation.mode=warning")
        appendProperty("ksp.incremental.log=true")

        appendProperty("org.gradle.jvmargs=-Xmx4096M -XX:MaxMetaspaceSize=1024m")
        appendProperty("kotlin.daemon.jvmargs=-Xmx4096M -XX:MaxMetaspaceSize=1024m")

        // Update `params` in test classses to enable / disable K2.
        appendProperty("ksp.useK2=$useK2")

        // Uncomment this to debug compiler and compiler plugin.
        // gradleProperties.appendText("\nsystemProp.kotlin.compiler.execution.strategy=in-process")
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

    fun appendProperty(property: String) {
        gradleProperties.appendText("\n$property")
    }
}
