package com.google.devtools.ksp.gradle

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

class KspGradleLogger : KSPLogger {
    // TODO: support logging level.
    private val messager = System.out

    override fun logging(message: String, symbol: KSNode?) {
        messager.println("v: [ksp] $message")
    }

    override fun info(message: String, symbol: KSNode?) {
        messager.println("i: [ksp] $message")
    }

    override fun warn(message: String, symbol: KSNode?) {
        messager.println("w: [ksp] $message")
    }

    override fun error(message: String, symbol: KSNode?) {
        messager.println("e: [ksp] $message")
    }

    override fun exception(e: Throwable) {
        messager.println("e: [ksp] $e")
    }
}
