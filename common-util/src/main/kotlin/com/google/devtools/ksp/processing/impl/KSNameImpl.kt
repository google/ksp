package com.google.devtools.ksp.common.impl

import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.symbol.KSName

class KSNameImpl private constructor(val name: String) : KSName {
    companion object : KSObjectCache<String, KSNameImpl>() {
        fun getCached(name: String) = cache.getOrPut(name) { KSNameImpl(name) }
    }

    override fun asString(): String {
        return name
    }

    override fun getQualifier(): String {
        val lastIndex = name.lastIndexOf('.')
        return if (lastIndex != -1) name.substring(0, lastIndex) else ""
    }

    override fun getShortName(): String {
        val lastIndex = name.lastIndexOf('.')
        return if (lastIndex != -1) name.substring(lastIndex + 1) else name
    }
}
