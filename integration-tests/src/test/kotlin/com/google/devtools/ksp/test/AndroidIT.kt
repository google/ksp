package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File

class AndroidIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground-android", "playground")

    @Test
    fun testPlaygroundAndroid() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        // Disabling configuration cache. See https://github.com/google/ksp/issues/299 for details
        gradleRunner.withArguments("clean", "build", "minifyReleaseWithR8", "--configuration-cache-problems=warn", "--info", "--stacktrace").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:build")?.outcome)
            val mergedConfiguration = File(project.root, "workload/build/outputs/mapping/release/configuration.txt")
            assert(mergedConfiguration.exists()) {
                "Merged configuration file not found!\n${printDirectoryTree(project.root)}"
            }
            val configurationText = mergedConfiguration.readText()
            assert("-keep class com.example.AClassBuilder { *; }" in configurationText) {
                "Merged configuration did not contain generated proguard rules!\n$configurationText"
            }
        }
    }
}

/**
 * Pretty print the directory tree and its file names.
 *
 * @param folder
 * must be a folder.
 * @return
 */
fun printDirectoryTree(folder: File): String? {
    require(folder.isDirectory) { "folder is not a Directory" }
    val indent = 0
    val sb = StringBuilder()
    printDirectoryTree(folder, indent, sb)
    return sb.toString()
}

private fun printDirectoryTree(
    folder: File, indent: Int,
    sb: StringBuilder
) {
    require(folder.isDirectory) { "folder is not a Directory" }
    sb.append(getIndentString(indent))
    sb.append("+--")
    sb.append(folder.name)
    sb.append("/")
    sb.append("\n")
    for (file in folder.listFiles()) {
        if (file.isDirectory) {
            printDirectoryTree(file, indent + 1, sb)
        } else {
            printFile(file, indent + 1, sb)
        }
    }
}

private fun printFile(file: File, indent: Int, sb: StringBuilder) {
    sb.append(getIndentString(indent))
    sb.append("+--")
    sb.append(file.name)
    sb.append("\n")
}

private fun getIndentString(indent: Int): String? {
    val sb = StringBuilder()
    for (i in 0 until indent) {
        sb.append("|  ")
    }
    return sb.toString()
}
