package com.google.devtools.ksp.processing

import java.io.File
import java.io.Serializable

private annotation class KSPArgParserGen(val name: String)

abstract class KSPConfig(
    val moduleName: String,
    val sourceRoots: List<File>,
    val commonSourceRoots: List<File>,
    val libraries: List<File>,

    val processorOptions: Map<String, String>,

    val projectBaseDir: File,
    val outputBaseDir: File,
    val cachesDir: File,

    val classOutputDir: File,
    val kotlinOutputDir: File,
    val resourceOutputDir: File,

    val incremental: Boolean,
    val incrementalLog: Boolean,
    val modifiedSources: List<File>,
    val removedSources: List<File>,
    val changedClasses: List<String>,

    val languageVersion: String,
    val apiVersion: String,

    val allWarningsAsErrors: Boolean,
    val mapAnnotationArgumentsInJava: Boolean,
) : Serializable {
    abstract class Builder {
        lateinit var moduleName: String
        lateinit var sourceRoots: List<File>
        var commonSourceRoots: List<File> = emptyList()
        var libraries: List<File> = emptyList()

        var processorOptions = mapOf<String, String>()

        lateinit var projectBaseDir: File
        lateinit var outputBaseDir: File
        lateinit var cachesDir: File

        lateinit var classOutputDir: File
        lateinit var kotlinOutputDir: File
        lateinit var resourceOutputDir: File

        var incremental: Boolean = false
        var incrementalLog: Boolean = false
        var modifiedSources: List<File> = emptyList()
        var removedSources: List<File> = emptyList()
        var changedClasses: List<String> = emptyList()

        lateinit var languageVersion: String
        lateinit var apiVersion: String

        var allWarningsAsErrors: Boolean = false
        var mapAnnotationArgumentsInJava: Boolean = false
    }
}

class KSPJvmConfig(
    val javaSourceRoots: List<File>,
    val javaOutputDir: File,
    val jdkHome: File?,
    val jvmTarget: String,
    val jvmDefaultMode: String,
    moduleName: String,
    sourceRoots: List<File>,
    commonSourceRoots: List<File>,
    libraries: List<File>,

    processorOptions: Map<String, String>,

    projectBaseDir: File,
    outputBaseDir: File,
    cachesDir: File,

    classOutputDir: File,
    kotlinOutputDir: File,
    resourceOutputDir: File,

    incremental: Boolean,
    incrementalLog: Boolean,
    modifiedSources: List<File>,
    removedSources: List<File>,
    changedClasses: List<String>,

    languageVersion: String,
    apiVersion: String,

    allWarningsAsErrors: Boolean,
    mapAnnotationArgumentsInJava: Boolean,
) : KSPConfig(
    moduleName,
    sourceRoots,
    commonSourceRoots,
    libraries,

    processorOptions,

    projectBaseDir,
    outputBaseDir,
    cachesDir,

    classOutputDir,
    kotlinOutputDir,
    resourceOutputDir,

    incremental,
    incrementalLog,
    modifiedSources,
    removedSources,
    changedClasses,

    languageVersion,
    apiVersion,

    allWarningsAsErrors,
    mapAnnotationArgumentsInJava,
) {
    @KSPArgParserGen(name = "kspJvmArgParser")
    class Builder : KSPConfig.Builder(), Serializable {
        var javaSourceRoots: List<File> = emptyList()
        lateinit var javaOutputDir: File
        var jdkHome: File? = null
        lateinit var jvmTarget: String
        var jvmDefaultMode: String = "disable"

        fun build(): KSPJvmConfig {
            return KSPJvmConfig(
                javaSourceRoots,
                javaOutputDir,
                jdkHome,
                jvmTarget,
                jvmDefaultMode,

                moduleName,
                sourceRoots,
                commonSourceRoots,
                libraries,

                processorOptions,

                projectBaseDir,
                outputBaseDir,
                cachesDir,
                classOutputDir,
                kotlinOutputDir,
                resourceOutputDir,

                incremental,
                incrementalLog,
                modifiedSources,
                removedSources,
                changedClasses,

                languageVersion,
                apiVersion,

                allWarningsAsErrors,
                mapAnnotationArgumentsInJava
            )
        }
    }
}

class KSPNativeConfig(
    val targetName: String,
    moduleName: String,
    sourceRoots: List<File>,
    commonSourceRoots: List<File>,
    libraries: List<File>,

    processorOptions: Map<String, String>,

    projectBaseDir: File,
    outputBaseDir: File,
    cachesDir: File,

    classOutputDir: File,
    kotlinOutputDir: File,
    resourceOutputDir: File,

    incremental: Boolean,
    incrementalLog: Boolean,
    modifiedSources: List<File>,
    removedSources: List<File>,
    changedClasses: List<String>,

    languageVersion: String,
    apiVersion: String,

    allWarningsAsErrors: Boolean,
    mapAnnotationArgumentsInJava: Boolean,
) : KSPConfig(
    moduleName,
    sourceRoots,
    commonSourceRoots,
    libraries,

    processorOptions,

    projectBaseDir,
    outputBaseDir,
    cachesDir,

    classOutputDir,
    kotlinOutputDir,
    resourceOutputDir,

    incremental,
    incrementalLog,
    modifiedSources,
    removedSources,
    changedClasses,

    languageVersion,
    apiVersion,

    allWarningsAsErrors,
    mapAnnotationArgumentsInJava,
) {
    @KSPArgParserGen(name = "kspNativeArgParser")
    class Builder : KSPConfig.Builder(), Serializable {
        lateinit var target: String

        fun build(): KSPNativeConfig {
            return KSPNativeConfig(
                target,
                moduleName,
                sourceRoots,
                commonSourceRoots,
                libraries,

                processorOptions,

                projectBaseDir,
                outputBaseDir,
                cachesDir,
                classOutputDir,
                kotlinOutputDir,
                resourceOutputDir,

                incremental,
                incrementalLog,
                modifiedSources,
                removedSources,
                changedClasses,

                languageVersion,
                apiVersion,

                allWarningsAsErrors,
                mapAnnotationArgumentsInJava
            )
        }
    }
}

class KSPJsConfig(
    val backend: String,
    moduleName: String,
    sourceRoots: List<File>,
    commonSourceRoots: List<File>,
    libraries: List<File>,

    processorOptions: Map<String, String>,

    projectBaseDir: File,
    outputBaseDir: File,
    cachesDir: File,

    classOutputDir: File,
    kotlinOutputDir: File,
    resourceOutputDir: File,

    incremental: Boolean,
    incrementalLog: Boolean,
    modifiedSources: List<File>,
    removedSources: List<File>,
    changedClasses: List<String>,

    languageVersion: String,
    apiVersion: String,

    allWarningsAsErrors: Boolean,
    mapAnnotationArgumentsInJava: Boolean,
) : KSPConfig(
    moduleName,
    sourceRoots,
    commonSourceRoots,
    libraries,

    processorOptions,

    projectBaseDir,
    outputBaseDir,
    cachesDir,

    classOutputDir,
    kotlinOutputDir,
    resourceOutputDir,

    incremental,
    incrementalLog,
    modifiedSources,
    removedSources,
    changedClasses,

    languageVersion,
    apiVersion,

    allWarningsAsErrors,
    mapAnnotationArgumentsInJava,
) {
    @KSPArgParserGen(name = "kspJsArgParser")
    class Builder : KSPConfig.Builder(), Serializable {
        lateinit var backend: String

        fun build(): KSPJsConfig {
            return KSPJsConfig(
                backend,
                moduleName,
                sourceRoots,
                commonSourceRoots,
                libraries,

                processorOptions,

                projectBaseDir,
                outputBaseDir,
                cachesDir,
                classOutputDir,
                kotlinOutputDir,
                resourceOutputDir,

                incremental,
                incrementalLog,
                modifiedSources,
                removedSources,
                changedClasses,

                languageVersion,
                apiVersion,

                allWarningsAsErrors,
                mapAnnotationArgumentsInJava
            )
        }
    }
}

data class Target(
    val platform: String,
    val args: Map<String, String>
)

class KSPCommonConfig(
    val targets: List<Target>,
    moduleName: String,
    sourceRoots: List<File>,
    commonSourceRoots: List<File>,
    libraries: List<File>,

    processorOptions: Map<String, String>,

    projectBaseDir: File,
    outputBaseDir: File,
    cachesDir: File,

    classOutputDir: File,
    kotlinOutputDir: File,
    resourceOutputDir: File,

    incremental: Boolean,
    incrementalLog: Boolean,
    modifiedSources: List<File>,
    removedSources: List<File>,
    changedClasses: List<String>,

    languageVersion: String,
    apiVersion: String,

    allWarningsAsErrors: Boolean,
    mapAnnotationArgumentsInJava: Boolean,
) : KSPConfig(
    moduleName,
    sourceRoots,
    commonSourceRoots,
    libraries,

    processorOptions,

    projectBaseDir,
    outputBaseDir,
    cachesDir,

    classOutputDir,
    kotlinOutputDir,
    resourceOutputDir,

    incremental,
    incrementalLog,
    modifiedSources,
    removedSources,
    changedClasses,

    languageVersion,
    apiVersion,

    allWarningsAsErrors,
    mapAnnotationArgumentsInJava,
) {
    @KSPArgParserGen(name = "kspCommonArgParser")
    class Builder : KSPConfig.Builder(), Serializable {
        lateinit var targets: List<Target>

        fun build(): KSPCommonConfig {
            return KSPCommonConfig(
                targets,
                moduleName,
                sourceRoots,
                commonSourceRoots,
                libraries,

                processorOptions,

                projectBaseDir,
                outputBaseDir,
                cachesDir,
                classOutputDir,
                kotlinOutputDir,
                resourceOutputDir,

                incremental,
                incrementalLog,
                modifiedSources,
                removedSources,
                changedClasses,

                languageVersion,
                apiVersion,

                allWarningsAsErrors,
                mapAnnotationArgumentsInJava
            )
        }
    }
}

fun parseString(arg: String): String {
    if (arg.length > 0 && arg[0] == '-')
        throw IllegalArgumentException("expecting a String arguemnt but got $arg")
    return arg
}

fun parseBoolean(arg: String): Boolean {
    if (arg.length > 0 && arg[0] == '-')
        throw IllegalArgumentException("expecting a Boolean arguemnt but got $arg")
    return arg.toBoolean()
}

fun parseFile(arg: String): File {
    if (arg.length > 0 && arg[0] == '-')
        throw IllegalArgumentException("expecting a File arguemnt but got $arg")
    // FIXME: AA isn't happy relative paths for source roots.
    return File(arg).absoluteFile
}

fun <T> parseList(arg: String, transform: (String) -> T): List<T> {
    if (arg.length > 0 && arg[0] == '-')
        throw IllegalArgumentException("expecting a List but got $arg")
    return arg.split(':').map { transform(it) }
}

fun <T> parseMap(arg: String, transform: (String) -> T): Map<String, T> {
    if (arg.length > 0 && arg[0] == '-')
        throw IllegalArgumentException("expecting a Map but got $arg")
    return arg.split(':').map {
        val (k, v) = it.split('=')
        k to transform(v)
    }.toMap()
}

fun parseTarget(arg: String): Target {
    if (arg.length > 0 && arg[0] == '-')
        throw IllegalArgumentException("expecting a target but got $arg")
    return Target(arg, emptyMap())
}

fun getArg(args: Array<String>, i: Int): String {
    if (i >= args.size || args[i].startsWith("-"))
        throw IllegalArgumentException("Expecting an argument")
    return args[i]
}
