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

package com.google.devtools.ksp.test

import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
class KSPCompilerPluginTest : AbstractKSPCompilerPluginTest() {
    @TestMetadata("annotatedUtil.kt")
    @Test
    fun testAnnotatedUtil() {
        runTest("../test-utils/testData/api/annotatedUtil.kt")
    }

    @TestMetadata("javaAnnotatedUtil.kt")
    @Test
    fun testJavaAnnotatedUtil() {
        runTest("../test-utils/testData/api/javaAnnotatedUtil.kt")
    }

    @TestMetadata("abstractFunctions.kt")
    @Test
    fun testAbstractFunctions() {
        runTest("../test-utils/testData/api/abstractFunctions.kt")
    }

    @TestMetadata("allFunctions_java_inherits_kt.kt")
    @Test
    fun testAllFunctions_java_inherits_kt() {
        runTest("../test-utils/testData/api/allFunctions_java_inherits_kt.kt")
    }

    @TestMetadata("allFunctions_kotlin.kt")
    @Test
    fun testAllFunctions_kotlin() {
        runTest("../test-utils/testData/api/allFunctions_java_inherits_kt.kt")
    }

    @TestMetadata("allFunctions_kt_inherits_java.kt")
    @Test
    fun testAllFunctions_kt_inherits_java() {
        runTest("../test-utils/testData/api/allFunctions_kt_inherits_java.kt")
    }

    @TestMetadata("annotationInDependencies.kt")
    @Test
    fun testAnnotationsInDependencies() {
        runTest("../test-utils/testData/api/annotationInDependencies.kt")
    }

    @TestMetadata("annotationOnConstructorParameter.kt")
    @Test
    fun testAnnotationOnConstructorParameter() {
        runTest("../test-utils/testData/api/annotationOnConstructorParameter.kt")
    }

    @TestMetadata("annotationWithArbitraryClassValue.kt")
    @Test
    fun testAnnotationWithArbitraryClassValue() {
        runTest("../test-utils/testData/api/annotationWithArbitraryClassValue.kt")
    }

    @TestMetadata("annotationValue_java.kt")
    @Test
    fun testAnnotationValue_java() {
        runTest("../test-utils/testData/api/annotationValue_java.kt")
    }

    @TestMetadata("annotationValue_kt.kt")
    @Test
    fun testAnnotationValue_kt() {
        runTest("../test-utils/testData/api/annotationValue_kt.kt")
    }

    @TestMetadata("annotationWithArrayValue.kt")
    @Test
    fun testAnnotationWithArrayValue() {
        runTest("../test-utils/testData/api/annotationWithArrayValue.kt")
    }

    @TestMetadata("annotationWithDefault.kt")
    @Test
    fun testAnnotationWithDefault() {
        runTest("../test-utils/testData/api/annotationWithDefault.kt")
    }

    @TestMetadata("annotationWithDefaultValues.kt")
    @Test
    fun testAnnotationWithDefaultValues() {
        runTest("../test-utils/testData/api/annotationWithDefaultValues.kt")
    }

    @TestMetadata("annotationWithJavaTypeValue.kt")
    @Test
    fun testAnnotationWithJavaTypeValue() {
        runTest("../test-utils/testData/api/annotationWithJavaTypeValue.kt")
    }

    @TestMetadata("asMemberOf.kt")
    @Test
    fun testAsMemberOf() {
        runTest("../test-utils/testData/api/asMemberOf.kt")
    }

    @TestMetadata("backingFields.kt")
    @Test
    fun testBackingFields() {
        runTest("../test-utils/testData/api/backingFields.kt")
    }

    @TestMetadata("builtInTypes.kt")
    @Test
    fun testBuiltInTypes() {
        runTest("../test-utils/testData/api/builtInTypes.kt")
    }

    @TestMetadata("checkOverride.kt")
    @Test
    fun testCheckOverride() {
        runTest("../test-utils/testData/api/checkOverride.kt")
    }

    @TestMetadata("classKinds.kt")
    @Test
    fun testClassKinds() {
        runTest("../test-utils/testData/api/classKinds.kt")
    }

    @TestMetadata("companion.kt")
    @Test
    fun testCompanion() {
        runTest("../test-utils/testData/api/companion.kt")
    }

    @TestMetadata("constProperties.kt")
    @Test
    fun testConstProperties() {
        runTest("../test-utils/testData/api/constProperties.kt")
    }

    @TestMetadata("constructorDeclarations.kt")
    @Test
    fun testConstructorDeclarations() {
        runTest("../test-utils/testData/api/constructorDeclarations.kt")
    }

    @TestMetadata("crossModuleTypeAlias.kt")
    @Test
    fun testCrossModuleTypeAlias() {
        runTest("../test-utils/testData/api/crossModuleTypeAlias.kt")
    }

    @TestMetadata("declarationInconsistency.kt")
    @Test
    fun testDeclarationInconsistency() {
        runTest("../test-utils/testData/api/declarationInconsistency.kt")
    }

    @TestMetadata("declarationPackageName.kt")
    @Test
    fun testDeclarationPackageName() {
        runTest("../test-utils/testData/api/declarationPackageName.kt")
    }

    @TestMetadata("declarationsInAccessor.kt")
    @Test
    fun testDeclarationsInAccessor() {
        runTest("../test-utils/testData/api/declarationsInAccessor.kt")
    }

    @TestMetadata("declarationOrder.kt")
    @Test
    fun testDeclarationOrder() {
        runTest("../test-utils/testData/api/declarationOrder.kt")
    }

    @TestMetadata("declarationUtil.kt")
    @Test
    fun testDeclarationUtil() {
        runTest("../test-utils/testData/api/declarationUtil.kt")
    }

    @TestMetadata("declared.kt")
    @Test
    fun testDeclared() {
        runTest("../test-utils/testData/api/declared.kt")
    }

    @TestMetadata("docString.kt")
    @Test
    fun testDocString() {
        runTest("../test-utils/testData/api/docString.kt")
    }

    @TestMetadata("equivalentJavaWildcards.kt")
    @Test
    fun testEquivalentJavaWildcards() {
        runTest("../test-utils/testData/api/equivalentJavaWildcards.kt")
    }

    @TestMetadata("errorTypes.kt")
    @Test
    fun testErrorTypes() {
        runTest("../test-utils/testData/api/errorTypes.kt")
    }

    @TestMetadata("functionTypeAlias.kt")
    @Test
    fun testFunctionTypeAlias() {
        runTest("../test-utils/testData/api/functionTypeAlias.kt")
    }

    @TestMetadata("functionTypeAnnotation.kt")
    @Test
    fun testFunctionTypeAnnotation() {
        runTest("../test-utils/testData/api/functionTypeAnnotation.kt")
    }

    @TestMetadata("functionTypes.kt")
    @Test
    fun testFunctionTypes() {
        runTest("../test-utils/testData/api/functionTypes.kt")
    }

    @TestMetadata("getAnnotationByTypeWithInnerDefault.kt")
    @Test
    fun testGetAnnotationByTypeWithInnerDefault() {
        runTest("../test-utils/testData/api/getAnnotationByTypeWithInnerDefault.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("getPackage.kt")
    @Test
    fun testGetPackage() {
        runTest("../test-utils/testData/api/getPackage.kt")
    }

    @TestMetadata("getByName.kt")
    @Test
    fun testGetByName() {
        runTest("../test-utils/testData/api/getByName.kt")
    }

    @TestMetadata("getSymbolsFromAnnotation.kt")
    @Test
    fun testGetSymbolsFromAnnotation() {
        runTest("../test-utils/testData/api/getSymbolsFromAnnotation.kt")
    }

    @TestMetadata("hello.kt")
    @Test
    fun testHello() {
        runTest("../test-utils/testData/api/hello.kt")
    }

    @TestMetadata("implicitElements.kt")
    @Test
    fun testImplicitElements() {
        runTest("../test-utils/testData/api/implicitElements.kt")
    }

    @TestMetadata("implicitPropertyAccessors.kt")
    @Test
    fun testImplicitPropertyAccessors() {
        runTest("../test-utils/testData/api/implicitPropertyAccessors.kt")
    }

    @TestMetadata("inheritedTypeAlias.kt")
    @Test
    fun testInheritedTypeAlias() {
        runTest("../test-utils/testData/api/inheritedTypeAlias.kt")
    }

    @TestMetadata("innerTypes.kt")
    @Test
    fun testInnerTypes() {
        runTest("../test-utils/testData/api/innerTypes.kt")
    }

    @TestMetadata("interfaceWithDefault.kt")
    @Test
    fun testInterfaceWithDefault() {
        runTest("../test-utils/testData/api/interfaceWithDefault.kt")
    }

    @TestMetadata("javaModifiers.kt")
    @Test
    fun testJavaModifiers() {
        runTest("../test-utils/testData/api/javaModifiers.kt")
    }

    @TestMetadata("javaNonNullTypes.kt")
    @Test
    fun testJavaNonNullTypes() {
        runTest("../test-utils/testData/api/javaNonNullTypes.kt")
    }

    @TestMetadata("javaSubtype.kt")
    @Test
    fun testJavaSubtype() {
        runTest("../test-utils/testData/api/javaSubtype.kt")
    }

    @TestMetadata("javaToKotlinMapper.kt")
    @Test
    fun testJavaToKotlinMapper() {
        runTest("../test-utils/testData/api/javaToKotlinMapper.kt")
    }

    @TestMetadata("javaTypes.kt")
    @Test
    fun testJavaTypes() {
        runTest("../test-utils/testData/api/javaTypes.kt")
    }

    @TestMetadata("javaTypes2.kt")
    @Test
    fun testJavaTypes2() {
        runTest("../test-utils/testData/api/javaTypes2.kt")
    }

    @TestMetadata("javaWildcards2.kt")
    @Test
    fun testJavaWildcards2() {
        runTest("../test-utils/testData/api/javaWildcards2.kt")
    }

    @TestMetadata("lateinitProperties.kt")
    @Test
    fun testLateinitProperties() {
        runTest("../test-utils/testData/api/lateinitProperties.kt")
    }

    @TestMetadata("libOrigins.kt")
    @Test
    fun testLibOrigins() {
        runTest("../test-utils/testData/api/libOrigins.kt")
    }

    @TestMetadata("makeNullable.kt")
    @Test
    fun testMakeNullable() {
        runTest("../test-utils/testData/api/makeNullable.kt")
    }

    @TestMetadata("mangledNames.kt")
    @Test
    fun testMangledNames() {
        runTest("../test-utils/testData/api/mangledNames.kt")
    }

    @TestMetadata("multipleModules.kt")
    @Test
    fun testMultipleModules() {
        runTest("../test-utils/testData/api/multipleModules.kt")
    }

    @TestMetadata("nestedAnnotations.kt")
    @Test
    fun testNestedAnnotations() {
        runTest("../test-utils/testData/api/nestedAnnotations.kt")
    }

    @TestMetadata("nestedClassType.kt")
    @Test
    fun testNestedClassType() {
        runTest("../test-utils/testData/api/nestedClassType.kt")
    }

    @TestMetadata("nullableTypes.kt")
    @Test
    fun testNullableTypes() {
        runTest("../test-utils/testData/api/nullableTypes.kt")
    }

    @TestMetadata("overridee.kt")
    @Test
    fun testOverridee() {
        runTest("../test-utils/testData/api/overridee.kt")
    }

    @TestMetadata("parameterTypes.kt")
    @Test
    fun testParameterTypes() {
        runTest("../test-utils/testData/api/parameterTypes.kt")
    }

    @TestMetadata("parent.kt")
    @Test
    fun testParent() {
        runTest("../test-utils/testData/api/parent.kt")
    }

    @TestMetadata("platformDeclaration.kt")
    @Test
    fun testPlatformDeclaration() {
        runTest("../test-utils/testData/api/platformDeclaration.kt")
    }

    @TestMetadata("rawTypes.kt")
    @Test
    fun testRawTypes() {
        runTest("../test-utils/testData/api/rawTypes.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("recordJavaAnnotationTypes.kt")
    @Test
    fun testRecordJavaAnnotationTypes() {
        runTest("../test-utils/testData/api/recordJavaAnnotationTypes.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("recordJavaAsMemberOf.kt")
    @Test
    fun testRecordJavaAsMemberOf() {
        runTest("../test-utils/testData/api/recordJavaAsMemberOf.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("recordJavaGetAllMembers.kt")
    @Test
    fun testRecordJavaGetAllMembers() {
        runTest("../test-utils/testData/api/recordJavaGetAllMembers.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("recordJavaOverrides.kt")
    @Test
    fun testRecordJavaOverrides() {
        runTest("../test-utils/testData/api/recordJavaOverrides.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("recordJavaSupertypes.kt")
    @Test
    fun testRecordJavaSupertypes() {
        runTest("../test-utils/testData/api/recordJavaSupertypes.kt")
    }

    @TestMetadata("referenceElement.kt")
    @Test
    fun testReferenceElement() {
        runTest("../test-utils/testData/api/referenceElement.kt")
    }

    @TestMetadata("replaceWithErrorTypeArgs.kt")
    @Test
    fun testReplaceWithErrorTypeArgs() {
        runTest("../test-utils/testData/api/replaceWithErrorTypeArgs.kt")
    }

    @TestMetadata("resolveJavaType.kt")
    @Test
    fun testResolveJavaType() {
        runTest("../test-utils/testData/api/resolveJavaType.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("sealedClass.kt")
    @Test
    fun testSealedClass() {
        runTest("../test-utils/testData/api/sealedClass.kt")
    }

    @TestMetadata("signatureMapper.kt")
    @Test
    fun testSignatureMapper() {
        runTest("../test-utils/testData/api/signatureMapper.kt")
    }

    @TestMetadata("superTypes.kt")
    @Test
    fun testSuperTypes() {
        runTest("../test-utils/testData/api/superTypes.kt")
    }

    @TestMetadata("throwList.kt")
    @Test
    fun testThrowList() {
        runTest("../test-utils/testData/api/throwList.kt")
    }

    @TestMetadata("topLevelMembers.kt")
    @Test
    fun testTopLevelMembers() {
        runTest("../test-utils/testData/api/topLevelMembers.kt")
    }

    @TestMetadata("typeAlias.kt")
    @Test
    fun testTypeAlias() {
        runTest("../test-utils/testData/api/typeAlias.kt")
    }

    @TestMetadata("typeAliasComparison.kt")
    @Test
    fun testTypeAliasComparison() {
        runTest("../test-utils/testData/api/typeAliasComparison.kt")
    }

    @TestMetadata("typeAnnotation.kt")
    @Test
    fun testTypeAnnotation() {
        runTest("../test-utils/testData/api/typeAnnotation.kt")
    }

    @TestMetadata("typeComposure.kt")
    @Test
    fun testTypeComposure() {
        runTest("../test-utils/testData/api/typeComposure.kt")
    }

    @TestMetadata("typeParameterEquals.kt")
    @Test
    fun testTypeParameterEquals() {
        runTest("../test-utils/testData/api/typeParameterEquals.kt")
    }

    @TestMetadata("typeParameterReference.kt")
    @Test
    fun testTypeParameterReference() {
        runTest("../test-utils/testData/api/typeParameterReference.kt")
    }

    @TestMetadata("varianceTypeCheck.kt")
    @Test
    fun testVarianceTypeCheck() {
        runTest("../test-utils/testData/api/varianceTypeCheck.kt")
    }

    @TestMetadata("validateTypes.kt")
    @Test
    fun testValidateTypes() {
        runTest("../test-utils/testData/api/validateTypes.kt")
    }

    @TestMetadata("visibilities.kt")
    @Test
    fun testVisibilities() {
        runTest("../test-utils/testData/api/visibilities.kt")
    }
}
