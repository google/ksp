/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.visitor

import org.jetbrains.kotlin.ksp.symbol.*

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

    override fun visitEnumEntryDeclaration(enumEntryDeclaration: KSEnumEntryDeclaration, data: D): R {
        return defaultHandler(enumEntryDeclaration, data)
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