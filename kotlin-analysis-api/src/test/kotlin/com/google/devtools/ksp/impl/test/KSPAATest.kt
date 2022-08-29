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
        runTest("../compiler-plugin/testData/api/annotatedUtil.kt")
    }

    @Disabled
    @TestMetadata("javaAnnotatedUtil.kt")
    @Test
    fun testJavaAnnotatedUtil() {
        runTest("../compiler-plugin/testData/api/javaAnnotatedUtil.kt")
    }

    @TestMetadata("abstractFunctions.kt")
    @Test
    fun testAbstractFunctions() {
        runTest("../compiler-plugin/testData/api/abstractFunctions.kt")
    }

    @TestMetadata("allFunctions_java_inherits_kt.kt")
    @Test
    fun testAllFunctions_java_inherits_kt() {
        runTest("../compiler-plugin/testData/api/allFunctions_java_inherits_kt.kt")
    }

    @TestMetadata("allFunctions_kotlin.kt")
    @Test
    fun testAllFunctions_kotlin() {
        runTest("../compiler-plugin/testData/api/allFunctions_java_inherits_kt.kt")
    }

    @Disabled
    @TestMetadata("allFunctions_kt_inherits_java.kt")
    @Test
    fun testAllFunctions_kt_inherits_java() {
        runTest("../compiler-plugin/testData/api/allFunctions_kt_inherits_java.kt")
    }

    @Disabled
    @TestMetadata("annotationInDependencies.kt")
    @Test
    fun testAnnotationsInDependencies() {
        runTest("../compiler-plugin/testData/api/annotationInDependencies.kt")
    }

    @TestMetadata("annotationOnConstructorParameter.kt")
    @Test
    fun testAnnotationOnConstructorParameter() {
        runTest("../compiler-plugin/testData/api/annotationOnConstructorParameter.kt")
    }

    @Disabled
    @TestMetadata("annotationWithArbitraryClassValue.kt")
    @Test
    fun testAnnotationWithArbitraryClassValue() {
        runTest("../compiler-plugin/testData/api/annotationWithArbitraryClassValue.kt")
    }

    @Disabled
    @TestMetadata("annotationValue_java.kt")
    @Test
    fun testAnnotationValue_java() {
        runTest("../compiler-plugin/testData/api/annotationValue_java.kt")
    }

    @TestMetadata("annotationValue_kt.kt")
    @Test
    fun testAnnotationValue_kt() {
        runTest("../compiler-plugin/testData/api/annotationValue_kt.kt")
    }

    @Disabled
    @TestMetadata("annotationWithArrayValue.kt")
    @Test
    fun testAnnotationWithArrayValue() {
        runTest("../compiler-plugin/testData/api/annotationWithArrayValue.kt")
    }

    @Disabled
    @TestMetadata("annotationWithDefault.kt")
    @Test
    fun testAnnotationWithDefault() {
        runTest("../compiler-plugin/testData/api/annotationWithDefault.kt")
    }

    @Disabled
    @TestMetadata("annotationWithJavaTypeValue.kt")
    @Test
    fun testAnnotationWithJavaTypeValue() {
        runTest("../compiler-plugin/testData/api/annotationWithJavaTypeValue.kt")
    }

    @Disabled
    @TestMetadata("asMemberOf.kt")
    @Test
    fun testAsMemberOf() {
        runTest("../compiler-plugin/testData/api/asMemberOf.kt")
    }

    @Disabled
    @TestMetadata("backingFields.kt")
    @Test
    fun testBackingFields() {
        runTest("../compiler-plugin/testData/api/backingFields.kt")
    }

    @Disabled
    @TestMetadata("builtInTypes.kt")
    @Test
    fun testBuiltInTypes() {
        runTest("../compiler-plugin/testData/api/builtInTypes.kt")
    }

    @Disabled
    @TestMetadata("checkOverride.kt")
    @Test
    fun testCheckOverride() {
        runTest("../compiler-plugin/testData/api/checkOverride.kt")
    }

    @TestMetadata("classKinds.kt")
    @Test
    fun testClassKinds() {
        runTest("../compiler-plugin/testData/api/classKinds.kt")
    }

    @TestMetadata("companion.kt")
    @Test
    fun testCompanion() {
        runTest("../compiler-plugin/testData/api/companion.kt")
    }

    @Disabled
    @TestMetadata("constProperties.kt")
    @Test
    fun testConstProperties() {
        runTest("../compiler-plugin/testData/api/constProperties.kt")
    }

    @Disabled
    @TestMetadata("constructorDeclarations.kt")
    @Test
    fun testConstructorDeclarations() {
        runTest("../compiler-plugin/testData/api/constructorDeclarations.kt")
    }

    @Disabled
    @TestMetadata("crossModuleTypeAlias.kt")
    @Test
    fun testCrossModuleTypeAlias() {
        runTest("../compiler-plugin/testData/api/crossModuleTypeAlias.kt")
    }

    @TestMetadata("declarationInconsistency.kt")
    @Test
    fun testDeclarationInconsistency() {
        runTest("../compiler-plugin/testData/api/declarationInconsistency.kt")
    }

    @Disabled
    @TestMetadata("declarationPackageName.kt")
    @Test
    fun testDeclarationPackageName() {
        runTest("../compiler-plugin/testData/api/declarationPackageName.kt")
    }

    @Disabled
    @TestMetadata("declarationOrder.kt")
    @Test
    fun testDeclarationOrder() {
        runTest("../compiler-plugin/testData/api/declarationOrder.kt")
    }

    @Disabled
    @TestMetadata("declarationUtil.kt")
    @Test
    fun testDeclarationUtil() {
        runTest("../compiler-plugin/testData/api/declarationUtil.kt")
    }

    @TestMetadata("declared.kt")
    @Test
    fun testDeclared() {
        runTest("../compiler-plugin/testData/api/declared.kt")
    }

    @Disabled
    @TestMetadata("docString.kt")
    @Test
    fun testDocString() {
        runTest("../compiler-plugin/testData/api/docString.kt")
    }

    @Disabled
    @TestMetadata("equivalentJavaWildcards.kt")
    @Test
    fun testEquivalentJavaWildcards() {
        runTest("../compiler-plugin/testData/api/equivalentJavaWildcards.kt")
    }

    @TestMetadata("errorTypes.kt")
    @Test
    fun testErrorTypes() {
        runTest("../compiler-plugin/testData/api/errorTypes.kt")
    }

    @Disabled
    @TestMetadata("functionTypeAlias.kt")
    @Test
    fun testFunctionTypeAlias() {
        runTest("../compiler-plugin/testData/api/functionTypeAlias.kt")
    }

    @Disabled
    @TestMetadata("functionTypes.kt")
    @Test
    fun testFunctionTypes() {
        runTest("../compiler-plugin/testData/api/functionTypes.kt")
    }

    @Disabled
    @TestMetadata("getPackage.kt")
    @Test
    fun testGetPackage() {
        runTest("../compiler-plugin/testData/api/getPackage.kt")
    }

    @Disabled
    @TestMetadata("getByName.kt")
    @Test
    fun testGetByName() {
        runTest("../compiler-plugin/testData/api/getByName.kt")
    }

    @Disabled
    @TestMetadata("getSymbolsFromAnnotation.kt")
    @Test
    fun testGetSymbolsFromAnnotation() {
        runTest("../compiler-plugin/testData/api/getSymbolsFromAnnotation.kt")
    }

    @TestMetadata("hello.kt")
    @Test
    fun testHello() {
        runTest("../compiler-plugin/testData/api/hello.kt")
    }

    @Disabled
    @TestMetadata("implicitElements.kt")
    @Test
    fun testImplicitElements() {
        runTest("../compiler-plugin/testData/api/implicitElements.kt")
    }

    @Disabled
    @TestMetadata("implicitPropertyAccessors.kt")
    @Test
    fun testImplicitPropertyAccessors() {
        runTest("../compiler-plugin/testData/api/implicitPropertyAccessors.kt")
    }

    @Disabled
    @TestMetadata("innerTypes.kt")
    @Test
    fun testInnerTypes() {
        runTest("../compiler-plugin/testData/api/innerTypes.kt")
    }

    @Disabled
    @TestMetadata("interfaceWithDefault.kt")
    @Test
    fun testInterfaceWithDefault() {
        runTest("../compiler-plugin/testData/api/interfaceWithDefault.kt")
    }

    @Disabled
    @TestMetadata("javaModifiers.kt")
    @Test
    fun testJavaModifiers() {
        runTest("../compiler-plugin/testData/api/javaModifiers.kt")
    }

    @Disabled
    @TestMetadata("javaToKotlinMapper.kt")
    @Test
    fun testJavaToKotlinMapper() {
        runTest("../compiler-plugin/testData/api/javaToKotlinMapper.kt")
    }

    @Disabled
    @TestMetadata("javaTypes.kt")
    @Test
    fun testJavaTypes() {
        runTest("../compiler-plugin/testData/api/javaTypes.kt")
    }

    @Disabled
    @TestMetadata("javaTypes2.kt")
    @Test
    fun testJavaTypes2() {
        runTest("../compiler-plugin/testData/api/javaTypes2.kt")
    }

    @Disabled
    @TestMetadata("javaWildcards2.kt")
    @Test
    fun testJavaWildcards2() {
        runTest("../compiler-plugin/testData/api/javaWildcards2.kt")
    }

    @Disabled
    @TestMetadata("lateinitProperties.kt")
    @Test
    fun testLateinitProperties() {
        runTest("../compiler-plugin/testData/api/lateinitProperties.kt")
    }

    @Disabled
    @TestMetadata("libOrigins.kt")
    @Test
    fun testLibOrigins() {
        runTest("../compiler-plugin/testData/api/libOrigins.kt")
    }

    @Disabled
    @TestMetadata("makeNullable.kt")
    @Test
    fun testMakeNullable() {
        runTest("../compiler-plugin/testData/api/makeNullable.kt")
    }

    @Disabled
    @TestMetadata("mangledNames.kt")
    @Test
    fun testMangledNames() {
        runTest("../compiler-plugin/testData/api/mangledNames.kt")
    }

    @Disabled
    @TestMetadata("multipleModules.kt")
    @Test
    fun testMultipleModules() {
        runTest("../compiler-plugin/testData/api/multipleModules.kt")
    }

    @TestMetadata("nestedClassType.kt")
    @Test
    fun testNestedClassType() {
        runTest("../compiler-plugin/testData/api/nestedClassType.kt")
    }

    @TestMetadata("nullableTypes.kt")
    @Test
    fun testNullableTypes() {
        runTest("../compiler-plugin/testData/api/nullableTypes.kt")
    }

    @Disabled
    @TestMetadata("overridee.kt")
    @Test
    fun testOverridee() {
        runTest("../compiler-plugin/testData/api/overridee.kt")
    }

    @Disabled
    @TestMetadata("parameterTypes.kt")
    @Test
    fun testParameterTypes() {
        runTest("../compiler-plugin/testData/api/parameterTypes.kt")
    }

    @Disabled
    @TestMetadata("parent.kt")
    @Test
    fun testParent() {
        runTest("../compiler-plugin/testData/api/parent.kt")
    }

    @Disabled
    @TestMetadata("platformDeclaration.kt")
    @Test
    fun testPlatformDeclaration() {
        runTest("../compiler-plugin/testData/api/platformDeclaration.kt")
    }

    @Disabled
    @TestMetadata("rawTypes.kt")
    @Test
    fun testRawTypes() {
        runTest("../compiler-plugin/testData/api/rawTypes.kt")
    }

    @Disabled
    @TestMetadata("recordJavaAnnotationTypes.kt")
    @Test
    fun testRecordJavaAnnotationTypes() {
        runTest("../compiler-plugin/testData/api/recordJavaAnnotationTypes.kt")
    }

    @Disabled
    @TestMetadata("recordJavaAsMemberOf.kt")
    @Test
    fun testRecordJavaAsMemberOf() {
        runTest("../compiler-plugin/testData/api/recordJavaAsMemberOf.kt")
    }

    @Disabled
    @TestMetadata("recordJavaGetAllMembers.kt")
    @Test
    fun testRecordJavaGetAllMembers() {
        runTest("../compiler-plugin/testData/api/recordJavaGetAllMembers.kt")
    }

    @Disabled
    @TestMetadata("recordJavaOverrides.kt")
    @Test
    fun testRecordJavaOverrides() {
        runTest("../compiler-plugin/testData/api/recordJavaOverrides.kt")
    }

    @Disabled
    @TestMetadata("recordJavaSupertypes.kt")
    @Test
    fun testRecordJavaSupertypes() {
        runTest("../compiler-plugin/testData/api/recordJavaSupertypes.kt")
    }

    @Disabled
    @TestMetadata("referenceElement.kt")
    @Test
    fun testReferenceElement() {
        runTest("../compiler-plugin/testData/api/referenceElement.kt")
    }

    @Disabled
    @TestMetadata("replaceWithErrorTypeArgs.kt")
    @Test
    fun testReplaceWithErrorTypeArgs() {
        runTest("../compiler-plugin/testData/api/replaceWithErrorTypeArgs.kt")
    }

    @Disabled
    @TestMetadata("resolveJavaType.kt")
    @Test
    fun testResolveJavaType() {
        runTest("../compiler-plugin/testData/api/resolveJavaType.kt")
    }

    @Disabled
    @TestMetadata("sealedClass.kt")
    @Test
    fun testSealedClass() {
        runTest("../compiler-plugin/testData/api/sealedClass.kt")
    }

    @Disabled
    @TestMetadata("signatureMapper.kt")
    @Test
    fun testSignatureMapper() {
        runTest("../compiler-plugin/testData/api/signatureMapper.kt")
    }

    @Disabled
    @TestMetadata("throwList.kt")
    @Test
    fun testThrowList() {
        runTest("../compiler-plugin/testData/api/throwList.kt")
    }

    @Disabled
    @TestMetadata("topLevelMembers.kt")
    @Test
    fun testTopLevelMembers() {
        runTest("../compiler-plugin/testData/api/topLevelMembers.kt")
    }

    @Disabled
    @TestMetadata("typeAlias.kt")
    @Test
    fun testTypeAlias() {
        runTest("../compiler-plugin/testData/api/typeAlias.kt")
    }

    @Disabled
    @TestMetadata("typeAliasComparison.kt")
    @Test
    fun testTypeAliasComparison() {
        runTest("../compiler-plugin/testData/api/typeAliasComparison.kt")
    }

    @TestMetadata("typeComposure.kt")
    @Test
    fun testTypeComposure() {
        runTest("../compiler-plugin/testData/api/typeComposure.kt")
    }

    @Disabled
    @TestMetadata("typeParameterReference.kt")
    @Test
    fun testTypeParameterReference() {
        runTest("../compiler-plugin/testData/api/typeParameterReference.kt")
    }

    @Disabled
    @TestMetadata("varianceTypeCheck.kt")
    @Test
    fun testVarianceTypeCheck() {
        runTest("../compiler-plugin/testData/api/varianceTypeCheck.kt")
    }

    @Disabled
    @TestMetadata("validateTypes.kt")
    @Test
    fun testValidateTypes() {
        runTest("../compiler-plugin/testData/api/validateTypes.kt")
    }

    @Disabled
    @TestMetadata("visibilities.kt")
    @Test
    fun testVisibilities() {
        runTest("../compiler-plugin/testData/api/visibilities.kt")
    }
}
