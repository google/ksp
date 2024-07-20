/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package com.google.devtools.ksp.symbol.impl.synthetic

import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.kotlin.KSErrorType

class KSErrorTypeClassDeclaration(
    private val type: KSErrorType,
) : KSClassDeclaration {
    override val annotations: Sequence<KSAnnotation>
        get() = emptySequence()

    override val classKind: ClassKind
        get() = ClassKind.CLASS

    override val containingFile: KSFile?
        get() = null

    override val declarations: Sequence<KSDeclaration>
        get() = emptySequence()

    override val isActual: Boolean
        get() = false

    override val isExpect: Boolean
        get() = false

    override val isCompanionObject: Boolean
        get() = false

    override val location: Location
        get() = NonExistLocation

    override val parent: KSNode?
        get() = null

    override val modifiers: Set<Modifier>
        get() = emptySet()

    override val origin: Origin
        get() = Origin.SYNTHETIC

    override val packageName: KSName
        get() = KSNameImpl.getCached("")

    override val parentDeclaration: KSDeclaration?
        get() = null

    override val primaryConstructor: KSFunctionDeclaration?
        get() = null

    override val qualifiedName: KSName?
        get() = null

    override val simpleName: KSName = KSNameImpl.getCached(type.toString())

    override val superTypes: Sequence<KSTypeReference>
        get() = emptySequence()

    override val typeParameters: List<KSTypeParameter>
        get() = emptyList()

    override fun getSealedSubclasses(): Sequence<KSClassDeclaration> = emptySequence()

    override fun asStarProjectedType(): KSType {
        return type
    }

    override fun asType(typeArguments: List<KSTypeArgument>): KSType {
        return type
    }

    override fun findActuals(): Sequence<KSDeclaration> {
        return emptySequence()
    }

    override fun findExpects(): Sequence<KSDeclaration> {
        return emptySequence()
    }

    override fun getAllFunctions(): Sequence<KSFunctionDeclaration> {
        return emptySequence()
    }

    override fun getAllProperties(): Sequence<KSPropertyDeclaration> {
        return emptySequence()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitClassDeclaration(this, data)
    }

    override fun toString(): String {
        return simpleName.asString()
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other is KSErrorTypeClassDeclaration && other.type == type
    }

    override fun hashCode(): Int = type.hashCode()

    override val docString
        get() = null
}
