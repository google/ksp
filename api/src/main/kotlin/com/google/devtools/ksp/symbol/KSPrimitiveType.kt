/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
package com.google.devtools.ksp.symbol

/** A primitive or string annotation value, preserving its underlying Kotlin type. */
sealed interface KSPrimitiveType

@JvmInline
value class KSBool(val value: Boolean) : KSPrimitiveType

@JvmInline
value class KSByte(val value: Byte) : KSPrimitiveType

@JvmInline
value class KSShort(val value: Short) : KSPrimitiveType

@JvmInline
value class KSInt(val value: Int) : KSPrimitiveType

@JvmInline
value class KSLong(val value: Long) : KSPrimitiveType

@JvmInline
value class KSFloat(val value: Float) : KSPrimitiveType

@JvmInline
value class KSDouble(val value: Double) : KSPrimitiveType

@JvmInline
value class KSChar(val value: Char) : KSPrimitiveType

@JvmInline
value class KSString(val value: String) : KSPrimitiveType

@JvmInline
value class KSUByte(val value: UByte) : KSPrimitiveType

@JvmInline
value class KSUShort(val value: UShort) : KSPrimitiveType

@JvmInline
value class KSUInt(val value: UInt) : KSPrimitiveType

@JvmInline
value class KSULong(val value: ULong) : KSPrimitiveType
