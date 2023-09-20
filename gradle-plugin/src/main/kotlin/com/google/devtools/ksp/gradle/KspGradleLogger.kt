package com.google.devtools.ksp.gradle

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import org.gradle.api.logging.LogLevel

class KspGradleLogger(val loglevel: LogLevel) : KSPLogger {
    private val messager = System.out

    override fun logging(message: String, symbol: KSNode?) {
        if (loglevel.ordinal <= LogLevel.DEBUG.ordinal)
            messager.println("v: [ksp] $message")
    }

    override fun info(message: String, symbol: KSNode?) {
        if (loglevel.ordinal <= LogLevel.INFO.ordinal)
            messager.println("i: [ksp] $message")
    }

    override fun warn(message: String, symbol: KSNode?) {
        if (loglevel.ordinal <= LogLevel.WARN.ordinal)
            messager.println("w: [ksp] $message")
    }

    override fun error(message: String, symbol: KSNode?) {
        if (loglevel.ordinal <= LogLevel.ERROR.ordinal)
            messager.println("e: [ksp] $message")
    }

    override fun exception(e: Throwable) {
        if (loglevel.ordinal <= LogLevel.ERROR.ordinal)
            messager.println("e: [ksp] $e")
    }
}
