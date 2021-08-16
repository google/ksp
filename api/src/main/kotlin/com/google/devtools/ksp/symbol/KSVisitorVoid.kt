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
package com.google.devtools.ksp.symbol

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

    override fun visitValueParameter(valueParameter: KSValueParameter, data: Unit) {
    }

    override fun visitValueArgument(valueArgument: KSValueArgument, data: Unit) {
    }
}
