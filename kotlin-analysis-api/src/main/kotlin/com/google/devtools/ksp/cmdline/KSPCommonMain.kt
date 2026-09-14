package com.google.devtools.ksp.cmdline

import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.KSPConfig
import com.google.devtools.ksp.processing.KspGradleLogger
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.processing.kspCommonArgParser
import com.google.devtools.ksp.processing.kspCommonArgParserHelp
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader
import kotlin.system.exitProcess

class KSPCommonMain {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if ("-h" in args || "--help" in args) {
                printHelpMsg(kspCommonArgParserHelp())
            } else {
                runWithArgs(args, ::kspCommonArgParser)
            }
        }
    }
}

internal fun printHelpMsg(optionsList: String) {
    println("Available options:")
    println(optionsList)
    println("where:")
    println(" * is required")
    println(" List is <path-separator> separated. E.g., arg1:arg2:arg3 on Linux/Mac, or arg1;arg2;arg3 on Windows")
    println(" Map is in the form key1=value1:key2=value2 on Linux/Mac or key1=value1;key2=value2 on Windows")
}

internal fun runWithArgs(args: Array<String>, parse: (Array<String>) -> Pair<KSPConfig, List<String>>) {

    val loggingVerbosity = System.getProperty("ksp.logging", "warn")
    val loggingLevel = when (loggingVerbosity.lowercase()) {
        "error" -> KspGradleLogger.LOGGING_LEVEL_ERROR
        "warn" -> KspGradleLogger.LOGGING_LEVEL_WARN
        "warning" -> KspGradleLogger.LOGGING_LEVEL_WARN
        "info" -> KspGradleLogger.LOGGING_LEVEL_INFO
        "debug" -> KspGradleLogger.LOGGING_LEVEL_LOGGING
        else -> KspGradleLogger.LOGGING_LEVEL_WARN
    }

    val logger = KspGradleLogger(loggingLevel)

    try {
        val (config, classpath) = parse(args)
        val processorClassloader = URLClassLoader(classpath.map { File(it).toURI().toURL() }.toTypedArray())

        @Suppress("UNCHECKED_CAST")
        val processorProviders = ServiceLoader
            .load(
                processorClassloader.loadClass("com.google.devtools.ksp.processing.SymbolProcessorProvider"),
                processorClassloader
            )
            .toList() as List<SymbolProcessorProvider>

        val exitCode = KotlinSymbolProcessing(config, processorProviders, logger).execute()
        exitProcess(exitCode.code)
    } catch (t: Throwable) {
        // Manually print stack trace and log the exception if error occurred.
        // Then call exitProcess to ensure the process is halted.
        logger.exception(t)
        t.printStackTrace()
        exitProcess(KotlinSymbolProcessing.ExitCode.PROCESSING_ERROR.code)
    }
}
