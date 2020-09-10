/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.synthetic

import org.jetbrains.kotlin.ksp.processing.impl.ResolverImpl
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSNameImpl

object KSErrorTypeClassDeclaration : KSClassDeclaration {
    override val annotations: List<KSAnnotation> = emptyList()

    override val classKind: ClassKind = ClassKind.CLASS

    override val containingFile: KSFile? = null

    override val declarations: List<KSDeclaration> = emptyList()

    override val isActual: Boolean = false

    override val isExpect: Boolean = false

    override val isCompanionObject: Boolean = false

    override val location: Location = NonExistLocation

    override val modifiers: Set<Modifier> = emptySet()

    override val origin: Origin = Origin.SYNTHETIC

    override val packageName: KSName = KSNameImpl.getCached("")

    override val parentDeclaration: KSDeclaration? = null

    override val primaryConstructor: KSFunctionDeclaration? = null

    override val qualifiedName: KSName? = null

    override val simpleName: KSName = KSNameImpl.getCached("<Error>")

    override val superTypes: List<KSTypeReference> = emptyList()

    override val typeParameters: List<KSTypeParameter> = emptyList()

    override fun asStarProjectedType(): KSType {
        return ResolverImpl.instance.builtIns.nothingType
    }

    override fun asType(typeArguments: List<KSTypeArgument>): KSType {
        return ResolverImpl.instance.builtIns.nothingType
    }

    override fun findActuals(): List<KSDeclaration> {
        return emptyList()
    }

    override fun findExpects(): List<KSDeclaration> {
        return emptyList()
    }

    override fun getAllFunctions(): List<KSFunctionDeclaration> {
        return emptyList()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitClassDeclaration(this, data)
    }

    override fun toString(): String {
        return "Error type synthetic declaration"
    }
}