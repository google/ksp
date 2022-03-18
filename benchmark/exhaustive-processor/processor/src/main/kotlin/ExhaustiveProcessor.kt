/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSTopDownVisitor
import java.io.File

class ExhaustiveProcessor(
    val codeGenerator: CodeGenerator,
    val options: Map<String, String>
) : SymbolProcessor {
    val file: File? = null
    override fun finish() {
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        file?.writeText("")
        val visitor = ExhaustiveVisitor()
        resolver.getAllFiles().forEach { it.accept(visitor, Unit) }
        file?.appendText(visitor.collectedNodes.joinToString("\n"))
        return emptyList()
    }

    inner class ExhaustiveVisitor : KSTopDownVisitor<Unit, Unit>() {
        val collectedNodes = mutableListOf<String>()

        override fun defaultHandler(node: KSNode, data: Unit) {
            file?.appendText("--- visiting ${node.location} ---\n")
        }

        override fun visitAnnotated(annotated: KSAnnotated, data: Unit) {
            super.visitAnnotated(annotated, data)
        }

        override fun visitAnnotation(annotation: KSAnnotation, data: Unit) {
            file?.appendText("ANNO: ${annotation.shortName}")
            annotation.shortName
            annotation.useSiteTarget
            super.visitAnnotation(annotation, data)
        }

        override fun visitCallableReference(reference: KSCallableReference, data: Unit) {
            super.visitCallableReference(reference, data)
        }

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            classDeclaration.classKind
            classDeclaration.primaryConstructor?.accept(this, Unit)
            classDeclaration.isCompanionObject
            classDeclaration.getAllFunctions().map { it.accept(this, Unit) }
            classDeclaration.getAllProperties().map { it.accept(this, Unit) }
            classDeclaration.asStarProjectedType()
            if (classDeclaration.origin != Origin.KOTLIN_LIB && classDeclaration.origin != Origin.JAVA_LIB) {
                classDeclaration.superTypes.map { it.accept(this, Unit) }
            }
            return super.visitClassDeclaration(classDeclaration, data)
        }

        override fun visitClassifierReference(reference: KSClassifierReference, data: Unit) {
            reference.referencedName()
            super.visitClassifierReference(reference, data)
        }

        override fun visitDeclaration(declaration: KSDeclaration, data: Unit) {
            declaration.containingFile
            declaration.packageName
            declaration.qualifiedName
            declaration.simpleName
            declaration.parentDeclaration
            super.visitDeclaration(declaration, data)
        }

        override fun visitDeclarationContainer(declarationContainer: KSDeclarationContainer, data: Unit) {
            super.visitDeclarationContainer(declarationContainer, data)
        }

        override fun visitDynamicReference(reference: KSDynamicReference, data: Unit) {
            super.visitDynamicReference(reference, data)
        }

        override fun visitFile(file: KSFile, data: Unit) {
            file.fileName
            file.packageName
            file.filePath
            super.visitFile(file, data)
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            function.functionKind
            function.isAbstract
            function.findOverridee()
            super.visitFunctionDeclaration(function, data)
        }

        override fun visitModifierListOwner(modifierListOwner: KSModifierListOwner, data: Unit) {
            modifierListOwner.modifiers
            super.visitModifierListOwner(modifierListOwner, data)
        }

        override fun visitNode(node: KSNode, data: Unit) {
            collectedNodes.add(node.origin.toString())
            collectedNodes.add(node.location.toString())
            super.visitNode(node, data)
        }

        override fun visitParenthesizedReference(reference: KSParenthesizedReference, data: Unit) {
            super.visitParenthesizedReference(reference, data)
        }

        override fun visitPropertyAccessor(accessor: KSPropertyAccessor, data: Unit) {
            accessor.receiver
            super.visitPropertyAccessor(accessor, data)
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
            if (!property.isLocal()) {
                property.isMutable
                property.findOverridee()
                property.isDelegated()
            }
            super.visitPropertyDeclaration(property, data)
        }

        override fun visitPropertyGetter(getter: KSPropertyGetter, data: Unit) {
            super.visitPropertyGetter(getter, data)
        }

        override fun visitPropertySetter(setter: KSPropertySetter, data: Unit) {
            super.visitPropertySetter(setter, data)
        }

        override fun visitReferenceElement(element: KSReferenceElement, data: Unit) {
            super.visitReferenceElement(element, data)
        }

        override fun visitTypeAlias(typeAlias: KSTypeAlias, data: Unit) {
            typeAlias.name
            super.visitTypeAlias(typeAlias, data)
        }

        override fun visitTypeArgument(typeArgument: KSTypeArgument, data: Unit) {
            typeArgument.variance
            super.visitTypeArgument(typeArgument, data)
        }

        override fun visitTypeParameter(typeParameter: KSTypeParameter, data: Unit) {
            typeParameter.name
            typeParameter.variance
            typeParameter.isReified
            super.visitTypeParameter(typeParameter, data)
        }

        override fun visitTypeReference(typeReference: KSTypeReference, data: Unit) {
            typeReference.resolve()
            super.visitTypeReference(typeReference, data)
        }

        override fun visitValueArgument(valueArgument: KSValueArgument, data: Unit) {
            valueArgument.name
            valueArgument.isSpread
            valueArgument.value
            super.visitValueArgument(valueArgument, data)
        }

        override fun visitValueParameter(valueParameter: KSValueParameter, data: Unit) {
            valueParameter.name
            valueParameter.isCrossInline
            valueParameter.isNoInline
            valueParameter.isVal
            valueParameter.isVar
            valueParameter.isVararg
            valueParameter.hasDefault
            super.visitValueParameter(valueParameter, data)
        }
    }
}

class ExhaustiveProcessorProvider : SymbolProcessorProvider {
    override fun create(
        env: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return ExhaustiveProcessor(env.codeGenerator, env.options)
    }
}
