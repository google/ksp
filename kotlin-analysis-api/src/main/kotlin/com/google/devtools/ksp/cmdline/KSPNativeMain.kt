package com.google.devtools.ksp.cmdline

import com.google.devtools.ksp.processing.kspNativeArgParser
import com.google.devtools.ksp.processing.kspNativeArgParserHelp

class KSPNativeMain {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if ("-h" in args || "--help" in args) {
                printHelpMsg(kspNativeArgParserHelp())
            } else {
                runWithArgs(args, ::kspNativeArgParser)
            }
        }
    }
}
