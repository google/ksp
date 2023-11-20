package com.google.devtools.ksp

import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.PersistentHashMap
import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutput
import java.io.File

abstract class PersistentMap<K, V>(
    storageFile: File,
    keyDescriptor: KeyDescriptor<K>,
    dataExternalizer: DataExternalizer<V>
) : PersistentHashMap<K, V>(
    storageFile,
    keyDescriptor,
    dataExternalizer
) {
    val keys: Collection<K>
        get() = mutableListOf<K>().also { list ->
            this.processKeysWithExistingMapping { key -> list.add(key) }
        }

    operator fun set(key: K, value: V) = put(key, value)

    fun clear() {
        keys.forEach {
            remove(it)
        }
    }

    fun flush() = force()
}

object FileKeyDescriptor : KeyDescriptor<File> {
    override fun read(input: DataInput): File {
        return File(IOUtil.readString(input))
    }

    override fun save(output: DataOutput, value: File) {
        IOUtil.writeString(value.path, output)
    }

    override fun getHashCode(value: File): Int = value.hashCode()

    override fun isEqual(val1: File, val2: File): Boolean = val1 == val2
}

object FileExternalizer : DataExternalizer<File> {
    override fun read(input: DataInput): File = File(IOUtil.readString(input))

    override fun save(output: DataOutput, value: File) {
        IOUtil.writeString(value.path, output)
    }
}

class ListExternalizer<T>(
    private val elementExternalizer: DataExternalizer<T>,
) : DataExternalizer<List<T>> {

    override fun save(output: DataOutput, value: List<T>) {
        value.forEach { elementExternalizer.save(output, it) }
    }

    override fun read(input: DataInput): List<T> {
        val result = mutableListOf<T>()
        val stream = input as DataInputStream

        while (stream.available() > 0) {
            result.add(elementExternalizer.read(stream))
        }

        return result
    }
}

class FileToFilesMap(
    storageFile: File,
) : PersistentMap<File, List<File>>(
    storageFile,
    FileKeyDescriptor,
    ListExternalizer(FileExternalizer)
)

class FileToSymbolsMap(
    storageFile: File,
    lookupSymbolExternalizer: DataExternalizer<LookupSymbolWrapper>
) : PersistentMap<File, List<LookupSymbolWrapper>>(
    storageFile,
    FileKeyDescriptor,
    ListExternalizer(lookupSymbolExternalizer),
)
