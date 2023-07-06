package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSEmptyVisitor

class LocalDeclarationProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()
    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val visitor = LocalDeclarationVisitor()
        resolver.getAllFiles().forEach { it.accept(visitor, result) }
        return emptyList()
    }
}

class LocalDeclarationVisitor : KSEmptyVisitor<MutableList<String>, Unit>() {
    override fun defaultHandler(node: KSNode, data: MutableList<String>) {
        data.add(node.toString())
    }

    override fun visitFile(file: KSFile, data: MutableList<String>) {
        super.visitFile(file, data)
        file.declarations.forEach { it.accept(this, data) }
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: MutableList<String>) {
        super.visitClassDeclaration(classDeclaration, data)
        classDeclaration.declarations.forEach { it.accept(this, data) }
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: MutableList<String>) {
        super.visitFunctionDeclaration(function, data)
        function.declarations.forEach { it.accept(this, data) }
    }

    override fun visitPropertyGetter(getter: KSPropertyGetter, data: MutableList<String>) {
        getter.declarations.forEach { it.accept(this, data) }
    }

    override fun visitPropertySetter(setter: KSPropertySetter, data: MutableList<String>) {
        setter.declarations.forEach { it.accept(this, data) }
    }

    override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: MutableList<String>) {
        super.visitPropertyDeclaration(property, data)
        property.getter?.accept(this, data)
        property.setter?.accept(this, data)
    }
}
