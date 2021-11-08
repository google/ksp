package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.symbol.KSName

class KSNameImpl(val name: String) : KSName {
    override fun asString(): String {
        return name
    }

    override fun getQualifier(): String {
        return name.split(".").dropLast(1).joinToString(".")
    }

    override fun getShortName(): String {
        return name.split(".").last()
    }
}
