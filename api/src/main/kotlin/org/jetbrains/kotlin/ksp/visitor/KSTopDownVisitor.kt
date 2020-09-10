/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.visitor

import org.jetbrains.kotlin.ksp.symbol.*

/**
 * Visit all elements recursively.
 *
 * For subclasses overriding a function, remember to call the corresponding super method.
 */
abstract class KSTopDownVisitor<D, R> : KSDefaultVisitor<D, R>() {
    private fun Collection<KSNode>.accept(data: D) {
        forEach { it.accept(this@KSTopDownVisitor, data) }
    }

    private fun KSNode.accept(data: D) = accept(this@KSTopDownVisitor, data)

    override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: D): R {
        property.type?.accept(data)
        property.extensionReceiver?.accept(data)
        property.getter?.accept(data)
        property.setter?.accept(data)
        return super.visitPropertyDeclaration(property, data)
    }

    override fun visitAnnotated(annotated: KSAnnotated, data: D): R {
        annotated.annotations.accept(data)
        return super.visitAnnotated(annotated, data)
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: D): R {
        classDeclaration.superTypes.accept(data)
        return super.visitClassDeclaration(classDeclaration, data)
    }

    override fun visitDeclaration(declaration: KSDeclaration, data: D): R {
        declaration.typeParameters.accept(data)
        return super.visitDeclaration(declaration, data)
    }

    override fun visitDeclarationContainer(declarationContainer: KSDeclarationContainer, data: D): R {
        declarationContainer.declarations.accept(data)
        return super.visitDeclarationContainer(declarationContainer, data)
    }

    override fun visitAnnotation(annotation: KSAnnotation, data: D): R {
        annotation.annotationType.accept(data)
        annotation.arguments.accept(data)
        return super.visitAnnotation(annotation, data)
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: D): R {
        function.extensionReceiver?.accept(data)
        function.parameters.accept(data)
        function.returnType?.accept(data)
        return super.visitFunctionDeclaration(function, data)
    }

    override fun visitCallableReference(reference: KSCallableReference, data: D): R {
        reference.functionParameters.accept(data)
        reference.receiverType?.accept(data)
        reference.returnType.accept(data)
        return super.visitCallableReference(reference, data)
    }

    override fun visitParenthesizedReference(reference: KSParenthesizedReference, data: D): R {
        reference.element.accept(data)
        return super.visitParenthesizedReference(reference, data)
    }

    override fun visitPropertyGetter(getter: KSPropertyGetter, data: D): R {
        getter.returnType?.accept(data)
        return super.visitPropertyGetter(getter, data)
    }

    override fun visitPropertySetter(setter: KSPropertySetter, data: D): R {
        setter.parameter.accept(data)
        return super.visitPropertySetter(setter, data)
    }

    override fun visitReferenceElement(element: KSReferenceElement, data: D): R {
        element.typeArguments.accept(data)
        return super.visitReferenceElement(element, data)
    }

    override fun visitTypeAlias(typeAlias: KSTypeAlias, data: D): R {
        typeAlias.type.accept(data)
        return super.visitTypeAlias(typeAlias, data)
    }

    override fun visitTypeArgument(typeArgument: KSTypeArgument, data: D): R {
        typeArgument.type?.accept(data)
        return super.visitTypeArgument(typeArgument, data)
    }

    override fun visitTypeParameter(typeParameter: KSTypeParameter, data: D): R {
        typeParameter.bounds.accept(data)
        return super.visitTypeParameter(typeParameter, data)
    }

    override fun visitTypeReference(typeReference: KSTypeReference, data: D): R {
        typeReference.element?.accept(data)
        return super.visitTypeReference(typeReference, data)
    }

    override fun visitClassifierReference(reference: KSClassifierReference, data: D): R {
        reference.qualifier?.accept(data)
        return super.visitClassifierReference(reference, data)
    }

    override fun visitVariableParameter(variableParameter: KSVariableParameter, data: D): R {
        variableParameter.type?.accept(data)
        return super.visitVariableParameter(variableParameter, data)
    }
}
