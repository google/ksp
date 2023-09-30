package com.google.devtools.ksp.impl

import java.io.File
import java.io.Serializable

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
