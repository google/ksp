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

sealed interface KSPrimitiveType

@JvmInline
value class KSBool(val v: Boolean) : KSPrimitiveType

@JvmInline
value class KSByte(val v: Byte) : KSPrimitiveType

@JvmInline
value class KSShort(val v: Short) : KSPrimitiveType

@JvmInline
value class KSInt(val v: Int) : KSPrimitiveType

@JvmInline
value class KSLong(val v: Long) : KSPrimitiveType

@JvmInline
value class KSFloat(val v: Float) : KSPrimitiveType

@JvmInline
value class KSDouble(val v: Double) : KSPrimitiveType

@JvmInline
value class KSChar(val v: Char) : KSPrimitiveType

@JvmInline
value class KSString(val v: String) : KSPrimitiveType

@JvmInline
value class KSUByte(val v: UByte) : KSPrimitiveType

@JvmInline
value class KSUShort(val v: UShort) : KSPrimitiveType

@JvmInline
value class KSUInt(val v: UInt) : KSPrimitiveType

@JvmInline
value class KSULong(val v: ULong) : KSPrimitiveType
