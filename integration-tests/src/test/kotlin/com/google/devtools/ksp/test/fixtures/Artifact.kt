package com.google.devtools.ksp.test.fixtures

import java.io.File
import java.util.zip.ZipFile
import org.junit.Assert

// A snapshot of the digest of output jar.
class Artifact(file: File) {
    private fun getCRCs(file: File): Map<String, Long> {
        Assert.assertTrue(file.exists())
        return ZipFile(file).use {
            it.entries()
                .asSequence()
                .map {
                    it.name to it.crc
                }
                .toMap()
        }
    }

    val crcs: Map<String, Long> = getCRCs(file)

    override fun equals(other: Any?): Boolean {
        if (other !is Artifact) return false

        return crcs == other.crcs
    }
}
