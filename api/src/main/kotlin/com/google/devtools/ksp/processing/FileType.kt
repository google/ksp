package com.google.devtools.ksp.processing

/**
 * Used to determine where files should be stored when being
 * created through the [CodeGenerator].
 */
enum class FileType {
    CLASS,
    JAVA_SOURCE,
    KOTLIN_SOURCE,
    RESOURCE
}
