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
    }

    @Bug("https://github.com/google/ksp/issues/2997", BugState.OPEN)
    abstract fun testAliasedAnnotations()

    @TestMetadata("annotatedUtil.kt")
    @Test
    fun testAnnotatedUtil() {
        runTest("$AA_PATH/annotatedUtil.kt")
    }

    @TestMetadata("javaAnnotatedUtil.kt")
    @Test
    fun testJavaAnnotatedUtil() {
        runTest("$AA_PATH/javaAnnotatedUtil.kt")
    }

    @TestMetadata("abstractFunctions.kt")
    @Test
    fun testAbstractFunctions() {
        runTest("$AA_PATH/abstractFunctions.kt")
    }

    @TestMetadata("allFunctions_java_inherits_kt.kt")
    @Test
    fun testAllFunctions_java_inherits_kt() {
        runTest("$AA_PATH/allFunctions_java_inherits_kt.kt")
    }

    @TestMetadata("allFunctions_kotlin.kt")
    @Test
    fun testAllFunctions_kotlin() {
        runTest("$AA_PATH/allFunctions_kotlin.kt")
    }

    @TestMetadata("allFunctions_kt_inherits_java.kt")
    @Test
    fun testAllFunctions_kt_inherits_java() {
        runTest("$AA_PATH/allFunctions_kt_inherits_java.kt")
    }

    @Bug("https://github.com/google/ksp/issues/2912", BugState.OPEN)
    @Negative("KEEP-402 specifies that the :all meta-target cannot be applied to annotation groups.")
    abstract fun testAllUseSiteTargetAppliedToAnnotationList()

    @TestMetadata("annotationsInDependencies.kt")
    @Test
    fun testAnnotationsInDependencies() {
        runTest("$AA_PATH/annotationsInDependencies.kt")
    }

    @TestMetadata("annotationOnConstructorParameter.kt")
    @Test
    fun testAnnotationOnConstructorParameter() {
        runTest("$AA_PATH/annotationOnConstructorParameter.kt")
    }

    @TestMetadata("annotationOnReceiver.kt")
    @Test
    fun testAnnotationOnReceiver() {
        runTest("$AA_PATH/annotationOnReceiver.kt")
    }

    @TestMetadata("annotationsRepeatable.kt")
    @Test
    fun testAnnotationsRepeatable() {
        runTest("$AA_PATH/annotationsRepeatable.kt")
    }

    @TestMetadata("annotationTargets.kt")
    @Test
    fun testAnnotationTargets() {
        runTest("$AA_PATH/annotationTargets.kt")
    }

    @TestMetadata("annotationWithArbitraryClassValue.kt")
    @Test
    fun testAnnotationWithArbitraryClassValue() {
        runTest("$AA_PATH/annotationWithArbitraryClassValue.kt")
    }

    @TestMetadata("annotationWithNestedClassValue.kt")
    @Test
    fun testAnnotationWithNestedClassValue() {
        runTest("$AA_PATH/annotationWithNestedClassValue.kt")
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
        runTest("$AA_PATH/annotationWithArrayValue.kt")
    }

    @Bug("https://github.com/google/ksp/issues/3008", BugState.OPEN)
    abstract fun testAnnotationArrayValueType()

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
        runTest("$AA_PATH/annotationWithJavaTypeValue.kt")
    }

    @TestMetadata("asMemberOf.kt")
    @Test
    fun testAsMemberOf() {
        runTest("$AA_PATH/asMemberOf.kt")
    }

    @TestMetadata("backingFields.kt")
    @Test
    fun testBackingFields() {
        runTest("$AA_PATH/backingFields.kt")
    }

    @TestMetadata("builtInTypes.kt")
    @Test
    fun testBuiltInTypes() {
        runTest("$AA_PATH/builtInTypes.kt")
    }

    @TestMetadata("objCacheA.kt")
    @Test
    fun testObjCacheA() {
        runTest("$AA_PATH/objCacheA.kt")
    }

    @TestMetadata("objCacheB.kt")
    @Test
    fun testObjCacheB() {
        runTest("$AA_PATH/objCacheB.kt")
    }

    @TestMetadata("checkOverride.kt")
    @Test
    fun testCheckOverride() {
        runTest("$AA_PATH/checkOverride.kt")
    }

    @TestMetadata("classKinds.kt")
    @Test
    fun testClassKinds() {
        runTest("$AA_PATH/classKinds.kt")
    }

    @TestMetadata("companion.kt")
    @Test
    fun testCompanion() {
        runTest("$AA_PATH/companion.kt")
    }

    @TestMetadata("constProperties.kt")
    @Test
    fun testConstProperties() {
        runTest("$AA_PATH/constProperties.kt")
    }

    @TestMetadata("constructorDeclarations.kt")
    @Test
    fun testConstructorDeclarations() {
        runTest("$AA_PATH/constructorDeclarations.kt")
    }

    @TestMetadata("crossModuleTypeAlias.kt")
    @Test
    fun testCrossModuleTypeAlias() {
        runTest("$AA_PATH/crossModuleTypeAlias.kt")
    }

    @Bug(
        "https://github.com/google/ksp/issues/2472",
        BugState.OPEN,
        "KEEP 367: Context parameters are stable in Kotlin 2.4.0"
    )
    abstract fun testContextParameters()

    @TestMetadata("declarationInconsistency.kt")
    @Test
    fun testDeclarationInconsistency() {
        runTest("$AA_PATH/declarationInconsistency.kt")
    }

    @TestMetadata("declarationPackageName.kt")
    @Test
    fun testDeclarationPackageName() {
        runTest("$AA_PATH/declarationPackageName.kt")
    }

    @TestMetadata("declarationsInAccessor.kt")
    @Test
    fun testDeclarationsInAccessor() {
        runTest("$AA_PATH/declarationsInAccessor.kt")
    }

    @TestMetadata("declarationsInClass.kt")
    @Test
    fun testDeclarationsInClass() {
        runTest("$AA_PATH/declarationsInClass.kt")
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
        runTest("$AA_PATH/declared.kt")
    }

    @TestMetadata("docString.kt")
    @Test
    fun testDocString() {
        runTest("$AA_PATH/docString.kt")
    }

    @TestMetadata("equals.kt")
    @Test
    fun testEquals() {
        runTest("$AA_PATH/equals.kt")
    }

    @TestMetadata("equivalentJavaWildcards.kt")
    @Test
    fun testEquivalentJavaWildcards() {
        runTest("$AA_PATH/equivalentJavaWildcards.kt")
    }

    @TestMetadata("enumModifiers.kt")
    @Test
    @Bug("https://github.com/google/ksp/issues/2271", BugState.OPEN)
    fun testEnumModifiers() {
        runThrowingTest("$AA_PATH/enumModifiers.kt")
    }

    @TestMetadata("errorTypes.kt")
    @Test
    fun testErrorTypes() {
        runTest("$AA_PATH/errorTypes.kt")
    }

    @Bug(
        "https://github.com/google/ksp/issues/2873",
        BugState.OPEN,
        "KEEP 430: Explicit backing fields added in Kotlin 2.4.0"
    )
    abstract fun testExplicitBackingFields()

    @TestMetadata("fieldAndPropertyUseSiteTargetOnConstructorParameters.kt")
    @Test
    @Bug("https://github.com/google/ksp/issues/2913", BugState.FIXED)
    @Negative("Constructor params not declared with val do not have generated properties or backing fields.")
    fun testFieldAndPropertyUseSiteTargetOnConstructorParameters() {
        runTest(
            "$AA_PATH/getSymbolsWithAnnotation/negative/fieldAndPropertyUseSiteTargetOnConstructorParameters.kt"
        )
    }

    @TestMetadata("functionTypeAlias.kt")
    @Test
    fun testFunctionTypeAlias() {
        runTest("$AA_PATH/functionTypeAlias.kt")
    }

    @TestMetadata("functionTypeAnnotation.kt")
    @Test
    fun testFunctionTypeAnnotation() {
        runTest("$AA_PATH/functionTypeAnnotation.kt")
    }

    @TestMetadata("functionTypes.kt")
    @Test
    fun testFunctionTypes() {
        runTest("$AA_PATH/functionTypes.kt")
    }

    @TestMetadata("functionKinds.kt")
    @Test
    fun testFunctionKinds() {
        runTest("$AA_PATH/functionKinds.kt")
    }

    @TestMetadata("getAnnotationByTypeWithInnerDefault.kt")
    @Test
    fun testGetAnnotationByTypeWithInnerDefault() {
        runTest("$AA_PATH/getAnnotationByTypeWithInnerDefault.kt")
    }

    @TestMetadata("getPackage.kt")
    @Test
    fun testGetPackage() {
        runTest("$AA_PATH/getPackage.kt")
    }

    @TestMetadata("getByName.kt")
    @Test
    fun testGetByName() {
        runTest("$AA_PATH/getByName.kt")
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
        runTest("$AA_PATH/hello.kt")
    }

    @TestMetadata("implicitElements.kt")
    @Test
    fun testImplicitElements() {
        runTest("$AA_PATH/implicitElements.kt")
    }

    @TestMetadata("implicitPropertyAccessors.kt")
    @Test
    fun testImplicitPropertyAccessors() {
        runTest("$AA_PATH/implicitPropertyAccessors.kt")
    }

    @TestMetadata("internalOfFriends.kt")
    @Test
    fun testInternalOfFriends() {
        runTest("$AA_PATH/internalOfFriends.kt")
    }

    @TestMetadata("inheritedTypeAlias.kt")
    @Test
    fun testInheritedTypeAlias() {
        runTest("$AA_PATH/inheritedTypeAlias.kt")
    }

    @TestMetadata("innerTypes.kt")
    @Test
    fun testInnerTypes() {
        runTest("$AA_PATH/innerTypes.kt")
    }

    @TestMetadata("interfaceWithDefault.kt")
    @Test
    fun testInterfaceWithDefault() {
        runTest("$AA_PATH/interfaceWithDefault.kt")
    }

    @TestMetadata("isMutable.kt")
    @Test
    fun testIsMutable() {
        runTest("$AA_PATH/isMutable.kt")
    }

    @TestMetadata("javaModifiers.kt")
    @Test
    fun testJavaModifiers() {
        runTest("$AA_PATH/javaModifiers.kt")
    }

    @TestMetadata("javaNonNullTypes.kt")
    @Test
    fun testJavaNonNullTypes() {
        runTest("$AA_PATH/javaNonNullTypes.kt")
    }

    @TestMetadata("javaSubtype.kt")
    @Test
    fun testJavaSubtype() {
        runTest("$AA_PATH/javaSubtype.kt")
    }

    @Bug("https://github.com/google/ksp/issues/2925", BugState.OPEN)
    abstract fun testJavaSubtypeOfKotlinInterface()

    @TestMetadata("javaToKotlinMapper.kt")
    @Test
    fun testJavaToKotlinMapper() {
        runTest("$AA_PATH/javaToKotlinMapper.kt")
    }

    @TestMetadata("javaTypes.kt")
    @Test
    fun testJavaTypes() {
        runTest("$AA_PATH/javaTypes.kt")
    }

    @TestMetadata("javaTypes2.kt")
    @Test
    fun testJavaTypes2() {
        runTest("$AA_PATH/javaTypes2.kt")
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

    @TestMetadata("jvmNameRecord.kt")
    @Test
    @Bug("https://github.com/google/ksp/issues/2812", BugState.FIXED)
    fun testJvmNameRecord() {
        runTest("$AA_PATH/jvmNameRecord.kt")
    }

    @TestMetadata("lateinitProperties.kt")
    @Test
    fun testLateinitProperties() {
        runTest("$AA_PATH/lateinitProperties.kt")
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
        runTest("$AA_PATH/makeNullable.kt")
    }

    @TestMetadata("mangledNames.kt")
    @Test
    @Bug(
        "https://github.com/google/ksp/issues/2964",
        BugState.FIXED,
        "Module names have been aligned across platforms in Kotlin 2.4.0. See KT-69701."
    )
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
        runTest("$AA_PATH/multipleModules.kt")
    }

    @TestMetadata("nestedAnnotations.kt")
    @Test
    fun testNestedAnnotations() {
        runTest("$AA_PATH/nestedAnnotations.kt")
    }

    @TestMetadata("nestedClassType.kt")
    @Test
    fun testNestedClassType() {
        runTest("$AA_PATH/nestedClassType.kt")
    }

    @TestMetadata("nullableTypes.kt")
    @Test
    fun testNullableTypes() {
        runTest("$AA_PATH/nullableTypes.kt")
    }

    @TestMetadata("conflictingOverride.kt")
    @Test
    fun testConflictingOverride() {
        runTest("$AA_PATH/overridee/conflictingOverride.kt")
    }

    @TestMetadata("javaAccessor.kt")
    @Test
    fun testJavaAccessor() {
        runTest("$AA_PATH/overridee/javaAccessor.kt")
    }

    @TestMetadata("javaAnno.kt")
    @Test
    fun testJavaAnno() {
        runTest("$AA_PATH/overridee/javaAnno.kt")
    }

    @TestMetadata("javaOverrideInSource.kt")
    @Test
    fun testJavaOverrideInSource() {
        runTest("$AA_PATH/overridee/javaOverrideInSource.kt")
    }

    @TestMetadata("noOverride.kt")
    @Test
    fun testNoOverride() {
        runTest("$AA_PATH/overridee/noOverride.kt")
    }

    @TestMetadata("overrideInLib.kt")
    @Test
    fun testOverrideInLib() {
        runTest("$AA_PATH/overridee/overrideInLib.kt")
    }

    @TestMetadata("overrideInSource.kt")
    @Test
    fun testOverrideInSource() {
        runTest("$AA_PATH/overridee/overrideInSource.kt")
    }

    @TestMetadata("overrideOrder.kt")
    @Test
    fun testOverrideOrder() {
        runTest("$AA_PATH/overridee/overrideOrder.kt")
    }

    @TestMetadata("packageAnnotations.kt")
    @Test
    fun testPackageAnnotation() {
        runTest("$AA_PATH/packageAnnotations.kt")
    }

    @TestMetadata("primaryConstructorOverride.kt")
    @Test
    fun testPrimaryConstructorOverride() {
        runTest("$AA_PATH/overridee/primaryConstructorOverride.kt")
    }

    @TestMetadata("parameterTypes.kt")
    @Test
    fun testParameterTypes() {
        runTest("$AA_PATH/parameterTypes.kt")
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
        runTest("$AA_PATH/platformDeclaration.kt")
    }

    @TestMetadata("rawTypes.kt")
    @Test
    fun testRawTypes() {
        runTest("$AA_PATH/rawTypes.kt")
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
        runTest("$AA_PATH/sealedClass.kt")
    }

    @TestMetadata("javaWildcardsSelfReferencing.kt")
    @Test
    @Bug("https://github.com/google/ksp/issues/1729", BugState.FIXED)
    @Bug("https://github.com/google/ksp/issues/1705", BugState.FIXED)
    fun testJavaWildcardsSelfReferencing() {
        runTest("$AA_PATH/javaWildcardsSelfReferencing.kt")
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
        runTest("$AA_PATH/superTypes.kt")
    }

    @TestMetadata("throwList.kt")
    @Test
    fun testThrowList() {
        runTest("$AA_PATH/throwList.kt")
    }

    @TestMetadata("topLevelMembers.kt")
    @Test
    fun testTopLevelMembers() {
        runTest("$AA_PATH/topLevelMembers.kt")
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
        runTest("$AA_PATH/typeAliasComparison.kt")
    }

    @TestMetadata("typeComposure.kt")
    @Test
    fun testTypeComposure() {
        runTest("$AA_PATH/typeComposure.kt")
    }

    @TestMetadata("typeComparison2.kt")
    @Test
    fun testTypeComparison2() {
        runTest("$AA_PATH/typeComparison2.kt")
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
        runTest("$AA_PATH/varianceTypeCheck.kt")
    }

    @TestMetadata("validateTypes.kt")
    @Test
    fun testValidateTypes() {
        runTest("$AA_PATH/validateTypes.kt")
    }

    @TestMetadata("vararg.kt")
    @Test
    fun testVararg() {
        runTest("$AA_PATH/vararg.kt")
    }

    @TestMetadata("visibilities.kt")
    @Test
    fun testVisibilities() {
        runTest("$AA_PATH/visibilities.kt")
    }

    @TestMetadata("multipleround.kt")
    @Test
    fun testMultipleround() {
        runTest("$AA_PATH/multipleround.kt")
    }

    @TestMetadata("deferredSymbols.kt")
    @Test
    fun testDeferredSymbols() {
        runTest("$AA_PATH/deferredSymbols.kt")
    }

    @TestMetadata("deferredJavaSymbols.kt")
    @Test
    fun testDeferredJavaSymbols() {
        runTest("$AA_PATH/deferredJavaSymbols.kt")
    }

    @Disabled
    @TestMetadata("deferredTypeRefs.kt")
    @Test
    fun testDeferredTypeRefs() {
        runTest("$AA_PATH/deferredTypeRefs.kt")
    }

    @TestMetadata("exitCode.kt")
    @Test
    fun testExitCode() {
        runTest("$AA_PATH/exitCode.kt")
    }

    @TestMetadata("packageProviderForGenerated.kt")
    @Test
    fun testPackageProviderForGenerated() {
        runTest("$AA_PATH/packageProviderForGenerated.kt")
    }

    @TestMetadata("typeAnnotationClassReference.kt")
    @Test
    @Bug("https://github.com/google/ksp/issues/3030", BugState.OPEN)
    @Bug(
        "https://github.com/google/ksp/issues/3036",
        BugState.FIXED,
        "The toString method of KSValueArgument has a circular call chain leading to a stack overflow."
    )
    fun testTypeAnnotationClassReference() {
        runThrowingTest("$AA_PATH/typeAnnotationClassReference.kt")
    }
}
