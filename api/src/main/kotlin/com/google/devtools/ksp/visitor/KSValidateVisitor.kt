package com.google.devtools.ksp.visitor

import com.google.devtools.ksp.ExceptionMessage
import com.google.devtools.ksp.symbol.*

class KSValidateVisitor(private val predicate: (KSNode, KSNode) -> Boolean) : KSDefaultVisitor<Unit, Boolean>() {
    private fun validateDeclarations(declarationContainer: KSDeclarationContainer): Boolean {
        return !declarationContainer.declarations.any { predicate(declarationContainer, it) && !it.accept(this, Unit) }
    }

    private fun validateTypeParameters(declaration: KSDeclaration): Boolean {
        return !declaration.typeParameters.any { predicate(declaration, it) && !it.accept(this, Unit) }
    }

    private fun validateType(type: KSType): Boolean {
        return !type.isError && !type.arguments.any { it.type?.accept(this, Unit) == false }
    }

    override fun defaultHandler(node: KSNode, data: Unit): Boolean {
        throw IllegalStateException("unhandled validation condition, $ExceptionMessage")
    }

    override fun visitTypeReference(typeReference: KSTypeReference, data: Unit): Boolean {
        return validateType(typeReference.resolve())
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit): Boolean {
        if (!validateTypeParameters(classDeclaration)) {
            return false
        }
        if (classDeclaration.asStarProjectedType().isError) {
            return false
        }
        if (classDeclaration.superTypes.any { predicate(classDeclaration, it) && !it.accept(this, Unit) }) {
            return false
        }
        if (!validateDeclarations(classDeclaration)) {
            return false
        }
        return true
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit): Boolean {
        if (function.returnType != null && !(predicate(function, function.returnType!!) && function.returnType!!.accept(this, data))) {
            return false
        }
        if (function.parameters.any { predicate(function, it) && !it.accept(this, Unit)}) {
            return false
        }
        if (!validateTypeParameters(function)) {
            return false
        }
        if (!validateDeclarations(function)) {
            return false
        }
        return true
    }

    override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit): Boolean {
        if (predicate(property, property.type) && property.type.resolve().isError) {
            return false
        }
        if (!validateTypeParameters(property)) {
            return false
        }
        return true
    }

    override fun visitTypeParameter(typeParameter: KSTypeParameter, data: Unit): Boolean {
        if (typeParameter.bounds.any { predicate(typeParameter, it) && !it.accept(this, Unit) }) {
            return false
        }
        return true
    }

    override fun visitValueParameter(valueParameter: KSValueParameter, data: Unit): Boolean {
        return valueParameter.type?.accept(this, Unit) != false
    }
}