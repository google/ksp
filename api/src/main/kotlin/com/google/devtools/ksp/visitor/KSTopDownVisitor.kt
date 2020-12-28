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
        return super.visitPropertyDeclaration(property, data).also {
            property.type.accept(data)
            property.extensionReceiver?.accept(data)
            property.initializer?.accept(data)
            property.delegate?.accept(data)
            property.getter?.accept(data)
            property.setter?.accept(data)
        }
    }

    override fun visitAnnotated(annotated: KSAnnotated, data: D): R {
        return super.visitAnnotated(annotated, data).also {
            annotated.annotations.accept(data)
        }
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: D): R {
        return super.visitClassDeclaration(classDeclaration, data).also {
            classDeclaration.superTypes.accept(data)
        }
    }

    override fun visitDeclaration(declaration: KSDeclaration, data: D): R {
        return super.visitDeclaration(declaration, data).also {
            declaration.typeParameters.accept(data)
        }
    }

    override fun visitDeclarationContainer(declarationContainer: KSDeclarationContainer, data: D): R {
        return super.visitDeclarationContainer(declarationContainer, data).also {
            declarationContainer.declarations.accept(data)
        }
    }

    override fun visitAnnotation(annotation: KSAnnotation, data: D): R {
        return super.visitAnnotation(annotation, data).also {
            annotation.annotationType.accept(data)
            annotation.arguments.accept(data)
        }
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: D): R {
        return super.visitFunctionDeclaration(function, data).also {
            function.extensionReceiver?.accept(data)
            function.parameters.accept(data)
            function.returnType?.accept(data)
            function.body?.accept(data)
        }
    }

    override fun visitCallableReference(reference: KSCallableReference, data: D): R {
        return super.visitCallableReference(reference, data).also {
            reference.functionParameters.accept(data)
            reference.receiverType?.accept(data)
            reference.returnType.accept(data)
        }
    }

    override fun visitParenthesizedReference(reference: KSParenthesizedReference, data: D): R {
        return super.visitParenthesizedReference(reference, data).also {
            reference.element.accept(data)
        }
    }

    override fun visitPropertyGetter(getter: KSPropertyGetter, data: D): R {
        return super.visitPropertyGetter(getter, data).also {
            getter.returnType?.accept(data)
        }
    }

    override fun visitPropertySetter(setter: KSPropertySetter, data: D): R {
        return super.visitPropertySetter(setter, data).also {
            setter.parameter.accept(data)
        }
    }

    override fun visitReferenceElement(element: KSReferenceElement, data: D): R {
        return super.visitReferenceElement(element, data).also {
            element.typeArguments.accept(data)
        }
    }

    override fun visitTypeAlias(typeAlias: KSTypeAlias, data: D): R {
        return super.visitTypeAlias(typeAlias, data).also {
            typeAlias.type.accept(data)
        }
    }

    override fun visitTypeArgument(typeArgument: KSTypeArgument, data: D): R {
        return super.visitTypeArgument(typeArgument, data).also {
            typeArgument.type?.accept(data)
        }
    }

    override fun visitTypeParameter(typeParameter: KSTypeParameter, data: D): R {
        return super.visitTypeParameter(typeParameter, data).also {
            typeParameter.bounds.accept(data)
        }
    }

    override fun visitTypeReference(typeReference: KSTypeReference, data: D): R {
        return super.visitTypeReference(typeReference, data).also {
            typeReference.element?.accept(data)
        }
    }

    override fun visitClassifierReference(reference: KSClassifierReference, data: D): R {
        return super.visitClassifierReference(reference, data).also {
            reference.qualifier?.accept(data)
        }
    }

    override fun visitValueParameter(valueParameter: KSValueParameter, data: D): R {
        return super.visitValueParameter(valueParameter, data).also {
            valueParameter.type?.accept(data)
        }
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
