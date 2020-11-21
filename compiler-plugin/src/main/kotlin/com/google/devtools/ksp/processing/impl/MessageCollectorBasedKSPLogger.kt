/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    override var errorCount = 0
        private set

    override var warningCount = 0
        private set

    override var infoCount = 0
        private set

    override var loggingCount = 0
        private set

    override var exceptionCount = 0
        private set

    private fun convertMessage(message: String, symbol: KSNode?): String =
        when (val location = symbol?.location) {
            is FileLocation -> "$PREFIX${location.filePath}:${location.lineNumber}: $message"
            is NonExistLocation, null -> "$PREFIX$message"
        }

    override fun logging(message: String, symbol: KSNode?) {
        loggingCount++
        messageCollector.report(CompilerMessageSeverity.LOGGING, convertMessage(message, symbol))
    }

    override fun info(message: String, symbol: KSNode?) {
        infoCount++
        messageCollector.report(CompilerMessageSeverity.INFO, convertMessage(message, symbol))
    }

    override fun warn(message: String, symbol: KSNode?) {
        warningCount++
        messageCollector.report(CompilerMessageSeverity.WARNING, convertMessage(message, symbol))
    }

    override fun error(message: String, symbol: KSNode?) {
        errorCount++
        messageCollector.report(CompilerMessageSeverity.ERROR, convertMessage(message, symbol))
    }

    override fun exception(e: Throwable) {
        exceptionCount++
        val writer = StringWriter()
        e.printStackTrace(PrintWriter(writer))
        messageCollector.report(CompilerMessageSeverity.EXCEPTION, writer.toString())
    }
}