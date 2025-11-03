package com.google.devtools.ksp.test.utils

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.junit.Assert
import java.io.File
import java.util.jar.JarFile

fun assertMergedConfigurationOutput(project: TemporaryTestProject, expectedOutput: String) {
    val mergedConfiguration = File(project.root, "workload/build/outputs/mapping/release/configuration.txt")
    assert(mergedConfiguration.exists()) {
        "Merged configuration file not found!\n${printDirectoryTree(project.root)}"
    }
    val configurationText = mergedConfiguration.readText()
    assert(expectedOutput in configurationText) {
        "Merged configuration did not contain expected output!\n"
    }
}

fun JarFile.assertContainsNonNullEntry(path: String) {
    val entry = getEntry(path)
    Assert.assertNotNull("Entry '$path' should exist in the JAR file.", entry)
    Assert.assertTrue("Entry '$path' should not be empty.", entry.size > 0)
}
