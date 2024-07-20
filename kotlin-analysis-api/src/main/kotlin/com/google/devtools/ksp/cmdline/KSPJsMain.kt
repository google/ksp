package com.google.devtools.ksp.cmdline

import com.google.devtools.ksp.processing.kspJsArgParser
import com.google.devtools.ksp.processing.kspJsArgParserHelp

class KSPJsMain {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if ("-h" in args || "--help" in args) {
                printHelpMsg(kspJsArgParserHelp())
            } else {
                runWithArgs(args, ::kspJsArgParser)
            }
        }
    }
}
