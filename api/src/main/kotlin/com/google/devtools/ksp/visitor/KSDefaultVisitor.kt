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
package com.google.devtools.ksp.visitor

import com.google.devtools.ksp.symbol.*

/**
 * A visitor that delegates to super types for methods that are not overridden.
 */
abstract class KSDefaultVisitor<D, R> : KSEmptyVisitor<D, R>() {
    override fun visitDynamicReference(reference: KSDynamicReference, data: D): R {
        this.visitReferenceElement(reference, data)
        return super.visitDynamicReference(reference, data)
    }

    override fun visitFile(file: KSFile, data: D): R {
        this.visitAnnotated(file, data)
        this.visitDeclarationContainer(file, data)
        return super.visitFile(file, data)
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: D): R {
        this.visitDeclaration(function, data)
        this.visitDeclarationContainer(function, data)
        return super.visitFunctionDeclaration(function, data)
    }

    override fun visitCallableReference(reference: KSCallableReference, data: D): R {
        this.visitReferenceElement(reference, data)
        return super.visitCallableReference(reference, data)
    }

    override fun visitParenthesizedReference(reference: KSParenthesizedReference, data: D): R {
        this.visitReferenceElement(reference, data)
        return super.visitParenthesizedReference(reference, data)
    }

    override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: D): R {
        this.visitDeclaration(property, data)
        return super.visitPropertyDeclaration(property, data)
    }

    override fun visitPropertyAccessor(accessor: KSPropertyAccessor, data: D): R {
        this.visitModifierListOwner(accessor, data)
        this.visitAnnotated(accessor, data)
        return super.visitPropertyAccessor(accessor, data)
    }

    override fun visitPropertyGetter(getter: KSPropertyGetter, data: D): R {
        this.visitPropertyAccessor(getter, data)
        return super.visitPropertyGetter(getter, data)
    }

    override fun visitPropertySetter(setter: KSPropertySetter, data: D): R {
        this.visitPropertyAccessor(setter, data)
        return super.visitPropertySetter(setter, data)
    }

    override fun visitTypeAlias(typeAlias: KSTypeAlias, data: D): R {
        this.visitDeclaration(typeAlias, data)
        return super.visitTypeAlias(typeAlias, data)
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: D): R {
        this.visitDeclaration(classDeclaration, data)
        this.visitDeclarationContainer(classDeclaration, data)
        return super.visitClassDeclaration(classDeclaration, data)
    }

    override fun visitTypeParameter(typeParameter: KSTypeParameter, data: D): R {
        this.visitDeclaration(typeParameter, data)
        return super.visitTypeParameter(typeParameter, data)
    }

    override fun visitTypeReference(typeReference: KSTypeReference, data: D): R {
        this.visitAnnotated(typeReference, data)
        this.visitModifierListOwner(typeReference, data)
        return super.visitTypeReference(typeReference, data)
    }

    override fun visitValueParameter(valueParameter: KSValueParameter, data: D): R {
        this.visitAnnotated(valueParameter, data)
        return super.visitValueParameter(valueParameter, data)
    }

    override fun visitValueArgument(valueArgument: KSValueArgument, data: D): R {
        this.visitAnnotated(valueArgument, data)
        return super.visitValueArgument(valueArgument, data)
    }

    override fun visitClassifierReference(reference: KSClassifierReference, data: D): R {
        this.visitReferenceElement(reference, data)
        return super.visitClassifierReference(reference, data)
    }

    override fun visitDefNonNullReference(reference: KSDefNonNullReference, data: D): R {
        this.visitReferenceElement(reference, data)
        return super.visitDefNonNullReference(reference, data)
    }

    override fun visitTypeArgument(typeArgument: KSTypeArgument, data: D): R {
        this.visitAnnotated(typeArgument, data)
        return super.visitTypeArgument(typeArgument, data)
    }

    override fun visitDeclaration(declaration: KSDeclaration, data: D): R {
        this.visitAnnotated(declaration, data)
        this.visitModifierListOwner(declaration, data)
        return super.visitDeclaration(declaration, data)
    }

    override fun visitAnnotated(annotated: KSAnnotated, data: D): R {
        this.visitNode(annotated, data)
        return super.visitAnnotated(annotated, data)
    }

    override fun visitAnnotation(annotation: KSAnnotation, data: D): R {
        this.visitNode(annotation, data)
        return super.visitAnnotation(annotation, data)
    }

    override fun visitDeclarationContainer(declarationContainer: KSDeclarationContainer, data: D): R {
        this.visitNode(declarationContainer, data)
        return super.visitDeclarationContainer(declarationContainer, data)
    }

    override fun visitModifierListOwner(modifierListOwner: KSModifierListOwner, data: D): R {
        this.visitNode(modifierListOwner, data)
        return super.visitModifierListOwner(modifierListOwner, data)
    }

    override fun visitReferenceElement(element: KSReferenceElement, data: D): R {
        this.visitNode(element, data)
        return super.visitReferenceElement(element, data)
    }
}
