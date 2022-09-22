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

package com.google.devtools.ksp.impl.test

import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
class KSPAATest : AbstractKSPAATest() {

    @Disabled
    @TestMetadata("annotatedUtil.kt")
    @Test
    fun testAnnotatedUtil() {
        runTest("../test-utils/testData/api/annotatedUtil.kt")
    }

    @Disabled
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

    @Disabled
    @TestMetadata("allFunctions_kt_inherits_java.kt")
    @Test
    fun testAllFunctions_kt_inherits_java() {
        runTest("../test-utils/testData/api/allFunctions_kt_inherits_java.kt")
    }

    @Disabled
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

    @Disabled
    @TestMetadata("annotationWithArbitraryClassValue.kt")
    @Test
    fun testAnnotationWithArbitraryClassValue() {
        runTest("../test-utils/testData/api/annotationWithArbitraryClassValue.kt")
    }

    @Disabled
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

    @Disabled
    @TestMetadata("annotationWithArrayValue.kt")
    @Test
    fun testAnnotationWithArrayValue() {
        runTest("../test-utils/testData/api/annotationWithArrayValue.kt")
    }

    @Disabled
    @TestMetadata("annotationWithDefault.kt")
    @Test
    fun testAnnotationWithDefault() {
        runTest("../test-utils/testData/api/annotationWithDefault.kt")
    }

    @Disabled
    @TestMetadata("annotationWithDefaultValues.kt")
    @Test
    fun testAnnotationWithDefaultValues() {
        runTest("../test-utils/testData/api/annotationWithDefaultValues.kt")
    }

    @Disabled
    @TestMetadata("annotationWithJavaTypeValue.kt")
    @Test
    fun testAnnotationWithJavaTypeValue() {
        runTest("../test-utils/testData/api/annotationWithJavaTypeValue.kt")
    }

    @Disabled
    @TestMetadata("asMemberOf.kt")
    @Test
    fun testAsMemberOf() {
        runTest("../test-utils/testData/api/asMemberOf.kt")
    }

    @Disabled
    @TestMetadata("backingFields.kt")
    @Test
    fun testBackingFields() {
        runTest("../test-utils/testData/api/backingFields.kt")
    }

    @Disabled
    @TestMetadata("builtInTypes.kt")
    @Test
    fun testBuiltInTypes() {
        runTest("../test-utils/testData/api/builtInTypes.kt")
    }

    @Disabled
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

    @Disabled
    @TestMetadata("constProperties.kt")
    @Test
    fun testConstProperties() {
        runTest("../test-utils/testData/api/constProperties.kt")
    }

    @Disabled
    @TestMetadata("constructorDeclarations.kt")
    @Test
    fun testConstructorDeclarations() {
        runTest("../test-utils/testData/api/constructorDeclarations.kt")
    }

    @Disabled
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

    @Disabled
    @TestMetadata("declarationPackageName.kt")
    @Test
    fun testDeclarationPackageName() {
        runTest("../test-utils/testData/api/declarationPackageName.kt")
    }

    @Disabled
    @TestMetadata("declarationOrder.kt")
    @Test
    fun testDeclarationOrder() {
        runTest("../test-utils/testData/api/declarationOrder.kt")
    }

    @Disabled
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

    @Disabled
    @TestMetadata("docString.kt")
    @Test
    fun testDocString() {
        runTest("../test-utils/testData/api/docString.kt")
    }

    @Disabled
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

    @Disabled
    @TestMetadata("functionTypeAlias.kt")
    @Test
    fun testFunctionTypeAlias() {
        runTest("../test-utils/testData/api/functionTypeAlias.kt")
    }

    @Disabled
    @TestMetadata("functionTypeAnnotation.kt")
    @Test
    fun testFunctionTypeAnnotation() {
        runTest("../test-utils/testData/api/functionTypeAnnotation.kt")
    }

    @Disabled
    @TestMetadata("functionTypes.kt")
    @Test
    fun testFunctionTypes() {
        runTest("../test-utils/testData/api/functionTypes.kt")
    }

    @Disabled
    @TestMetadata("getAnnotationByTypeWithInnerDefault.kt")
    @Test
    fun testGetAnnotationByTypeWithInnerDefault() {
        runTest("../test-utils/testData/api/getAnnotationByTypeWithInnerDefault.kt")
    }

    @Disabled
    @TestMetadata("getPackage.kt")
    @Test
    fun testGetPackage() {
        runTest("../test-utils/testData/api/getPackage.kt")
    }

    @Disabled
    @TestMetadata("getByName.kt")
    @Test
    fun testGetByName() {
        runTest("../test-utils/testData/api/getByName.kt")
    }

    @Disabled
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

    @Disabled
    @TestMetadata("innerTypes.kt")
    @Test
    fun testInnerTypes() {
        runTest("../test-utils/testData/api/innerTypes.kt")
    }

    @Disabled
    @TestMetadata("interfaceWithDefault.kt")
    @Test
    fun testInterfaceWithDefault() {
        runTest("../test-utils/testData/api/interfaceWithDefault.kt")
    }

    @Disabled
    @TestMetadata("javaModifiers.kt")
    @Test
    fun testJavaModifiers() {
        runTest("../test-utils/testData/api/javaModifiers.kt")
    }

    @Disabled
    @TestMetadata("javaNonNullTypes.kt")
    @Test
    fun testJavaNonNullTypes() {
        runTest("../test-utils/testData/api/javaNonNullTypes.kt")
    }

    @Disabled
    @TestMetadata("javaSubtype.kt")
    @Test
    fun testJavaSubtype() {
        runTest("../test-utils/testData/api/javaSubtype.kt")
    }

    @Disabled
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

    @Disabled
    @TestMetadata("javaTypes2.kt")
    @Test
    fun testJavaTypes2() {
        runTest("../test-utils/testData/api/javaTypes2.kt")
    }

    @Disabled
    @TestMetadata("javaWildcards2.kt")
    @Test
    fun testJavaWildcards2() {
        runTest("../test-utils/testData/api/javaWildcards2.kt")
    }

    @Disabled
    @TestMetadata("lateinitProperties.kt")
    @Test
    fun testLateinitProperties() {
        runTest("../test-utils/testData/api/lateinitProperties.kt")
    }

    @Disabled
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

    @Disabled
    @TestMetadata("mangledNames.kt")
    @Test
    fun testMangledNames() {
        runTest("../test-utils/testData/api/mangledNames.kt")
    }

    @Disabled
    @TestMetadata("multipleModules.kt")
    @Test
    fun testMultipleModules() {
        runTest("../test-utils/testData/api/multipleModules.kt")
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

    @Disabled
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

    @Disabled
    @TestMetadata("parent.kt")
    @Test
    fun testParent() {
        runTest("../test-utils/testData/api/parent.kt")
    }

    @Disabled
    @TestMetadata("platformDeclaration.kt")
    @Test
    fun testPlatformDeclaration() {
        runTest("../test-utils/testData/api/platformDeclaration.kt")
    }

    @Disabled
    @TestMetadata("rawTypes.kt")
    @Test
    fun testRawTypes() {
        runTest("../test-utils/testData/api/rawTypes.kt")
    }

    @Disabled
    @TestMetadata("recordJavaAnnotationTypes.kt")
    @Test
    fun testRecordJavaAnnotationTypes() {
        runTest("../test-utils/testData/api/recordJavaAnnotationTypes.kt")
    }

    @Disabled
    @TestMetadata("recordJavaAsMemberOf.kt")
    @Test
    fun testRecordJavaAsMemberOf() {
        runTest("../test-utils/testData/api/recordJavaAsMemberOf.kt")
    }

    @Disabled
    @TestMetadata("recordJavaGetAllMembers.kt")
    @Test
    fun testRecordJavaGetAllMembers() {
        runTest("../test-utils/testData/api/recordJavaGetAllMembers.kt")
    }

    @Disabled
    @TestMetadata("recordJavaOverrides.kt")
    @Test
    fun testRecordJavaOverrides() {
        runTest("../test-utils/testData/api/recordJavaOverrides.kt")
    }

    @Disabled
    @TestMetadata("recordJavaSupertypes.kt")
    @Test
    fun testRecordJavaSupertypes() {
        runTest("../test-utils/testData/api/recordJavaSupertypes.kt")
    }

    @Disabled
    @TestMetadata("referenceElement.kt")
    @Test
    fun testReferenceElement() {
        runTest("../test-utils/testData/api/referenceElement.kt")
    }

    @Disabled
    @TestMetadata("replaceWithErrorTypeArgs.kt")
    @Test
    fun testReplaceWithErrorTypeArgs() {
        runTest("../test-utils/testData/api/replaceWithErrorTypeArgs.kt")
    }

    @Disabled
    @TestMetadata("resolveJavaType.kt")
    @Test
    fun testResolveJavaType() {
        runTest("../test-utils/testData/api/resolveJavaType.kt")
    }

    @Disabled
    @TestMetadata("sealedClass.kt")
    @Test
    fun testSealedClass() {
        runTest("../test-utils/testData/api/sealedClass.kt")
    }

    @Disabled
    @TestMetadata("signatureMapper.kt")
    @Test
    fun testSignatureMapper() {
        runTest("../test-utils/testData/api/signatureMapper.kt")
    }

    @Disabled
    @TestMetadata("superTypes.kt")
    @Test
    fun testSuperTypes() {
        runTest("../test-utils/testData/api/superTypes.kt")
    }

    @Disabled
    @TestMetadata("throwList.kt")
    @Test
    fun testThrowList() {
        runTest("../test-utils/testData/api/throwList.kt")
    }

    @Disabled
    @TestMetadata("topLevelMembers.kt")
    @Test
    fun testTopLevelMembers() {
        runTest("../test-utils/testData/api/topLevelMembers.kt")
    }

    @Disabled
    @TestMetadata("typeAlias.kt")
    @Test
    fun testTypeAlias() {
        runTest("../test-utils/testData/api/typeAlias.kt")
    }

    @Disabled
    @TestMetadata("typeAliasComparison.kt")
    @Test
    fun testTypeAliasComparison() {
        runTest("../test-utils/testData/api/typeAliasComparison.kt")
    }

    @TestMetadata("typeComposure.kt")
    @Test
    fun testTypeComposure() {
        runTest("../test-utils/testData/api/typeComposure.kt")
    }

    @Disabled
    @TestMetadata("typeParameterReference.kt")
    @Test
    fun testTypeParameterReference() {
        runTest("../test-utils/testData/api/typeParameterReference.kt")
    }

    @Disabled
    @TestMetadata("varianceTypeCheck.kt")
    @Test
    fun testVarianceTypeCheck() {
        runTest("../test-utils/testData/api/varianceTypeCheck.kt")
    }

    @Disabled
    @TestMetadata("validateTypes.kt")
    @Test
    fun testValidateTypes() {
        runTest("../test-utils/testData/api/validateTypes.kt")
    }

    @Disabled
    @TestMetadata("visibilities.kt")
    @Test
    fun testVisibilities() {
        runTest("../test-utils/testData/api/visibilities.kt")
    }
}
