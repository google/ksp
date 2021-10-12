import com.example.annotation.Builder
import com.squareup.javapoet.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement

class JavaBuilderProcessor : AbstractProcessor() {

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }
    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf("com.example.annotation.Builder")
    }

    private fun processType(typeElement: TypeElement) {
        val typeSpecBuilder = TypeSpec.classBuilder("${typeElement.simpleName}Builder")
        val constructor = typeElement.enclosedElements.filterIsInstance<ExecutableElement>()
                .filter { it.simpleName.toString() == "<init>" }
                .first()

        constructor.parameters.forEach {
            typeSpecBuilder.addField(ClassName.get(it.asType()), it.simpleName.toString(), Modifier.PRIVATE)
            val parameterSpec = ParameterSpec.builder(ClassName.get(it.asType()), it.simpleName.toString()).build()
            typeSpecBuilder.addMethod(
                    MethodSpec.methodBuilder("with${it.simpleName.toString().capitalize()}")
                            .addParameter(parameterSpec)
                            .addModifiers(Modifier.PUBLIC)
                            .addStatement("this.\$N = \$N", it.simpleName, it.simpleName)
                            .addStatement("return this")
                            .returns(ClassName.bestGuess("${typeElement.qualifiedName}Builder"))
                            .build()
            )
        }
        val statements = constructor.parameters.map { it.simpleName.toString() }.joinToString(", ")
        typeSpecBuilder.addMethod(
                MethodSpec.methodBuilder("build")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(ClassName.get(typeElement.asType()))
                        .addStatement("return new ${typeElement.simpleName}($statements)")
                        .build()
        )
        val javaFileBuilder = JavaFile.builder((typeElement.enclosingElement as PackageElement).qualifiedName.toString(), typeSpecBuilder.build())
        javaFileBuilder.skipJavaLangImports(true).build().writeTo(processingEnv.filer)
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        val elements = roundEnv!!.getElementsAnnotatedWith(Builder::class.java)
        elements.filterIsInstance<TypeElement>()
                .map { processType(it) }
        return true
    }

}