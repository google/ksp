/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package com.google.devtools.ksp.impl.symbol.util

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSType
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClass
import org.jetbrains.kotlin.load.kotlin.getContainingKotlinJvmBinaryClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.isCompanionObject
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

data class BinaryClassInfo(
    val fieldAccFlags: Map<String, Int>,
    val methodAccFlags: Map<String, Int>
)

/**
 * Lookup cache for field names names for deserialized classes.
 * To check if a field has backing field, we need to look for binary field names, hence they are cached here.
 */
object BinaryClassInfoCache : KSObjectCache<ClassId, BinaryClassInfo>() {
    fun getCached(
        kotlinJvmBinaryClass: KotlinJvmBinaryClass,
    ) = getCached(
        kotlinJvmBinaryClass.classId, (kotlinJvmBinaryClass as? VirtualFileKotlinClass)?.file?.contentsToByteArray()
    )

    fun getCached(classId: ClassId, virtualFileContent: ByteArray?) = cache.getOrPut(classId) {
        val fieldAccFlags = mutableMapOf<String, Int>()
        val methodAccFlags = mutableMapOf<String, Int>()
        ClassReader(virtualFileContent).accept(
            object : ClassVisitor(Opcodes.API_VERSION) {
                override fun visitField(
                    access: Int,
                    name: String?,
                    descriptor: String?,
                    signature: String?,
                    value: Any?
                ): FieldVisitor? {
                    if (name != null) {
                        fieldAccFlags.put(name, access)
                    }
                    return null
                }

                override fun visitMethod(
                    access: Int,
                    name: String?,
                    descriptor: String?,
                    signature: String?,
                    exceptions: Array<out String>?
                ): MethodVisitor? {
                    if (name != null) {
                        methodAccFlags.put(name + descriptor, access)
                    }
                    return null
                }
            },
            ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
        )
        BinaryClassInfo(fieldAccFlags, methodAccFlags)
    }
}

/**
 * Workaround for backingField in deserialized descriptors.
 * They always return non-null for backing field even when they don't have a backing field.
 */
private fun DeserializedPropertyDescriptor.hasBackingFieldInBinaryClass(): Boolean {
    val kotlinJvmBinaryClass = if (containingDeclaration.isCompanionObject()) {
        // Companion objects have backing fields in containing classes.
        // https://kotlinlang.org/docs/java-to-kotlin-interop.html#static-fields
        val container = containingDeclaration.containingDeclaration as? DeserializedClassDescriptor
        (container?.source as? KotlinJvmBinarySourceElement)?.binaryClass
    } else {
        this.getContainingKotlinJvmBinaryClass()
    } ?: return false
    return BinaryClassInfoCache.getCached(kotlinJvmBinaryClass).fieldAccFlags.containsKey(name.asString())
}

fun KSAnnotated.hasAnnotation(fqn: String): Boolean =
    annotations.any {
        fqn.endsWith(it.shortName.asString()) &&
            it.annotationType.resolve().declaration.qualifiedName?.asString() == fqn
    }

fun Resolver.extractThrowsFromClassFile(
    virtualFileContent: ByteArray,
    jvmDesc: String?,
    simpleName: String?
): Sequence<KSType> {
    val exceptionNames = mutableListOf<String>()
    ClassReader(virtualFileContent).accept(
        object : ClassVisitor(Opcodes.API_VERSION) {
            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?,
            ): MethodVisitor {
                if (name == simpleName && jvmDesc == descriptor) {
                    exceptions?.toList()?.let { exceptionNames.addAll(it) }
                }
                return object : MethodVisitor(Opcodes.API_VERSION) {
                }
            }
        },
        ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
    )
    return exceptionNames.mapNotNull {
        this.getClassDeclarationByName(it.replace("/", "."))?.asStarProjectedType()
    }.asSequence()
}
