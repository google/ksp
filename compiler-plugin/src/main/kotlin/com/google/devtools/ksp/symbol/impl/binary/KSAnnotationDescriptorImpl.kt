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

package com.google.devtools.ksp.symbol.impl.binary

import com.google.devtools.ksp.ExceptionMessage
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.findPsi
import com.google.devtools.ksp.symbol.impl.kotlin.KSNameImpl
import com.google.devtools.ksp.symbol.impl.kotlin.KSValueArgumentLiteImpl
import com.google.devtools.ksp.symbol.impl.kotlin.getKSTypeCached
import com.google.devtools.ksp.symbol.impl.synthetic.KSTypeReferenceSyntheticImpl
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotationMethod
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.load.java.components.JavaAnnotationDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaAnnotationDescriptor
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.VirtualFileBoundJavaClass
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassSignatureParser
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaAnnotationVisitor
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaMethod
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.ClassifierResolutionContext
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClass
import org.jetbrains.kotlin.load.kotlin.getContainingKotlinJvmBinaryClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes.API_VERSION

class KSAnnotationDescriptorImpl private constructor(
    val descriptor: AnnotationDescriptor,
    override val parent: KSNode?
) : KSAnnotation {
    companion object : KSObjectCache<Pair<AnnotationDescriptor, KSNode?>, KSAnnotationDescriptorImpl>() {
        fun getCached(descriptor: AnnotationDescriptor, parent: KSNode?) = cache.getOrPut(Pair(descriptor, parent)) {
            KSAnnotationDescriptorImpl(descriptor, parent)
        }
    }

    override val origin =
        when (descriptor) {
            is JavaAnnotationDescriptor, is LazyJavaAnnotationDescriptor -> Origin.JAVA_LIB
            else -> Origin.KOTLIN_LIB
        }

    override val location: Location = NonExistLocation

    override val annotationType: KSTypeReference by lazy {
        KSTypeReferenceDescriptorImpl.getCached(descriptor.type, origin, this)
    }

    override val arguments: List<KSValueArgument> by lazy {
        descriptor.createKSValueArguments(this)
    }

    override val shortName: KSName by lazy {
        KSNameImpl.getCached(descriptor.fqName!!.shortName().asString())
    }

    override val useSiteTarget: AnnotationUseSiteTarget? = null

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnnotation(this, data)
    }

    override fun toString(): String {
        return "@${shortName.asString()}"
    }
}

private fun ClassId.findKSClassDeclaration(): KSClassDeclaration? {
    val ksName = KSNameImpl.getCached(this.asSingleFqName().asString())
    return ResolverImpl.instance.getClassDeclarationByName(ksName)
}

private fun ClassId.findKSType(): KSType? = findKSClassDeclaration()?.asStarProjectedType()

private fun <T> ConstantValue<T>.toValue(parent: KSNode): Any? = when (this) {
    is AnnotationValue -> KSAnnotationDescriptorImpl.getCached(value, parent)
    is ArrayValue -> value.map { it.toValue(parent) }
    is EnumValue -> value.first.findKSClassDeclaration()?.declarations?.find {
        it is KSClassDeclaration && it.classKind == ClassKind.ENUM_ENTRY &&
            it.simpleName.asString() == value.second.asString()
    }?.let { (it as KSClassDeclaration).asStarProjectedType() }
    is KClassValue -> when (val classValue = value) {
        is KClassValue.Value.NormalClass -> if (classValue.arrayDimensions > 0) {
            classValue.value.classId.findKSType()?.let { componentType ->
                var resultingType = componentType
                for (i in 1..classValue.arrayDimensions) {
                    resultingType = ResolverImpl.instance.builtIns.arrayType.replace(
                        listOf(
                            ResolverImpl.instance.getTypeArgument(
                                KSTypeReferenceSyntheticImpl.getCached(resultingType, null), Variance.INVARIANT
                            )
                        )
                    )
                }
                resultingType
            }
        } else classValue.classId.findKSType()
        is KClassValue.Value.LocalClass -> getKSTypeCached(classValue.type)
    }
    is ErrorValue, is NullValue -> null
    else -> value
}

fun AnnotationDescriptor.createKSValueArguments(ownerAnnotation: KSAnnotation): List<KSValueArgument> {
    val presentValueArguments = allValueArguments.map { (name, constantValue) ->
        KSValueArgumentLiteImpl.getCached(
            KSNameImpl.getCached(name.asString()),
            constantValue.toValue(ownerAnnotation),
            ownerAnnotation
        )
    }
    val presentValueArgumentNames = presentValueArguments.map { it.name.asString() }
    val argumentsFromDefault = (this.type.constructor.declarationDescriptor as? ClassDescriptor)?.constructors?.single()
        ?.let { argumentsFromDefault ->
            argumentsFromDefault.getAbsentDefaultArguments(presentValueArgumentNames, ownerAnnotation)
        } ?: emptyList()
    return presentValueArguments.plus(argumentsFromDefault)
}

fun ClassConstructorDescriptor.getAbsentDefaultArguments(
    excludeNames: List<String>,
    ownerAnnotation: KSAnnotation
): List<KSValueArgument> {
    return this.valueParameters
        .filterNot { param -> excludeNames.contains(param.name.asString()) || !param.hasDefaultValue() }
        .map { param ->
            KSValueArgumentLiteImpl.getCached(
                KSNameImpl.getCached(param.name.asString()),
                param.getDefaultValue(ownerAnnotation),
                ownerAnnotation
            )
        }
}

fun ValueParameterDescriptor.getDefaultValue(ownerAnnotation: KSAnnotation): Any? {

    // Copied from kotlin compiler
    // TODO: expose in upstream
    fun convertTypeToKClassValue(javaType: JavaType): KClassValue? {
        var type = javaType
        var arrayDimensions = 0
        while (type is JavaArrayType) {
            type = type.componentType
            arrayDimensions++
        }
        return when (type) {
            is JavaPrimitiveType -> {
                val primitiveType = type.type
                    // void.class is not representable in Kotlin, we approximate it by Unit::class
                    ?: return KClassValue(ClassId.topLevel(StandardNames.FqNames.unit.toSafe()), 0)
                if (arrayDimensions > 0) {
                    KClassValue(ClassId.topLevel(primitiveType.arrayTypeFqName), arrayDimensions - 1)
                } else {
                    KClassValue(ClassId.topLevel(primitiveType.typeFqName), arrayDimensions)
                }
            }
            is JavaClassifierType -> {
                val fqName = FqName(type.classifierQualifiedName)
                // TODO: support nested classes somehow
                val classId = JavaToKotlinClassMap.mapJavaToKotlin(fqName) ?: ClassId.topLevel(fqName)
                KClassValue(classId, arrayDimensions)
            }
            else -> null
        }
    }

    // Copied from kotlin compiler
    // TODO: expose in upstream
    fun JavaAnnotationArgument.convert(expectedType: KotlinType): ConstantValue<*>? {
        return when (this) {
            is JavaLiteralAnnotationArgument -> value?.let {
                when (value) {
                    // Note: `value` expression may be of class that does not match field type in some cases
                    // tested for Int, left other checks just in case
                    is Byte, is Short, is Int, is Long -> {
                        ConstantValueFactory.createIntegerConstantValue((value as Number).toLong(), expectedType, false)
                    }
                    else -> {
                        ConstantValueFactory.createConstantValue(value)
                    }
                }
            }
            is JavaEnumValueAnnotationArgument -> {
                enumClassId?.let { enumClassId ->
                    entryName?.let { entryName ->
                        EnumValue(enumClassId, entryName)
                    }
                }
            }
            is JavaArrayAnnotationArgument -> {
                val elementType = expectedType.builtIns.getArrayElementType(expectedType)
                ConstantValueFactory.createArrayValue(
                    getElements().mapNotNull { it.convert(elementType) },
                    expectedType
                )
            }
            is JavaAnnotationAsAnnotationArgument -> {
                AnnotationValue(
                    LazyJavaAnnotationDescriptor(ResolverImpl.lazyJavaResolverContext, this.getAnnotation())
                )
            }
            is JavaClassObjectAnnotationArgument -> {
                convertTypeToKClassValue(getReferencedType())
            }
            else -> null
        }
    }

    val psi = this.findPsi()
    return when (psi) {
        null -> {
            val file = if (this.source is JavaSourceElement) {
                (
                    ((this.source as JavaSourceElement).javaElement as? BinaryJavaMethod)
                        ?.containingClass as? VirtualFileBoundJavaClass
                    )?.virtualFile?.contentsToByteArray()
            } else {
                (this.containingDeclaration.getContainingKotlinJvmBinaryClass() as? VirtualFileKotlinClass)
                    ?.file?.contentsToByteArray()
            }
            if (file == null) {
                null
            } else {
                var defaultValue: JavaAnnotationArgument? = null
                ClassReader(file).accept(
                    object : ClassVisitor(API_VERSION) {
                        override fun visitMethod(
                            access: Int,
                            name: String?,
                            desc: String?,
                            signature: String?,
                            exceptions: Array<out String>?
                        ): MethodVisitor {
                            return if (name == this@getDefaultValue.name.asString()) {
                                object : MethodVisitor(API_VERSION) {
                                    override fun visitAnnotationDefault(): AnnotationVisitor =
                                        BinaryJavaAnnotationVisitor(
                                            ClassifierResolutionContext { null },
                                            BinaryClassSignatureParser()
                                        ) {
                                            defaultValue = it
                                        }
                                }
                            } else
                                object : MethodVisitor(API_VERSION) {}
                        }
                    },
                    ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
                )
                defaultValue?.convert(this.type)?.toValue(ownerAnnotation)
            }
        }
        is KtParameter -> ResolverImpl.instance.evaluateConstant(psi.defaultValue, this.type)?.toValue(ownerAnnotation)
        is PsiAnnotationMethod -> JavaPsiFacade.getInstance(psi.project).constantEvaluationHelper
            .computeConstantExpression((psi).defaultValue)
        else -> throw IllegalStateException("Unexpected psi ${psi.javaClass}, $ExceptionMessage")
    }
}
