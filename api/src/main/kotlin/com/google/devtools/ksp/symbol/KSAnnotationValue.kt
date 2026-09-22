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

/** A typed representation of an annotation argument's value. */
sealed interface KSAnnotationValue

/** A primitive or string value wrapped in its corresponding [KSPrimitiveType]. */
@JvmInline
value class Primitive(val type: KSPrimitiveType) : KSAnnotationValue

/** A class reference represented by its [KSType]. */
@JvmInline
value class ReflectionClassReference(val type: KSType) : KSAnnotationValue

/** An enum entry represented by its [KSClassDeclaration]. */
@JvmInline
value class EnumClass(val clazz: KSClassDeclaration) : KSAnnotationValue

/** A nested annotation represented by its [KSAnnotation]. */
@JvmInline
value class AnnotationClass(val anno: KSAnnotation) : KSAnnotationValue

/** An array or collection whose elements are recursively represented as [KSAnnotationValue]s. */
@JvmInline
value class ArrayValue(val values: Array<KSAnnotationValue>) : KSAnnotationValue

/** A value that could not be converted, with a diagnostic [message] and the original [v]. */
class ErrorValue(val message: String, val v: Any?) : KSAnnotationValue
