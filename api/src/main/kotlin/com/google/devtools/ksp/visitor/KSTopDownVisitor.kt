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
 * Visit all elements recursively.
 *
 * For subclasses overriding a function, remember to call the corresponding super method.
 */
abstract class KSTopDownVisitor<D, R> : KSDefaultVisitor<D, R>() {
    private fun Collection<KSNode>.accept(data: D) = forEach { it.accept(this@KSTopDownVisitor, data) }

    private fun KSNode.accept(data: D) = accept(this@KSTopDownVisitor, data)

    override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: D): R {
        property.type.accept(data)
        property.extensionReceiver?.accept(data)
        return super.visitPropertyDeclaration(property, data).also {
            property.initializer?.accept(data)
            property.delegate?.accept(data)
            property.getter?.accept(data)
            property.setter?.accept(data)
        }
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
        return super.visitFunctionDeclaration(function, data).also {
            function.body?.accept(data)
        }
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

    override fun visitValueParameter(valueParameter: KSValueParameter, data: D): R {
        valueParameter.type?.accept(data)
        return super.visitValueParameter(valueParameter, data)
    }

    override fun visitAnonymousInitializer(initializer: KSAnonymousInitializer, data: D): R {
        return super.visitAnonymousInitializer(initializer, data).also {
            initializer.statements.accept(data)
        }
    }

    override fun visitPropertyAccessor(accessor: KSPropertyAccessor, data: D): R {
        return super.visitPropertyAccessor(accessor, data).also {
            accessor.body?.accept(data)
        }
    }

    override fun visitChainCallsExpression(expression: KSChainCallsExpression, data: D): R {
        return super.visitChainCallsExpression(expression, data).also {
            expression.chains.accept(data)
        }
    }

    override fun visitWhenExpression(expression: KSWhenExpression, data: D): R {
        return super.visitWhenExpression(expression, data).also {
            expression.subject?.accept(data)
            expression.branches.accept(data)
        }
    }

    override fun visitDslExpression(expression: KSDslExpression, data: D): R {
        return super.visitDslExpression(expression, data).also {
            expression.closures.accept(data)
        }
    }

    override fun visitBlockExpression(expression: KSBlockExpression, data: D): R {
        return super.visitBlockExpression(expression, data).also {
            expression.statements.accept(data)
        }
    }

    override fun visitLabeledExpression(expression: KSLabeledExpression, data: D): R {
        return super.visitLabeledExpression(expression, data).also {
            expression.body.accept(data)
        }
    }

    override fun visitIfExpression(expression: KSIfExpression, data: D): R {
        return super.visitIfExpression(expression, data).also {
            expression.then.accept(data)
            expression.otherwise?.accept(data)
        }
    }

    override fun visitWhenExpressionBranch(whenBranch: KSWhenExpression.Branch, data: D): R {
        return super.visitWhenExpressionBranch(whenBranch, data).also {
            whenBranch.body.accept(data)
        }
    }
}
