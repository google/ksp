package com.google.devtools.ksp.test.utils

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.junit.jupiter.api.Assertions
import java.io.File
import java.util.jar.JarFile

fun assertMergedConfigurationOutput(project: TemporaryTestProject, expectedOutput: String) {
    val mergedConfiguration = File(project.root, "workload/build/outputs/mapping/release/configuration.txt")
    Assertions.assertTrue(mergedConfiguration.exists()) {
        "Merged configuration file not found!\n${printDirectoryTree(project.root)}"
    }
    val configurationText = mergedConfiguration.readText()
    Assertions.assertTrue(expectedOutput in configurationText) {
        "Merged configuration did not contain expected output!\n"
    }
}

fun JarFile.assertContainsNonNullEntry(path: String) {
    val entry = getEntry(path)
    Assertions.assertNotNull(entry) { "Entry '$path' should exist in the JAR file." }
    Assertions.assertTrue(entry.size > 0) { "Entry '$path' should not be empty." }
}
