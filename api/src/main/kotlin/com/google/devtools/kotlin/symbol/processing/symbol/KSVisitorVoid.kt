/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol

/**
 * A visitor that doesn't pass or return anything.
 */
open class KSVisitorVoid : KSVisitor<Unit, Unit> {
    override fun visitNode(node: KSNode, data: Unit) {
    }

    override fun visitAnnotated(annotated: KSAnnotated, data: Unit) {
    }

    override fun visitAnnotation(annotation: KSAnnotation, data: Unit) {
    }

    override fun visitModifierListOwner(modifierListOwner: KSModifierListOwner, data: Unit) {
    }

    override fun visitDeclaration(declaration: KSDeclaration, data: Unit) {
    }

    override fun visitDeclarationContainer(declarationContainer: KSDeclarationContainer, data: Unit) {
    }

    override fun visitDynamicReference(reference: KSDynamicReference, data: Unit) {
    }

    override fun visitFile(file: KSFile, data: Unit) {
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
    }

    override fun visitCallableReference(reference: KSCallableReference, data: Unit) {
    }

    override fun visitParenthesizedReference(reference: KSParenthesizedReference, data: Unit) {
    }

    override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
    }

    override fun visitPropertyAccessor(accessor: KSPropertyAccessor, data: Unit) {
    }

    override fun visitPropertyGetter(getter: KSPropertyGetter, data: Unit) {
    }

    override fun visitPropertySetter(setter: KSPropertySetter, data: Unit) {
    }

    override fun visitClassifierReference(reference: KSClassifierReference, data: Unit) {
    }

    override fun visitReferenceElement(element: KSReferenceElement, data: Unit) {
    }

    override fun visitTypeAlias(typeAlias: KSTypeAlias, data: Unit) {
    }

    override fun visitTypeArgument(typeArgument: KSTypeArgument, data: Unit) {
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    }

    override fun visitTypeParameter(typeParameter: KSTypeParameter, data: Unit) {
    }

    override fun visitTypeReference(typeReference: KSTypeReference, data: Unit) {
    }

    override fun visitVariableParameter(variableParameter: KSVariableParameter, data: Unit) {
    }

    override fun visitValueArgument(valueArgument: KSValueArgument, data: Unit) {
    }
}