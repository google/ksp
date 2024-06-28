package com.google.devtools.ksp.common

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File

private object FileSerializer : KSerializer<File> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("File", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: File) = encoder.encodeString(value.path)
    override fun deserialize(decoder: Decoder): File = File(decoder.decodeString())
}

private object SymbolSerializer : KSerializer<LookupSymbolWrapper> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LookupSymbolWrapper", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LookupSymbolWrapper) =
        encoder.encodeString("${value.name}:${value.scope}")
    override fun deserialize(decoder: Decoder): LookupSymbolWrapper {
        val (name, scope) = decoder.decodeString().split(':')
        return LookupSymbolWrapper(name, scope)
    }
}

private val fileToFilesMapSerializer = MapSerializer(FileSerializer, ListSerializer(FileSerializer))
private val fileToSymbolsMapSerializer = MapSerializer(FileSerializer, ListSerializer(SymbolSerializer))

abstract class PersistentMap<K, V>(
    private val serializer: KSerializer<Map<K, V>>,
    private val storage: File,
    private val m: MutableMap<K, V>,
) : MutableMap<K, V> by m {

    @OptIn(ExperimentalSerializationApi::class)
    fun flush() {
        storage.outputStream().use {
            Json.encodeToStream(serializer, m.toMap(), it)
        }
    }
    override fun toString() = m.toString()

    companion object {
        @JvmStatic
        @OptIn(ExperimentalSerializationApi::class)
        protected fun <K, V> deserialize(serializer: KSerializer<Map<K, V>>, storage: File): MutableMap<K, V> {
            return if (storage.exists()) {
                Json.decodeFromStream(serializer, storage.inputStream()).toMutableMap()
            } else {
                mutableMapOf()
            }
        }
    }
}

class FileToFilesMap(
    storage: File
) : PersistentMap<File, List<File>>(fileToFilesMapSerializer, storage, deserialize(fileToFilesMapSerializer, storage))

class FileToSymbolsMap(
    storage: File
) : PersistentMap<File, List<LookupSymbolWrapper>>(
    fileToSymbolsMapSerializer,
    storage,
    deserialize(fileToSymbolsMapSerializer, storage)
)
