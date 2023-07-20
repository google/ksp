package com.google.devtools.ksp

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.LocalState
import org.gradle.process.CommandLineArgumentProvider
import java.io.File

class RelativizingLocalPathProvider(
    @Input
    val argumentName: String,
    @LocalState
    val file: File
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf("-D$argumentName=${file.absolutePath}")
}
