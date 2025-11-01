package com.google.devtools.ksp.test.utils

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import java.io.File

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
