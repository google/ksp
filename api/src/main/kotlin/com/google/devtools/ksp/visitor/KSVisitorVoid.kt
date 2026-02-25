package com.google.devtools.ksp.visitor

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSCallableReference
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSDeclarationContainer
import com.google.devtools.ksp.symbol.KSDefNonNullReference
import com.google.devtools.ksp.symbol.KSDynamicReference
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSModifierListOwner
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSParenthesizedReference
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.symbol.KSReferenceElement
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitor

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

    override fun visitDefNonNullReference(reference: KSDefNonNullReference, data: Unit) {
    }
}
