import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.*
import java.io.File
import java.io.OutputStream


class TestProcessor : SymbolProcessor {
    lateinit var codeGenerator: CodeGenerator
    lateinit var file: OutputStream

    fun emit(s: String, indent: String) {
        file.appendText("$indent$s\n")
    }

    override fun finish() {
        emit("TestProcessor: finish()", "")
        file.close()
    }

    override fun init(options: Map<String, String>, kotlinVersion: KotlinVersion, codeGenerator: CodeGenerator, logger: KSPLogger) {
        this.codeGenerator = codeGenerator
        file = codeGenerator.createNewFile(Dependencies(false), "", "TestProcessor", "log")
        emit("TestProcessor: init($options)", "")

        val javaFile = codeGenerator.createNewFile(Dependencies(false), "", "Generated", "java")
        javaFile.appendText("class Generated {}")
    }

    override fun process(resolver: Resolver) {
        val fileKt = codeGenerator.createNewFile(Dependencies(false), "", "HELLO", "java")
        fileKt.appendText("public class HELLO{\n")
        fileKt.appendText("public int foo() { return 1234; }\n")
        fileKt.appendText("}")

        val files = resolver.getAllFiles()
        emit("TestProcessor: process()", "")
        val visitor = TestVisitor()
        for (file in files) {
            emit("TestProcessor: processing ${file.fileName}", "")
            file.accept(visitor, "")
        }
    }

    inner class TestVisitor : KSVisitor<String, Unit> {

        override fun visitReferenceElement(element: KSReferenceElement, data: String) {
        }

        override fun visitModifierListOwner(modifierListOwner: KSModifierListOwner, data: String) {
            TODO("Not yet implemented")
        }

        override fun visitNode(node: KSNode, data: String) {
            TODO("Not yet implemented")
        }

        override fun visitPropertyAccessor(accessor: KSPropertyAccessor, data: String) {
            TODO("Not yet implemented")
        }

        override fun visitDynamicReference(reference: KSDynamicReference, data: String) {
            TODO("Not yet implemented")
        }
        val visited = HashSet<Any>()

        private fun checkVisited(symbol: Any): Boolean {
            return if (visited.contains(symbol)) {
                true
            } else {
                visited.add(symbol)
                false
            }
        }

        private fun invokeCommonDeclarationApis(declaration: KSDeclaration, indent: String) {
            emit(
              "${declaration.modifiers.joinToString(" ")} ${declaration.simpleName.asString()}", indent
            )
            declaration.annotations.map { it.accept(this, "$indent  ") }
            if (declaration.parentDeclaration != null)
                emit("  enclosing: ${declaration.parentDeclaration!!.qualifiedName?.asString()}", indent)
            declaration.containingFile?.let { emit("${it.packageName.asString()}.${it.fileName}", indent) }
            declaration.typeParameters.map { it.accept(this, "$indent  ") }
        }

        override fun visitFile(file: KSFile, data: String) {
//            if (!file.packageName.asString().startsWith("eu.kanade.tachiyomi.data")) {
//                return
//            }
            if (checkVisited(file)) return
            file.annotations.map{ it.accept(this, "$data  ") }
            emit(file.packageName.asString(), data)
            for (declaration in file.declarations) {
                declaration.accept(this, data)
            }
        }

        override fun visitAnnotation(annotation: KSAnnotation, data: String) {
            if (checkVisited(annotation)) return
            emit("annotation", data)
            annotation.annotationType.accept(this, "$data  ")
            annotation.arguments.map { it.accept(this, "$data  ") }
        }

        override fun visitCallableReference(reference: KSCallableReference, data: String) {
            if (checkVisited(reference)) return
            emit("element: ", data)
            reference.functionParameters.map { it.accept(this, "$data  ") }
            reference.receiverType?.accept(this, "$data receiver")
            reference.returnType.accept(this, "$data  ")
        }

        override fun visitPropertyGetter(getter: KSPropertyGetter, data: String) {
            if (checkVisited(getter)) return
            emit("propertyGetter: ", data)
            getter.annotations.map { it.accept(this, "$data  ") }
            emit(getter.modifiers.joinToString(" "), data)
            getter.returnType?.accept(this, "$data  ")
        }

        override fun visitPropertySetter(setter: KSPropertySetter, data: String) {
            if (checkVisited(setter)) return
            emit("propertySetter: ", data)
            setter.annotations.map { it.accept(this, "$data  ") }
            emit(setter.modifiers.joinToString(" "), data)
//            setter.parameter.accept(this, "$data  ")
        }

        override fun visitTypeArgument(typeArgument: KSTypeArgument, data: String) {
            if (checkVisited(typeArgument)) return
            typeArgument.annotations.map{ it.accept(this, "$data  ") }
            emit(
              when (typeArgument.variance) {
                  Variance.STAR -> "*"
                  Variance.COVARIANT -> "out"
                  Variance.CONTRAVARIANT -> "in"
                  else -> ""
              }, data
            )
            typeArgument.type?.accept(this, "$data  ")
        }

        override fun visitTypeParameter(typeParameter: KSTypeParameter, data: String) {
            if (checkVisited(typeParameter)) return
            typeParameter.annotations.map{ it.accept(this, "$data  ") }
            if (typeParameter.isReified) {
                emit("reified ", data)
            }
            emit(
              when (typeParameter.variance) {
                  Variance.COVARIANT -> "out "
                  Variance.CONTRAVARIANT -> "in "
                  else -> ""
              } + typeParameter.name.asString(), data
            )
            if (typeParameter.bounds.isNotEmpty()) {
                typeParameter.bounds.map { it.accept(this, "$data  ") }
            }
        }

        override fun visitValueParameter(valueParameter: KSValueParameter, data: String) {
            if (checkVisited(valueParameter)) return
            valueParameter.annotations.map { it.accept(this, "$data  ") }
            if (valueParameter.isVararg) {
                emit("vararg", "$data  ")
            }
            if (valueParameter.isNoInline) {
                emit("noinline", "$data  ")
            }
            if (valueParameter.isCrossInline) {
                emit("crossinline ", "$data  ")
            }
            emit(valueParameter.name?.asString() ?: "_", "$data  ")
            valueParameter.type.accept(this, "$data  ")
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: String) {
            if (checkVisited(function)) return
            invokeCommonDeclarationApis(function, data)
            for (declaration in function.declarations) {
                declaration.accept(this, "$data  ")
            }
            function.parameters.map { it.accept(this, "$data  ") }
            function.typeParameters.map { it.accept(this, "$data  ") }
            function.extensionReceiver?.accept(this, "$data extension:")
            emit("returnType:", data)
            function.returnType?.accept(this, "$data  ")
        }

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: String) {
            if (checkVisited(classDeclaration)) return
            invokeCommonDeclarationApis(classDeclaration, data)
            emit(classDeclaration.classKind.type, data)
            for (declaration in classDeclaration.declarations) {
                declaration.accept(this, "$data ")
            }
            classDeclaration.superTypes.map { it.accept(this, "$data  ") }
            classDeclaration.primaryConstructor?.accept(this, "$data  ")
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: String) {
            if (checkVisited(property)) return
            invokeCommonDeclarationApis(property, data)
            property.type.accept(this, "$data  ")
            property.extensionReceiver?.accept(this, "$data extension:")
            property.setter?.accept(this, "$data  ")
            property.getter?.accept(this, "$data  ")
        }

        override fun visitTypeReference(typeReference: KSTypeReference, data: String) {
            if (checkVisited(typeReference)) return
            typeReference.annotations.map{ it.accept(this, "$data  ") }
            val type = typeReference.resolve()
            type.let {
                emit("resolved to: ${it.declaration.qualifiedName?.asString()}", data)
            }
            //resolved.accept(this, "$data  ")
            // TODO: KSTypeReferenceJavaImpl hasn't completed yet.
            try {
                typeReference.element?.accept(this, "$data  ")
            } catch (e: IllegalStateException) {
                emit("TestProcessor: exception: $e", data)
            }
        }

        override fun visitAnnotated(annotated: KSAnnotated, data: String) {
        }

        override fun visitDeclaration(declaration: KSDeclaration, data: String) {
        }

        override fun visitDeclarationContainer(declarationContainer: KSDeclarationContainer, data: String) {
        }

        override fun visitParenthesizedReference(reference: KSParenthesizedReference, data: String) {
        }

        override fun visitClassifierReference(reference: KSClassifierReference, data: String) {
            if (checkVisited(reference)) return
            if (reference.typeArguments.isNotEmpty()) {
                reference.typeArguments.map { it.accept(this, "$data  ") }
            }
        }

        override fun visitTypeAlias(typeAlias: KSTypeAlias, data: String) {
        }

        override fun visitValueArgument(valueArgument: KSValueArgument, data: String) {
            if (checkVisited(valueArgument)) return
            val name = valueArgument.name?.asString() ?: "<no name>"
            emit("$name: ${valueArgument.value}", data)
            valueArgument.annotations.map { it.accept(this, "$data  ") }
        }
    }

}


