package com.google.devtools.ksp.cmdline

import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.KSPConfig
import com.google.devtools.ksp.processing.KspGradleLogger
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.processing.kspJvmArgParser
import com.google.devtools.ksp.processing.kspJvmArgParserHelp
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader
import kotlin.system.exitProcess

class KSPJvmMain {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if ("-h" in args || "--help" in args) {
                printHelpMsg(kspJvmArgParserHelp())
            } else {
                runWithArgs(args, ::kspJvmArgParser)
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
    val logger = KspGradleLogger(KspGradleLogger.LOGGING_LEVEL_WARN)
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
}
