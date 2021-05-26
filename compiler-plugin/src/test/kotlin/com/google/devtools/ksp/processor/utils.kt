package com.google.devtools.ksp.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid

open class BaseVisitor : KSVisitorVoid() {
    override fun visitClassDeclaration(type: KSClassDeclaration, data: Unit) {
        for (declaration in type.declarations) {
            declaration.accept(this, Unit)
        }
    }

    override fun visitFile(file: KSFile, data: Unit) {
        for (declaration in file.declarations) {
            declaration.accept(this, Unit)
        }
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        for (declaration in function.declarations) {
            declaration.accept(this, Unit)
        }
    }
}

