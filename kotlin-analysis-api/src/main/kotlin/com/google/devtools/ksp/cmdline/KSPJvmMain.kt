package com.google.devtools.ksp.cmdline

import com.google.devtools.ksp.processing.kspJvmArgParser
import com.google.devtools.ksp.processing.kspJvmArgParserHelp

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
