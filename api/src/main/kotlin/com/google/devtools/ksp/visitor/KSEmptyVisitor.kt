/*
 * Copyright 2010-2020 Google LLC, JetBrains s.r.o and and Kotlin Programming Language contributors.
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
 * A visitor that methods fall back to [defaultHandler] if not overridden.
 */
abstract class KSEmptyVisitor<D, R> : KSVisitor<D, R> {
    abstract fun defaultHandler(node: KSNode, data: D): R

    override fun visitNode(node: KSNode, data: D): R {
        return defaultHandler(node, data)
    }

    override fun visitAnnotated(annotated: KSAnnotated, data: D): R {
        return defaultHandler(annotated, data)
    }

    override fun visitAnnotation(annotation: KSAnnotation, data: D): R {
        return defaultHandler(annotation, data)
    }

    override fun visitModifierListOwner(modifierListOwner: KSModifierListOwner, data: D): R {
        return defaultHandler(modifierListOwner, data)
    }

    override fun visitDeclaration(declaration: KSDeclaration, data: D): R {
        return defaultHandler(declaration, data)
    }

    override fun visitDeclarationContainer(declarationContainer: KSDeclarationContainer, data: D): R {
        return defaultHandler(declarationContainer, data)
    }

    override fun visitDynamicReference(reference: KSDynamicReference, data: D): R {
        return defaultHandler(reference, data)
    }

    override fun visitFile(file: KSFile, data: D): R {
        return defaultHandler(file, data)
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: D): R {
        return defaultHandler(function, data)
    }

    override fun visitCallableReference(reference: KSCallableReference, data: D): R {
        return defaultHandler(reference, data)
    }

    override fun visitParenthesizedReference(reference: KSParenthesizedReference, data: D): R {
        return defaultHandler(reference, data)
    }

    override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: D): R {
        return defaultHandler(property, data)
    }

    override fun visitPropertyAccessor(accessor: KSPropertyAccessor, data: D): R {
        return defaultHandler(accessor, data)
    }

    override fun visitPropertyGetter(getter: KSPropertyGetter, data: D): R {
        return defaultHandler(getter, data)
    }

    override fun visitPropertySetter(setter: KSPropertySetter, data: D): R {
        return defaultHandler(setter, data)
    }

    override fun visitClassifierReference(reference: KSClassifierReference, data: D): R {
        return defaultHandler(reference, data)
    }

    override fun visitReferenceElement(element: KSReferenceElement, data: D): R {
        return defaultHandler(element, data)
    }

    override fun visitTypeAlias(typeAlias: KSTypeAlias, data: D): R {
        return defaultHandler(typeAlias, data)
    }

    override fun visitTypeArgument(typeArgument: KSTypeArgument, data: D): R {
        return defaultHandler(typeArgument, data)
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: D): R {
        return defaultHandler(classDeclaration, data)
    }

    override fun visitTypeParameter(typeParameter: KSTypeParameter, data: D): R {
        return defaultHandler(typeParameter, data)
    }

    override fun visitTypeReference(typeReference: KSTypeReference, data: D): R {
        return defaultHandler(typeReference, data)
    }

    override fun visitVariableParameter(variableParameter: KSVariableParameter, data: D): R {
        return defaultHandler(variableParameter, data)
    }

    override fun visitValueArgument(valueArgument: KSValueArgument, data: D): R {
        return defaultHandler(valueArgument, data)
    }
}