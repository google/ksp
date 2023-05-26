package com.google.devtools.ksp

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider
import java.io.File

class AbsolutePathProvider(
    @Input
    val argumentName: String,
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    val file: File
): CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String>  = listOf("-D$argumentName=${file.absolutePath}")
}
