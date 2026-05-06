/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import com.google.devtools.ksp.test.annotations.Bug
import com.google.devtools.ksp.test.annotations.BugState
import com.google.devtools.ksp.test.annotations.Negative
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
abstract class KSPUnitTestSuite(
    experimentalPsiResolution: Boolean
) : AbstractKSPAATest(experimentalPsiResolution) {

    companion object {
        internal const val AA_PATH: String = "../kotlin-analysis-api/testData"
        internal const val UTIL_PATH: String = "../test-utils/testData/api"
    }

    @TestMetadata("annotatedUtil.kt")
    @Test
    fun testAnnotatedUtil() {
        runTest("$UTIL_PATH/annotatedUtil.kt")
    }

    @TestMetadata("javaAnnotatedUtil.kt")
    @Test
    fun testJavaAnnotatedUtil() {
        runTest("$UTIL_PATH/javaAnnotatedUtil.kt")
    }

    @TestMetadata("abstractFunctions.kt")
    @Test
    fun testAbstractFunctions() {
        runTest("$UTIL_PATH/abstractFunctions.kt")
    }

    @TestMetadata("allFunctions_java_inherits_kt.kt")
    @Test
    fun testAllFunctions_java_inherits_kt() {
        runTest("$UTIL_PATH/allFunctions_java_inherits_kt.kt")
    }

    @TestMetadata("allFunctions_kotlin.kt")
    @Test
    fun testAllFunctions_kotlin() {
        runTest("$UTIL_PATH/allFunctions_java_inherits_kt.kt")
    }

    @TestMetadata("allFunctions_kt_inherits_java.kt")
    @Test
    fun testAllFunctions_kt_inherits_java() {
        runTest("$AA_PATH/allFunctions_kt_inherits_java.kt")
    }

    @TestMetadata("allUseSiteTargetAppliedToAnnotationList.kt")
    @Test
    @Bug("https://github.com/google/ksp/issues/2912", BugState.OPEN)
    @Negative("KEEP-402 specifies that the :all meta-target cannot be applied to annotation groups.")
    fun testAllUseSiteTargetAppliedToAnnotationList() {
        runFailingTest("$AA_PATH/getSymbolsWithAnnotation/negative/allUseSiteTargetAppliedToAnnotationList.kt")
    }

    @TestMetadata("annotationInDependencies.kt")
    @Test
    fun testAnnotationsInDependencies() {
        runTest("$AA_PATH/annotationInDependencies.kt")
    }

    @TestMetadata("annotationOnConstructorParameter.kt")
    @Test
    fun testAnnotationOnConstructorParameter() {
        runTest("$UTIL_PATH/annotationOnConstructorParameter.kt")
    }

    @TestMetadata("annotationOnReceiver.kt")
    @Test
    fun testAnnotationOnReceiver() {
        runTest("$UTIL_PATH/annotationOnReceiver.kt")
    }

    @TestMetadata("annotationsRepeatable.kt")
    @Test
    fun testAnnotationsRepeatable() {
        runTest("$AA_PATH/annotationsRepeatable.kt")
    }

    @TestMetadata("annotationTargets.kt")
    @Test
    fun testAnnotationTargets() {
        runTest("$UTIL_PATH/annotationTargets.kt")
    }

    @TestMetadata("annotationWithArbitraryClassValue.kt")
    @Test
    fun testAnnotationWithArbitraryClassValue() {
        runTest("$UTIL_PATH/annotationWithArbitraryClassValue.kt")
    }

    @TestMetadata("annotationWithNestedClassValue.kt")
    @Test
    fun testAnnotationWithNestedClassValue() {
        runTest("$UTIL_PATH/annotationWithNestedClassValue.kt")
    }

    @TestMetadata("defaultKClassValue.kt")
    @Test
    fun testAnnotationValue_defaultKClassValue() {
        runTest("$AA_PATH/annotationValue/defaultKClassValue.kt")
    }

    @TestMetadata("annotationValue_java.kt")
    @Test
    fun testAnnotationValue_java() {
        runTest("$AA_PATH/annotationValue/annotationValue_java.kt")
    }

    @TestMetadata("annotationValue_java2.kt")
    @Test
    fun testAnnotationValue_java2() {
        runTest("$AA_PATH/annotationValue/annotationValue_java2.kt")
    }

    @TestMetadata("annotationValue_kt.kt")
    @Test
    fun testAnnotationValue_kt() {
        runTest("$AA_PATH/annotationValue/annotationValue_kt.kt")
    }

    @TestMetadata("annotationWithArrayValue.kt")
    @Test
    fun testAnnotationWithArrayValue() {
        runTest("$UTIL_PATH/annotationWithArrayValue.kt")
    }

    @TestMetadata("annotationWithDefault.kt")
    @Test
    fun testAnnotationWithDefault() {
        runTest("$AA_PATH/annotationWithDefault.kt")
    }

    @TestMetadata("annotationWithDefaultValues.kt")
    @Test
    fun testAnnotationWithDefaultValues() {
        runTest("$AA_PATH/annotationWithDefaultValues.kt")
    }

    @TestMetadata("annotationWithJavaTypeValue.kt")
    @Test
    fun testAnnotationWithJavaTypeValue() {
        runTest("$UTIL_PATH/annotationWithJavaTypeValue.kt")
    }

    @TestMetadata("asMemberOf.kt")
    @Test
    fun testAsMemberOf() {
        runTest("$AA_PATH/asMemberOf.kt")
    }

    @TestMetadata("backingFields.kt")
    @Test
    fun testBackingFields() {
        runTest("$UTIL_PATH/backingFields.kt")
    }

    @TestMetadata("builtInTypes.kt")
    @Test
    fun testBuiltInTypes() {
        runTest("$UTIL_PATH/builtInTypes.kt")
    }

    @TestMetadata("objCacheA.kt")
    @Test
    fun testObjCacheA() {
        runTest("$UTIL_PATH/objCacheA.kt")
    }

    @TestMetadata("objCacheB.kt")
    @Test
    fun testObjCacheB() {
        runTest("$UTIL_PATH/objCacheB.kt")
    }

    @TestMetadata("checkOverride.kt")
    @Test
    fun testCheckOverride() {
        runTest("$AA_PATH/checkOverride.kt")
    }

    @TestMetadata("classKinds.kt")
    @Test
    fun testClassKinds() {
        runTest("$UTIL_PATH/classKinds.kt")
    }

    @TestMetadata("companion.kt")
    @Test
    fun testCompanion() {
        runTest("$UTIL_PATH/companion.kt")
    }

    @TestMetadata("constProperties.kt")
    @Test
    fun testConstProperties() {
        runTest("$UTIL_PATH/constProperties.kt")
    }

    @TestMetadata("constructorDeclarations.kt")
    @Test
    fun testConstructorDeclarations() {
        runTest("$UTIL_PATH/constructorDeclarations.kt")
    }

    @TestMetadata("crossModuleTypeAlias.kt")
    @Test
    fun testCrossModuleTypeAlias() {
        runTest("$UTIL_PATH/crossModuleTypeAlias.kt")
    }

    @TestMetadata("declarationInconsistency.kt")
    @Test
    fun testDeclarationInconsistency() {
        runTest("$UTIL_PATH/declarationInconsistency.kt")
    }

    @TestMetadata("declarationPackageName.kt")
    @Test
    fun testDeclarationPackageName() {
        runTest("$UTIL_PATH/declarationPackageName.kt")
    }

    @TestMetadata("declarationsInAccessor.kt")
    @Test
    fun testDeclarationsInAccessor() {
        runTest("$UTIL_PATH/declarationsInAccessor.kt")
    }

    @TestMetadata("declarationOrder.kt")
    @Test
    fun testDeclarationOrder() {
        runTest("$AA_PATH/declarationOrder.kt")
    }

    @TestMetadata("declarationUtil.kt")
    @Test
    fun testDeclarationUtil() {
        runTest("$AA_PATH/declarationUtil.kt")
    }

    @TestMetadata("declared.kt")
    @Test
    fun testDeclared() {
        runTest("$UTIL_PATH/declared.kt")
    }

    @TestMetadata("docString.kt")
    @Test
    fun testDocString() {
        runTest("$UTIL_PATH/docString.kt")
    }

    @TestMetadata("equals.kt")
    @Test
    fun testEquals() {
        runTest("$UTIL_PATH/equals.kt")
    }

    @TestMetadata("equivalentJavaWildcards.kt")
    @Test
    fun testEquivalentJavaWildcards() {
        runTest("$AA_PATH/equivalentJavaWildcards.kt")
    }

    @TestMetadata("errorTypes.kt")
    @Test
    fun testErrorTypes() {
        runTest("$AA_PATH/errorTypes.kt")
    }

    @Bug("https://github.com/google/ksp/issues/2913", BugState.OPEN)
    @Negative("Constructor params not declared with val do not have generated properties or backing fields.")
    abstract fun testFieldAndPropertyUseSiteTargetOnConstructorParameters()

    @TestMetadata("functionTypeAlias.kt")
    @Test
    fun testFunctionTypeAlias() {
        runTest("$UTIL_PATH/functionTypeAlias.kt")
    }

    @TestMetadata("functionTypeAnnotation.kt")
    @Test
    fun testFunctionTypeAnnotation() {
        runTest("$UTIL_PATH/functionTypeAnnotation.kt")
    }

    @TestMetadata("functionTypes.kt")
    @Test
    fun testFunctionTypes() {
        runTest("$UTIL_PATH/functionTypes.kt")
    }

    @TestMetadata("functionKinds.kt")
    @Test
    fun testFunctionKinds() {
        runTest("$UTIL_PATH/functionKinds.kt")
    }

    @TestMetadata("getAnnotationByTypeWithInnerDefault.kt")
    @Test
    fun testGetAnnotationByTypeWithInnerDefault() {
        runTest("$UTIL_PATH/getAnnotationByTypeWithInnerDefault.kt")
    }

    @TestMetadata("getPackage.kt")
    @Test
    fun testGetPackage() {
        runTest("$AA_PATH/getPackage.kt")
    }

    @TestMetadata("getByName.kt")
    @Test
    fun testGetByName() {
        runTest("$UTIL_PATH/getByName.kt")
    }

    @TestMetadata("getSymbolsFromAnnotation.kt")
    @Test
    fun testGetSymbolsFromAnnotation() {
        runTest("$AA_PATH/getSymbolsFromAnnotation.kt")
    }

    @TestMetadata("getSymbolsFromAnnotationInLib.kt")
    @Test
    fun testGetSymbolsFromAnnotationInLib() {
        runTest("$AA_PATH/getSymbolsFromAnnotationInLib.kt")
    }

    @TestMetadata("groupedAnnotations.kt")
    @Test
    fun testGroupedAnnotations() {
        runTest("$AA_PATH/getSymbolsWithAnnotation/groupedAnnotations.kt")
    }

    @TestMetadata("groupedAnnotationsWithUseSiteTargets.kt")
    @Test
    fun testGroupedAnnotationsWithUseSiteTargets() {
        runTest("$AA_PATH/getSymbolsWithAnnotation/groupedAnnotationsWithUseSiteTargets.kt")
    }

    @TestMetadata("hello.kt")
    @Test
    fun testHello() {
        runTest("$UTIL_PATH/hello.kt")
    }

    @TestMetadata("implicitElements.kt")
    @Test
    fun testImplicitElements() {
        runTest("$UTIL_PATH/implicitElements.kt")
    }

    @TestMetadata("implicitPropertyAccessors.kt")
    @Test
    fun testImplicitPropertyAccessors() {
        runTest("$AA_PATH/implicitPropertyAccessors.kt")
    }

    @TestMetadata("internalOfFriends.kt")
    @Test
    fun testInternalOfFriends() {
        runTest("$UTIL_PATH/internalOfFriends.kt")
    }

    @TestMetadata("inheritedTypeAlias.kt")
    @Test
    fun testInheritedTypeAlias() {
        runTest("$UTIL_PATH/inheritedTypeAlias.kt")
    }

    @TestMetadata("innerTypes.kt")
    @Test
    fun testInnerTypes() {
        runTest("$AA_PATH/innerTypes.kt")
    }

    @TestMetadata("interfaceWithDefault.kt")
    @Test
    fun testInterfaceWithDefault() {
        runTest("$UTIL_PATH/interfaceWithDefault.kt")
    }

    @TestMetadata("isMutable.kt")
    @Test
    fun testIsMutable() {
        runTest("$UTIL_PATH/isMutable.kt")
    }

    @TestMetadata("javaModifiers.kt")
    @Test
    fun testJavaModifiers() {
        runTest("$AA_PATH/javaModifiers.kt")
    }

    @TestMetadata("javaNonNullTypes.kt")
    @Test
    fun testJavaNonNullTypes() {
        runTest("$UTIL_PATH/javaNonNullTypes.kt")
    }

    @TestMetadata("javaSubtype.kt")
    @Test
    fun testJavaSubtype() {
        runTest("$UTIL_PATH/javaSubtype.kt")
    }

    @TestMetadata("javaToKotlinMapper.kt")
    @Test
    fun testJavaToKotlinMapper() {
        runTest("$UTIL_PATH/javaToKotlinMapper.kt")
    }

    @TestMetadata("javaTypes.kt")
    @Test
    fun testJavaTypes() {
        runTest("$UTIL_PATH/javaTypes.kt")
    }

    @TestMetadata("javaTypes2.kt")
    @Test
    fun testJavaTypes2() {
        runTest("$UTIL_PATH/javaTypes2.kt")
    }

    @TestMetadata("javaWildcards2.kt")
    @Test
    fun testJavaWildcards2() {
        runTest("$AA_PATH/javaWildcards2.kt")
    }

    @TestMetadata("jvmName.kt")
    @Test
    fun testJvmName() {
        runTest("$AA_PATH/jvmName.kt")
    }

    @TestMetadata("lateinitProperties.kt")
    @Test
    fun testLateinitProperties() {
        runTest("$UTIL_PATH/lateinitProperties.kt")
    }

    @TestMetadata("libOrigins.kt")
    @Test
    fun testLibOrigins() {
        runTest("$AA_PATH/libOrigins.kt")
    }

    @TestMetadata("localAnnotationClass")
    @Test
    fun testLocalAnnotationClass() {
        runTest("$AA_PATH/getSymbolsWithAnnotation/localAnnotationClass.kt")
    }

    @TestMetadata("localClasses")
    @Test
    fun testLocalClasses() {
        runTest("$AA_PATH/getSymbolsWithAnnotation/localClasses.kt")
    }

    @TestMetadata("locations.kt")
    @Test
    fun testLocations() {
        runTest("$AA_PATH/locations.kt")
    }

    @TestMetadata("makeNullable.kt")
    @Test
    fun testMakeNullable() {
        runTest("$UTIL_PATH/makeNullable.kt")
    }

    @TestMetadata("mangledNames.kt")
    @Test
    fun testMangledNames() {
        runTest("$AA_PATH/mangledNames.kt")
    }

    @TestMetadata("metaAnnotations")
    @Test
    fun testMetaAnnotations() {
        runTest("$AA_PATH/getSymbolsWithAnnotation/metaAnnotations.kt")
    }

    @TestMetadata("multipleModules.kt")
    @Test
    fun testMultipleModules() {
        runTest("$UTIL_PATH/multipleModules.kt")
    }

    @TestMetadata("nestedAnnotations.kt")
    @Test
    fun testNestedAnnotations() {
        runTest("$UTIL_PATH/nestedAnnotations.kt")
    }

    @TestMetadata("nestedClassType.kt")
    @Test
    fun testNestedClassType() {
        runTest("$AA_PATH/nestedClassType.kt")
    }

    @TestMetadata("nullableTypes.kt")
    @Test
    fun testNullableTypes() {
        runTest("$UTIL_PATH/nullableTypes.kt")
    }

    @TestMetadata("conflictingOverride.kt")
    @Test
    fun testConflictingOverride() {
        runTest("$UTIL_PATH/overridee/conflictingOverride.kt")
    }

    @TestMetadata("javaAccessor.kt")
    @Test
    fun testJavaAccessor() {
        runTest("$UTIL_PATH/overridee/javaAccessor.kt")
    }

    @TestMetadata("javaAnno.kt")
    @Test
    fun testJavaAnno() {
        runTest("$AA_PATH/overridee/javaAnno.kt")
    }

    @TestMetadata("javaOverrideInSource.kt")
    @Test
    fun testJavaOverrideInSource() {
        runTest("$UTIL_PATH/overridee/javaOverrideInSource.kt")
    }

    @TestMetadata("noOverride.kt")
    @Test
    fun testNoOverride() {
        runTest("$UTIL_PATH/overridee/noOverride.kt")
    }

    @TestMetadata("overrideInLib.kt")
    @Test
    fun testOverrideInLib() {
        runTest("$UTIL_PATH/overridee/overrideInLib.kt")
    }

    @TestMetadata("overrideInSource.kt")
    @Test
    fun testOverrideInSource() {
        runTest("$UTIL_PATH/overridee/overrideInSource.kt")
    }

    @TestMetadata("overrideOrder.kt")
    @Test
    fun testOverrideOrder() {
        runTest("$AA_PATH/overridee/overrideOrder.kt")
    }

    @TestMetadata("packageAnnotations.kt")
    @Test
    fun testPackageAnnotation() {
        runTest("$UTIL_PATH/packageAnnotations.kt")
    }

    @TestMetadata("primaryConstructorOverride.kt")
    @Test
    fun testPrimaryConstructorOverride() {
        runTest("$UTIL_PATH/overridee/primaryConstructorOverride.kt")
    }

    @TestMetadata("parameterTypes.kt")
    @Test
    fun testParameterTypes() {
        runTest("$UTIL_PATH/parameterTypes.kt")
    }

    @TestMetadata("parent.kt")
    @Test
    fun testParent() {
        runTest("$AA_PATH/parent.kt")
    }

    @Disabled
    @TestMetadata("platformDeclaration.kt")
    @Test
    fun testPlatformDeclaration() {
        runTest("$UTIL_PATH/platformDeclaration.kt")
    }

    @TestMetadata("rawTypes.kt")
    @Test
    fun testRawTypes() {
        runTest("$UTIL_PATH/rawTypes.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("recordJavaAnnotationTypes.kt")
    @Test
    fun testRecordJavaAnnotationTypes() {
        runTest("$AA_PATH/recordJavaAnnotationTypes.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("recordJavaAsMemberOf.kt")
    @Test
    fun testRecordJavaAsMemberOf() {
        runTest("$AA_PATH/recordJavaAsMemberOf.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("recordJavaGetAllMembers.kt")
    @Test
    fun testRecordJavaGetAllMembers() {
        runTest("$AA_PATH/recordJavaGetAllMembers.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("recordJavaOverrides.kt")
    @Test
    fun testRecordJavaOverrides() {
        runTest("$AA_PATH/recordJavaOverrides.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("recordJavaResolutions.kt")
    @Test
    fun testRecordJavaResolutions() {
        runTest("$AA_PATH/recordJavaResolutions.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("recordJavaSupertypes.kt")
    @Test
    fun testRecordJavaSupertypes() {
        runTest("$AA_PATH/recordJavaSupertypes.kt")
    }

    @TestMetadata("referenceElement.kt")
    @Test
    fun testReferenceElement() {
        runTest("$AA_PATH/referenceElement.kt")
    }

    @TestMetadata("repeatedNonRepeatableAnnotations.kt")
    @Test
    @Bug("https://github.com/google/ksp/issues/2919", BugState.FIXED)
    fun testRepeatedNonRepeatableAnnotations() {
        runTest("$AA_PATH/getSymbolsWithAnnotation/repeatedNonRepeatableAnnotations.kt")
    }

    @TestMetadata("replaceWithErrorTypeArgs.kt")
    @Test
    fun testReplaceWithErrorTypeArgs() {
        runTest("$AA_PATH/replaceWithErrorTypeArgs.kt")
    }

    @TestMetadata("resolveJavaType.kt")
    @Test
    fun testResolveJavaType() {
        runTest("$AA_PATH/resolveJavaType.kt")
    }

    @TestMetadata("sealedClass.kt")
    @Test
    fun testSealedClass() {
        runTest("$UTIL_PATH/sealedClass.kt")
    }

    @TestMetadata("shadowingAnnotations.kt")
    @Test
    fun testShadowingAnnotations() {
        runTest("$AA_PATH/getSymbolsWithAnnotation/shadowingAnnotations.kt")
    }

    @TestMetadata("signatureMapper.kt")
    @Test
    fun testSignatureMapper() {
        runTest("$AA_PATH/signatureMapper.kt")
    }

    @TestMetadata("superTypes.kt")
    @Test
    fun testSuperTypes() {
        runTest("$UTIL_PATH/superTypes.kt")
    }

    @TestMetadata("throwList.kt")
    @Test
    fun testThrowList() {
        runTest("$UTIL_PATH/throwList.kt")
    }

    @TestMetadata("topLevelMembers.kt")
    @Test
    fun testTopLevelMembers() {
        runTest("$UTIL_PATH/topLevelMembers.kt")
    }

    @TestMetadata("typeAlias.kt")
    @Test
    fun testTypeAlias() {
        runTest("$AA_PATH/typeAlias.kt")
    }

    @TestMetadata("nestedTypeAlias.kt")
    @Test
    fun testNestedTypeAlias() {
        runTest("$AA_PATH/nestedTypeAlias.kt")
    }

    @TestMetadata("typeAliasComparison.kt")
    @Test
    fun testTypeAliasComparison() {
        runTest("$UTIL_PATH/typeAliasComparison.kt")
    }

    @TestMetadata("typeComposure.kt")
    @Test
    fun testTypeComposure() {
        runTest("$UTIL_PATH/typeComposure.kt")
    }

    @TestMetadata("typeComparison2.kt")
    @Test
    fun testTypeComparison2() {
        runTest("$UTIL_PATH/typeComparison2.kt")
    }

    @TestMetadata("typeParameterReference.kt")
    @Test
    fun testTypeParameterReference() {
        runTest("$AA_PATH/typeParameterReference.kt")
    }

    @TestMetadata("typeParameterVariance.kt")
    @Test
    fun testTypeParameterVariance() {
        runTest("$AA_PATH/typeParameterVariance.kt")
    }

    @TestMetadata("useSiteTargets.kt")
    @Test
    fun testUseSiteTargets() {
        runTest("$AA_PATH/getSymbolsWithAnnotation/useSiteTargets.kt")
    }

    @TestMetadata("valueParameter.kt")
    @Test
    fun testValueParameter() {
        runTest("$AA_PATH/valueParameter.kt")
    }

    @TestMetadata("varianceTypeCheck.kt")
    @Test
    fun testVarianceTypeCheck() {
        runTest("$UTIL_PATH/varianceTypeCheck.kt")
    }

    @TestMetadata("validateTypes.kt")
    @Test
    fun testValidateTypes() {
        runTest("$UTIL_PATH/validateTypes.kt")
    }

    @TestMetadata("vararg.kt")
    @Test
    fun testVararg() {
        runTest("$UTIL_PATH/vararg.kt")
    }

    @TestMetadata("visibilities.kt")
    @Test
    fun testVisibilities() {
        runTest("$AA_PATH/visibilities.kt")
    }

    @TestMetadata("multipleround.kt")
    @Test
    fun testMultipleround() {
        runTest("$UTIL_PATH/multipleround.kt")
    }

    @TestMetadata("deferredSymbols.kt")
    @Test
    fun testDeferredSymbols() {
        runTest("$UTIL_PATH/deferredSymbols.kt")
    }

    @TestMetadata("deferredJavaSymbols.kt")
    @Test
    fun testDeferredJavaSymbols() {
        runTest("$UTIL_PATH/deferredJavaSymbols.kt")
    }

    @Disabled
    @TestMetadata("deferredTypeRefs.kt")
    @Test
    fun testDeferredTypeRefs() {
        runTest("$UTIL_PATH/deferredTypeRefs.kt")
    }

    @TestMetadata("exitCode.kt")
    @Test
    fun testExitCode() {
        runTest("$UTIL_PATH/exitCode.kt")
    }

    @TestMetadata("packageProviderForGenerated.kt")
    @Test
    fun testPackageProviderForGenerated() {
        runTest("$UTIL_PATH/packageProviderForGenerated.kt")
    }
}
