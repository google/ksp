/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol

/**
 * A visitor for program elements
 */
interface KSVisitor<D, R> {
    fun visitNode(node: KSNode, data: D): R

    fun visitAnnotated(annotated: KSAnnotated, data: D): R

    fun visitAnnotation(annotation: KSAnnotation, data: D): R

    fun visitModifierListOwner(modifierListOwner: KSModifierListOwner, data: D): R

    fun visitDeclaration(declaration: KSDeclaration, data: D): R

    fun visitDeclarationContainer(declarationContainer: KSDeclarationContainer, data: D): R

    fun visitDynamicReference(reference: KSDynamicReference, data: D): R

    fun visitFile(file: KSFile, data: D): R

    fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: D): R

    fun visitCallableReference(reference: KSCallableReference, data: D): R

    fun visitParenthesizedReference(reference: KSParenthesizedReference, data: D): R

    fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: D): R

    fun visitPropertyAccessor(accessor: KSPropertyAccessor, data: D): R

    fun visitPropertyGetter(getter: KSPropertyGetter, data: D): R

    fun visitPropertySetter(setter: KSPropertySetter, data: D): R

    fun visitReferenceElement(element: KSReferenceElement, data: D): R

    fun visitTypeAlias(typeAlias: KSTypeAlias, data: D): R

    fun visitTypeArgument(typeArgument: KSTypeArgument, data: D): R

    fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: D): R

    fun visitTypeParameter(typeParameter: KSTypeParameter, data: D): R

    fun visitTypeReference(typeReference: KSTypeReference, data: D): R

    fun visitVariableParameter(variableParameter: KSVariableParameter, data: D): R

    fun visitValueArgument(valueArgument: KSValueArgument, data: D): R

    fun visitClassifierReference(reference: KSClassifierReference, data: D): R
}