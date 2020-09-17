/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.processing.impl

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.NonExistLocation
import java.io.PrintWriter
import java.io.StringWriter

class MessageCollectorBasedKSPLogger(private val messageCollector: MessageCollector) : KSPLogger {

    companion object {
        const val PREFIX = "[ksp] "
    }

    private fun convertMessage(message: String, symbol: KSNode?): String =
        when (val location = symbol?.location) {
            is FileLocation -> "$PREFIX${location.filePath}:${location.lineNumber}: $message"
            is NonExistLocation, null -> "$PREFIX$message"
        }

    override fun logging(message: String, symbol: KSNode?) {
        messageCollector.report(CompilerMessageSeverity.LOGGING, convertMessage(message, symbol))
    }

    override fun info(message: String, symbol: KSNode?) {
        messageCollector.report(CompilerMessageSeverity.INFO, convertMessage(message, symbol))
    }

    override fun warn(message: String, symbol: KSNode?) {
        messageCollector.report(CompilerMessageSeverity.WARNING, convertMessage(message, symbol))
    }

    override fun error(message: String, symbol: KSNode?) {
        messageCollector.report(CompilerMessageSeverity.ERROR, convertMessage(message, symbol))
    }

    override fun exception(e: Throwable) {
        val writer = StringWriter()
        e.printStackTrace(PrintWriter(writer))
        messageCollector.report(CompilerMessageSeverity.EXCEPTION, writer.toString())
    }
}