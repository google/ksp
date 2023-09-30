package com.google.devtools.ksp.gradle

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

class KspGradleLogger(val loglevel: Int) : KSPLogger {
    private val messager = System.out

    override fun logging(message: String, symbol: KSNode?) {
        if (loglevel <= LOGGING_LEVEL_LOGGING)
            messager.println("v: [ksp] $message")
    }

    override fun info(message: String, symbol: KSNode?) {
        if (loglevel <= LOGGING_LEVEL_INFO)
            messager.println("i: [ksp] $message")
    }

    override fun warn(message: String, symbol: KSNode?) {
        if (loglevel <= LOGGING_LEVEL_WARN)
            messager.println("w: [ksp] $message")
    }

    override fun error(message: String, symbol: KSNode?) {
        if (loglevel <= LOGGING_LEVEL_ERROR)
            messager.println("e: [ksp] $message")
    }

    override fun exception(e: Throwable) {
        if (loglevel <= LOGGING_LEVEL_ERROR)
            messager.println("e: [ksp] $e")
    }

    companion object {
        const val LOGGING_LEVEL_LOGGING = 0
        const val LOGGING_LEVEL_INFO = 1
        const val LOGGING_LEVEL_WARN = 3
        const val LOGGING_LEVEL_ERROR = 5
    }
}
