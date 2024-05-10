package com.google.devtools.ksp.cmdline

import com.google.devtools.ksp.processing.kspCommonArgParser
import com.google.devtools.ksp.processing.kspCommonArgParserHelp

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
