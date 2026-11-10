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

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.InternalKSPException
import com.google.devtools.ksp.test.annotations.Bug
import com.google.devtools.ksp.test.annotations.BugState
import com.google.devtools.ksp.test.annotations.Negative
import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.reflect.KClass

@Execution(ExecutionMode.SAME_THREAD)
@TestDataPath($$"$CONTENT_ROOT/testData")
abstract class KSPUnitTestSuite(
    experimentalPsiResolution: Boolean,
    apiFeatures: ApiFeatures,
) : AbstractKSPAATest(experimentalPsiResolution, apiFeatures) {

    companion object {
        internal const val AA_PATH: String = "../kotlin-analysis-api/testData"
    }

    /**
     * Runs a positive test.  Prefer calling this function instead of [runTest].
     */
    fun runKSPTest(path: String) = runTest("$AA_PATH/$path")

    /**
     * Runs a failing test. Prefer calling this function instead of [runFailingTest].
     */
    fun runFailingKSPTest(path: String) = runFailingTest("$AA_PATH/$path")

    /**
     * Runs a throwing test. Prefer calling this function instead of [runThrowingTest].
     */
    fun runThrowingKSPTest(path: String, expectedThrowableType: KClass<*> = InternalKSPException::class) =
        runThrowingTest("$AA_PATH/$path", expectedThrowableType)

    @Bug("https://github.com/google/ksp/issues/2997", BugState.FIXED)
    @TestMetadata("getSymbolsWithAnnotation/aliasedAnnotation.kt")
    @Test
    fun testAliasedAnnotations() {
        runKSPTest("getSymbolsWithAnnotation/aliasedAnnotation.kt")
    }

    @TestMetadata("annotatedUtil.kt")
    @Test
    fun testAnnotatedUtil() {
        runKSPTest("annotatedUtil.kt")
    }

    @TestMetadata("javaAnnotatedUtil.kt")
    @Test
    fun testJavaAnnotatedUtil() {
        runKSPTest("javaAnnotatedUtil.kt")
    }

    @TestMetadata("abstractFunctions.kt")
    @Test
    fun testAbstractFunctions() {
        runKSPTest("abstractFunctions.kt")
    }

    @TestMetadata("allFunctions_java_inherits_kt.kt")
    @Test
    fun testAllFunctions_java_inherits_kt() {
        runKSPTest("allFunctions_java_inherits_kt.kt")
    }

    @TestMetadata("allFunctions_kotlin.kt")
    @Test
    fun testAllFunctions_kotlin() {
        runKSPTest("allFunctions_kotlin.kt")
    }

    @TestMetadata("allFunctions_kt_inherits_java.kt")
    @Test
    fun testAllFunctions_kt_inherits_java() {
        runKSPTest("allFunctions_kt_inherits_java.kt")
    }

    @Bug("https://github.com/google/ksp/issues/2912", BugState.OPEN)
    @Negative("KEEP-402 specifies that the :all meta-target cannot be applied to annotation groups.")
    @TestMetadata("getSymbolsWithAnnotation/negative/allUseSiteTargetAppliedToAnnotationList.kt")
    abstract fun testAllUseSiteTargetAppliedToAnnotationList()

    @TestMetadata("annotationsInDependencies.kt")
    @Test
    fun testAnnotationsInDependencies() {
        runKSPTest("annotationsInDependencies.kt")
    }

    @TestMetadata("annotationOnConstructorParameter.kt")
    @Test
    fun testAnnotationOnConstructorParameter() {
        runKSPTest("annotationOnConstructorParameter.kt")
    }

    @TestMetadata("annotationOnReceiver.kt")
    @Test
    fun testAnnotationOnReceiver() {
        runKSPTest("annotationOnReceiver.kt")
    }

    @TestMetadata("annotationsRepeatable.kt")
    @Test
    fun testAnnotationsRepeatable() {
        runKSPTest("annotationsRepeatable.kt")
    }

    @TestMetadata("annotationTargets.kt")
    @Test
    fun testAnnotationTargets() {
        runKSPTest("annotationTargets.kt")
    }

    @TestMetadata("annotationWithArbitraryClassValue.kt")
    @Test
    fun testAnnotationWithArbitraryClassValue() {
        runKSPTest("annotationWithArbitraryClassValue.kt")
    }

    @TestMetadata("annotationWithNestedClassValue.kt")
    @Test
    fun testAnnotationWithNestedClassValue() {
        runKSPTest("annotationWithNestedClassValue.kt")
    }

    @TestMetadata("annotationValue/defaultKClassValue.kt")
    @Test
    fun testAnnotationValue_defaultKClassValue() {
        runKSPTest("annotationValue/defaultKClassValue.kt")
    }

    @TestMetadata("annotationValue/annotationValue_java.kt")
    @Test
    fun testAnnotationValue_java() {
        runKSPTest("annotationValue/annotationValue_java.kt")
    }

    @TestMetadata("annotationValue/annotationValue_java2.kt")
    @Test
    fun testAnnotationValue_java2() {
        runKSPTest("annotationValue/annotationValue_java2.kt")
    }

    @TestMetadata("annotationValue/annotationValue_kt.kt")
    @Test
    fun testAnnotationValue_kt() {
        runKSPTest("annotationValue/annotationValue_kt.kt")
    }

    @TestMetadata("annotationWithArrayValue.kt")
    @Test
    fun testAnnotationWithArrayValue() {
        runKSPTest("annotationWithArrayValue.kt")
    }

    @Bug("https://github.com/google/ksp/issues/3008", BugState.OPEN)
    @TestMetadata("annotationArrayValueType.kt")
    @Test
    fun testAnnotationArrayValueType() {
        runKSPTest("annotationArrayValueType.kt")
    }

    @TestMetadata("annotationWithDefault.kt")
    @Test
    fun testAnnotationWithDefault() {
        runKSPTest("annotationWithDefault.kt")
    }

    @TestMetadata("annotationWithDefaultValues.kt")
    @Test
    fun testAnnotationWithDefaultValues() {
        runKSPTest("annotationWithDefaultValues.kt")
    }

    @TestMetadata("annotationWithJavaTypeValue.kt")
    @Test
    fun testAnnotationWithJavaTypeValue() {
        runKSPTest("annotationWithJavaTypeValue.kt")
    }

    @TestMetadata("asMemberOf.kt")
    @Test
    fun testAsMemberOf() {
        runKSPTest("asMemberOf.kt")
    }

    @TestMetadata("backingFields.kt")
    @Test
    fun testBackingFields() {
        runKSPTest("backingFields.kt")
    }

    @Bug("https://github.com/google/ksp/issues/3222", BugState.FIXED)
    @TestMetadata("backingFieldsSignatureMapper.kt")
    @Test
    fun testBackingFieldsSignatureMapper() {
        runKSPTest("backingFieldsSignatureMapper.kt")
    }

    @TestMetadata("getSymbolsWithAnnotation/backingFieldsLateinit.kt")
    @Test
    @Bug(
        "https://github.com/google/ksp/issues/2873",
        BugState.FIXED,
        "Minimal reproduction of error observed in integration test AndroidDataBindingIT"
    )
    fun testBackingFieldsLateinit() {
        runKSPTest("getSymbolsWithAnnotation/backingFieldsLateinit.kt")
    }

    @TestMetadata("builtInTypes.kt")
    @Test
    fun testBuiltInTypes() {
        runKSPTest("builtInTypes.kt")
    }

    @TestMetadata("objCacheA.kt")
    @Test
    fun testObjCacheA() {
        runKSPTest("objCacheA.kt")
    }

    @TestMetadata("objCacheB.kt")
    @Test
    fun testObjCacheB() {
        runKSPTest("objCacheB.kt")
    }

    @Bug(
        "https://github.com/google/ksp/issues/3154",
        BugState.FIXED,
        "This test asserts that resolver behavior change (when calling the enableNewFeatures lambda) is observable and not just a property of KSP's test framework."
    )
    @Bug("https://github.com/google/ksp/issues/3185", BugState.FIXED)
    @TestMetadata("changingBehavior.kt")
    @Test
    fun testChangingBehavior() {
        runKSPTest("changingBehavior.kt")
    }

    @Bug(
        "https://github.com/google/ksp/issues/3250",
        BugState.FIXED,
        "Regression test to ensure the old `registerProcessorForNewFeatures` opt-in mechanism is maintained thus ensuring binary compatibility."
    )
    @TestMetadata("registerProcessorForNewFeatures.kt")
    @Test
    fun testRegisterProcessorForNewFeatures() {
        runKSPTest("registerProcessorForNewFeatures.kt")
    }

    @TestMetadata("checkOverride.kt")
    @Test
    fun testCheckOverride() {
        runKSPTest("checkOverride.kt")
    }

    @TestMetadata("classKinds.kt")
    @Test
    fun testClassKinds() {
        runKSPTest("classKinds.kt")
    }

    @TestMetadata("companion.kt")
    @Test
    fun testCompanion() {
        runKSPTest("companion.kt")
    }

    @TestMetadata("constProperties.kt")
    @Test
    fun testConstProperties() {
        runKSPTest("constProperties.kt")
    }

    @TestMetadata("constructorDeclarations.kt")
    @Test
    fun testConstructorDeclarations() {
        runKSPTest("constructorDeclarations.kt")
    }

    @TestMetadata("crossModuleTypeAlias.kt")
    @Test
    fun testCrossModuleTypeAlias() {
        runKSPTest("crossModuleTypeAlias.kt")
    }

    @Bug(
        "https://github.com/google/ksp/issues/2472",
        BugState.FIXED,
        "KEEP 367: Context parameters are stable in Kotlin 2.4.0"
    )
    @TestMetadata("getSymbolsWithAnnotation/contextParameters.kt")
    @Test
    fun testContextParameters() {
        runKSPTest("getSymbolsWithAnnotation/contextParameters.kt")
    }

    @TestMetadata("contextParameterIdentity.kt")
    @Test
    fun testContextParameterIdentity() {
        runKSPTest("contextParameterIdentity.kt")
    }

    @TestMetadata("contextParameterTypes.kt")
    @Test
    fun testContextParameterTypes() {
        runKSPTest("contextParameterTypes.kt")
    }

    @TestMetadata("contextParameterTypeAlias.kt")
    @Test
    fun testContextParameterTypeAlias() {
        runKSPTest("contextParameterTypeAlias.kt")
    }

    @TestMetadata("contextParameterNames.kt")
    @Test
    fun testContextParameterNames() {
        runKSPTest("contextParameterNames.kt")
    }

    @TestMetadata("native/contextParameterNames.kt")
    @Test
    fun testNativeContextParameterNames() {
        runKSPTest("native/contextParameterNames.kt")
    }

    @TestMetadata("getSymbolsWithAnnotation/negative/contextParameterDangling.kt")
    @Test
    @Negative("A dangling context(...) modifier list with no following declaration is a syntax error.")
    fun testContextParameterDangling() {
        runKSPTest("getSymbolsWithAnnotation/negative/contextParameterDangling.kt")
    }

    @TestMetadata("declarationInconsistency.kt")
    @Test
    fun testDeclarationInconsistency() {
        runKSPTest("declarationInconsistency.kt")
    }

    @TestMetadata("declarationPackageName.kt")
    @Test
    fun testDeclarationPackageName() {
        runKSPTest("declarationPackageName.kt")
    }

    @TestMetadata("declarationsInAccessor.kt")
    @Test
    fun testDeclarationsInAccessor() {
        runKSPTest("declarationsInAccessor.kt")
    }

    @TestMetadata("declarationsInClass.kt")
    @Test
    fun testDeclarationsInClass() {
        runKSPTest("declarationsInClass.kt")
    }

    @TestMetadata("declarationOrder.kt")
    @Test
    fun testDeclarationOrder() {
        runKSPTest("declarationOrder.kt")
    }

    @TestMetadata("declarationUtil.kt")
    @Test
    fun testDeclarationUtil() {
        runKSPTest("declarationUtil.kt")
    }

    @TestMetadata("declared.kt")
    @Test
    fun testDeclared() {
        runKSPTest("declared.kt")
    }

    @TestMetadata("docString.kt")
    @Test
    fun testDocString() {
        runKSPTest("docString.kt")
    }

    @TestMetadata("equals.kt")
    @Test
    fun testEquals() {
        runKSPTest("equals.kt")
    }

    @TestMetadata("equivalentJavaWildcards.kt")
    @Test
    fun testEquivalentJavaWildcards() {
        runKSPTest("equivalentJavaWildcards.kt")
    }

    @TestMetadata("enumModifiers.kt")
    @Test
    @Bug("https://github.com/google/ksp/issues/2271", BugState.OPEN)
    fun testEnumModifiers() {
        runThrowingKSPTest("enumModifiers.kt")
    }

    @TestMetadata("errorTypes.kt")
    @Test
    fun testErrorTypes() {
        runKSPTest("errorTypes.kt")
    }

    @Bug(
        "https://github.com/google/ksp/issues/2873",
        BugState.FIXED,
        "KEEP 430: Explicit backing fields added in Kotlin 2.4.0"
    )
    @TestMetadata("getSymbolsWithAnnotation/explicitBackingFields.kt")
    @Test
    fun testExplicitBackingFields() {
        runKSPTest("getSymbolsWithAnnotation/explicitBackingFields.kt")
    }

    @Bug(
        "https://github.com/google/ksp/issues/3165",
        BugState.FIXED,
        "Obtaining package name of implicit backing field could throw exception."
    )
    @TestMetadata("getSymbolsWithAnnotation/backingFieldsPackageName.kt")
    @Test
    fun testBackingFieldsPackageName() {
        runKSPTest("getSymbolsWithAnnotation/backingFieldsPackageName.kt")
    }

    @Bug("https://github.com/google/ksp/issues/2420", BugState.OPEN)
    @TestMetadata("fakeOverrideProperties.kt")
    @Test
    fun testFakeOverrideProperties() {
        runFailingKSPTest("fakeOverrideProperties.kt")
    }

    @TestMetadata("getSymbolsWithAnnotation/negative/fieldAndPropertyUseSiteTargetOnConstructorParameters.kt")
    @Test
    @Bug("https://github.com/google/ksp/issues/2913", BugState.FIXED)
    @Negative("Constructor params not declared with val do not have generated properties or backing fields.")
    fun testFieldAndPropertyUseSiteTargetOnConstructorParameters() {
        runKSPTest("getSymbolsWithAnnotation/negative/fieldAndPropertyUseSiteTargetOnConstructorParameters.kt")
    }

    @TestMetadata("explicitBackingFieldsSubtyping.kt")
    @Test
    @Bug(
        "https://github.com/google/ksp/issues/2873",
        BugState.FIXED,
        "KEEP 430: Backing fields can be a subtype of the property."
    )
    fun testExplicitBackingFieldsSubtyping() {
        runKSPTest("explicitBackingFieldsSubtyping.kt")
    }

    @TestMetadata("functionTypeAlias.kt")
    @Test
    fun testFunctionTypeAlias() {
        runKSPTest("functionTypeAlias.kt")
    }

    @TestMetadata("functionTypeAnnotation.kt")
    @Test
    fun testFunctionTypeAnnotation() {
        runKSPTest("functionTypeAnnotation.kt")
    }

    @TestMetadata("functionTypes.kt")
    @Test
    fun testFunctionTypes() {
        runKSPTest("functionTypes.kt")
    }

    @TestMetadata("functionKinds.kt")
    @Test
    fun testFunctionKinds() {
        runKSPTest("functionKinds.kt")
    }

    @Bug("https://github.com/google/ksp/issues/2496", BugState.OPEN)
    @TestMetadata("functionKindsJavaInheritsKotlin.kt")
    abstract fun testFunctionKindsJavaInheritsKotlin()

    @TestMetadata("getAnnotationByTypeWithInnerDefault.kt")
    @Test
    fun testGetAnnotationByTypeWithInnerDefault() {
        runKSPTest("getAnnotationByTypeWithInnerDefault.kt")
    }

    @TestMetadata("getPackage.kt")
    @Test
    fun testGetPackage() {
        runKSPTest("getPackage.kt")
    }

    @TestMetadata("getByName.kt")
    @Test
    fun testGetByName() {
        runKSPTest("getByName.kt")
    }

    @TestMetadata("getSymbolsFromAnnotation.kt")
    @Test
    fun testGetSymbolsFromAnnotation() {
        runKSPTest("getSymbolsFromAnnotation.kt")
    }

    @TestMetadata("getSymbolsFromAnnotationInLib.kt")
    @Test
    fun testGetSymbolsFromAnnotationInLib() {
        runKSPTest("getSymbolsFromAnnotationInLib.kt")
    }

    @TestMetadata("getSymbolsWithAnnotation/groupedAnnotations.kt")
    @Test
    fun testGroupedAnnotations() {
        runKSPTest("getSymbolsWithAnnotation/groupedAnnotations.kt")
    }

    @TestMetadata("getSymbolsWithAnnotation/groupedAnnotationsWithUseSiteTargets.kt")
    abstract fun testGroupedAnnotationsWithUseSiteTargets()

    @TestMetadata("hello.kt")
    @Test
    fun testHello() {
        runKSPTest("hello.kt")
    }

    @TestMetadata("implicitElements.kt")
    @Test
    fun testImplicitElements() {
        runKSPTest("implicitElements.kt")
    }

    @TestMetadata("implicitPropertyAccessors.kt")
    @Test
    fun testImplicitPropertyAccessors() {
        runKSPTest("implicitPropertyAccessors.kt")
    }

    @TestMetadata("internalOfFriends.kt")
    @Test
    fun testInternalOfFriends() {
        runKSPTest("internalOfFriends.kt")
    }

    @TestMetadata("inheritedTypeAlias.kt")
    @Test
    fun testInheritedTypeAlias() {
        runKSPTest("inheritedTypeAlias.kt")
    }

    @TestMetadata("innerTypes.kt")
    @Test
    fun testInnerTypes() {
        runKSPTest("innerTypes.kt")
    }

    @TestMetadata("interfaceWithDefault.kt")
    @Test
    fun testInterfaceWithDefault() {
        runKSPTest("interfaceWithDefault.kt")
    }

    @TestMetadata("isMutable.kt")
    @Test
    fun testIsMutable() {
        runKSPTest("isMutable.kt")
    }

    @Bug("https://github.com/google/ksp/issues/3191", BugState.FIXED)
    @TestMetadata("javaBackingFieldUsedInKotlin.kt")
    @Test
    fun testJavaBackingFieldUsedInKotlin() {
        runKSPTest("javaBackingFieldUsedInKotlin.kt")
    }

    @Bug("https://github.com/google/ksp/issues/3123", BugState.OPEN)
    @Bug("https://github.com/google/ksp/issues/3125", BugState.FIXED)
    @Bug("https://github.com/google/ksp/issues/3155", BugState.OPEN)
    @Bug("https://github.com/google/ksp/issues/3185", BugState.FIXED)
    @TestMetadata("javaModifiers.kt")
    @Test
    fun testJavaModifiers() {
        runKSPTest("javaModifiers.kt")
    }

    @Bug("https://github.com/google/ksp/issues/3124", BugState.FIXED)
    @TestMetadata("javaModifiersJvmStaticAnnotation.kt")
    @Test
    fun testJavaModifiersJvmStaticAnnotation() {
        runKSPTest("javaModifiersJvmStaticAnnotation.kt")
    }

    @TestMetadata("javaNonNullTypes.kt")
    @Test
    fun testJavaNonNullTypes() {
        runKSPTest("javaNonNullTypes.kt")
    }

    @Bug("https://github.com/google/ksp/issues/3166", BugState.FIXED)
    @TestMetadata("getSymbolsWithAnnotation/javaBackingFieldsParents.kt")
    @Test
    fun testJavaBackingFieldsParents() {
        runKSPTest("getSymbolsWithAnnotation/javaBackingFieldsParents.kt")
    }

    @TestMetadata("javaSubtype.kt")
    @Test
    fun testJavaSubtype() {
        runKSPTest("javaSubtype.kt")
    }

    @Bug("https://github.com/google/ksp/issues/2925", BugState.OPEN)
    @TestMetadata("javaSubtypeOfKotlinInterface.kt")
    abstract fun testJavaSubtypeOfKotlinInterface()

    @TestMetadata("javaToKotlinMapper.kt")
    @Test
    fun testJavaToKotlinMapper() {
        runKSPTest("javaToKotlinMapper.kt")
    }

    @TestMetadata("javaTypes.kt")
    @Test
    fun testJavaTypes() {
        runKSPTest("javaTypes.kt")
    }

    @TestMetadata("javaTypes2.kt")
    @Test
    fun testJavaTypes2() {
        runKSPTest("javaTypes2.kt")
    }

    @TestMetadata("javaWildcards2.kt")
    @Test
    fun testJavaWildcards2() {
        runKSPTest("javaWildcards2.kt")
    }

    @TestMetadata("jvmName.kt")
    @Test
    fun testJvmName() {
        runKSPTest("jvmName.kt")
    }

    @TestMetadata("jvmNameRecord.kt")
    @Test
    @Bug("https://github.com/google/ksp/issues/2812", BugState.FIXED)
    fun testJvmNameRecord() {
        runKSPTest("jvmNameRecord.kt")
    }

    @Bug("https://github.com/google/ksp/issues/3125", BugState.FIXED)
    @TestMetadata("jvmModifiersCompanionObject.kt")
    @Test
    fun testJvmModifiersCompanionObject() {
        runKSPTest("jvmModifiersCompanionObject.kt")
    }

    @TestMetadata("lateinitProperties.kt")
    @Test
    fun testLateinitProperties() {
        runKSPTest("lateinitProperties.kt")
    }

    @TestMetadata("libOrigins.kt")
    @Test
    fun testLibOrigins() {
        runKSPTest("libOrigins.kt")
    }

    @TestMetadata("getSymbolsWithAnnotation/localAnnotationClass.kt")
    @Test
    fun testLocalAnnotationClass() {
        runKSPTest("getSymbolsWithAnnotation/localAnnotationClass.kt")
    }

    @TestMetadata("getSymbolsWithAnnotation/localClasses.kt")
    @Test
    fun testLocalClasses() {
        runKSPTest("getSymbolsWithAnnotation/localClasses.kt")
    }

    @TestMetadata("locations.kt")
    @Test
    fun testLocations() {
        runKSPTest("locations.kt")
    }

    @TestMetadata("makeNullable.kt")
    @Test
    fun testMakeNullable() {
        runKSPTest("makeNullable.kt")
    }

    @TestMetadata("mangledNames.kt")
    @Test
    @Bug(
        "https://github.com/google/ksp/issues/2964",
        BugState.FIXED,
        "Module names have been aligned across platforms in Kotlin 2.4.0. See KT-69701."
    )
    fun testMangledNames() {
        runKSPTest("mangledNames.kt")
    }

    @TestMetadata("getSymbolsWithAnnotation/metaAnnotations.kt")
    @Test
    fun testMetaAnnotations() {
        runKSPTest("getSymbolsWithAnnotation/metaAnnotations.kt")
    }

    @TestMetadata("multipleModules.kt")
    @Test
    fun testMultipleModules() {
        runKSPTest("multipleModules.kt")
    }

    @TestMetadata("nestedAnnotations.kt")
    @Test
    fun testNestedAnnotations() {
        runKSPTest("nestedAnnotations.kt")
    }

    @TestMetadata("nestedClassType.kt")
    @Test
    fun testNestedClassType() {
        runKSPTest("nestedClassType.kt")
    }

    @TestMetadata("nullableTypes.kt")
    @Test
    fun testNullableTypes() {
        runKSPTest("nullableTypes.kt")
    }

    @TestMetadata("overridee/conflictingOverride.kt")
    @Test
    fun testConflictingOverride() {
        runKSPTest("overridee/conflictingOverride.kt")
    }

    @TestMetadata("overridee/javaAccessor.kt")
    @Test
    fun testJavaAccessor() {
        runKSPTest("overridee/javaAccessor.kt")
    }

    @TestMetadata("overridee/javaAnno.kt")
    @Test
    fun testJavaAnno() {
        runKSPTest("overridee/javaAnno.kt")
    }

    @TestMetadata("overridee/javaOverrideInSource.kt")
    @Test
    fun testJavaOverrideInSource() {
        runKSPTest("overridee/javaOverrideInSource.kt")
    }

    @Bug(
        "https://github.com/google/ksp/issues/2873",
        BugState.FIXED,
        "Java fields are considered properties that always have fields (accessors do not count)."
    )
    @TestMetadata("javaBackingFields.kt")
    @Test
    fun testJavaBackingFields() {
        runKSPTest("javaBackingFields.kt")
    }

    @TestMetadata("overridee/noOverride.kt")
    @Test
    fun testNoOverride() {
        runKSPTest("overridee/noOverride.kt")
    }

    @TestMetadata("overridee/overrideInLib.kt")
    @Test
    fun testOverrideInLib() {
        runKSPTest("overridee/overrideInLib.kt")
    }

    @TestMetadata("overridee/overrideInSource.kt")
    @Test
    fun testOverrideInSource() {
        runKSPTest("overridee/overrideInSource.kt")
    }

    @TestMetadata("overridee/overrideOrder.kt")
    @Test
    fun testOverrideOrder() {
        runKSPTest("overridee/overrideOrder.kt")
    }

    @TestMetadata("packageAnnotations.kt")
    @Test
    fun testPackageAnnotation() {
        runKSPTest("packageAnnotations.kt")
    }

    @TestMetadata("overridee/primaryConstructorOverride.kt")
    @Test
    fun testPrimaryConstructorOverride() {
        runKSPTest("overridee/primaryConstructorOverride.kt")
    }

    @TestMetadata("parameterTypes.kt")
    @Test
    fun testParameterTypes() {
        runKSPTest("parameterTypes.kt")
    }

    @Bug("https://github.com/google/ksp/issues/2498", BugState.OPEN)
    @TestMetadata("parentOfJavaMethodOverridingKotlinProperty.kt")
    @Test
    fun testParentOfJavaMethodOverridingKotlinProperty() {
        runFailingKSPTest("parentOfJavaMethodOverridingKotlinProperty.kt")
    }

    @TestMetadata("parent.kt")
    @Test
    fun testParent() {
        runKSPTest("parent.kt")
    }

    @Disabled
    @TestMetadata("platformDeclaration.kt")
    @Test
    fun testPlatformDeclaration() {
        runKSPTest("platformDeclaration.kt")
    }

    @TestMetadata("rawTypes.kt")
    @Test
    fun testRawTypes() {
        runKSPTest("rawTypes.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("recordJavaAnnotationTypes.kt")
    @Test
    fun testRecordJavaAnnotationTypes() {
        runKSPTest("recordJavaAnnotationTypes.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("recordJavaAsMemberOf.kt")
    @Test
    fun testRecordJavaAsMemberOf() {
        runKSPTest("recordJavaAsMemberOf.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("recordJavaGetAllMembers.kt")
    @Test
    fun testRecordJavaGetAllMembers() {
        runKSPTest("recordJavaGetAllMembers.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("recordJavaOverrides.kt")
    @Test
    fun testRecordJavaOverrides() {
        runKSPTest("recordJavaOverrides.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("recordJavaResolutions.kt")
    @Test
    fun testRecordJavaResolutions() {
        runKSPTest("recordJavaResolutions.kt")
    }

    @DisabledOnOs(OS.WINDOWS)
    @TestMetadata("recordJavaSupertypes.kt")
    @Test
    fun testRecordJavaSupertypes() {
        runKSPTest("recordJavaSupertypes.kt")
    }

    @TestMetadata("referenceElement.kt")
    @Test
    fun testReferenceElement() {
        runKSPTest("referenceElement.kt")
    }

    @TestMetadata("getSymbolsWithAnnotation/repeatedNonRepeatableAnnotations.kt")
    @Test
    @Bug("https://github.com/google/ksp/issues/2919", BugState.FIXED)
    fun testRepeatedNonRepeatableAnnotations() {
        runKSPTest("getSymbolsWithAnnotation/repeatedNonRepeatableAnnotations.kt")
    }

    @TestMetadata("replaceWithErrorTypeArgs.kt")
    @Test
    fun testReplaceWithErrorTypeArgs() {
        runKSPTest("replaceWithErrorTypeArgs.kt")
    }

    @TestMetadata("resolveJavaType.kt")
    @Test
    fun testResolveJavaType() {
        runKSPTest("resolveJavaType.kt")
    }

    @TestMetadata("sealedClass.kt")
    @Test
    fun testSealedClass() {
        runKSPTest("sealedClass.kt")
    }

    @TestMetadata("javaWildcardsSelfReferencing.kt")
    @Test
    @Bug("https://github.com/google/ksp/issues/1729", BugState.FIXED)
    @Bug("https://github.com/google/ksp/issues/1705", BugState.FIXED)
    fun testJavaWildcardsSelfReferencing() {
        runKSPTest("javaWildcardsSelfReferencing.kt")
    }

    @TestMetadata("getSymbolsWithAnnotation/shadowingAnnotations.kt")
    @Test
    fun testShadowingAnnotations() {
        runKSPTest("getSymbolsWithAnnotation/shadowingAnnotations.kt")
    }

    @TestMetadata("signatureMapper.kt")
    @Test
    fun testSignatureMapper() {
        runKSPTest("signatureMapper.kt")
    }

    @TestMetadata("superTypes.kt")
    @Test
    fun testSuperTypes() {
        runKSPTest("superTypes.kt")
    }

    @TestMetadata("throwList.kt")
    @Test
    fun testThrowList() {
        runKSPTest("throwList.kt")
    }

    @TestMetadata("topLevelMembers.kt")
    @Test
    fun testTopLevelMembers() {
        runKSPTest("topLevelMembers.kt")
    }

    @TestMetadata("typeAlias.kt")
    @Test
    fun testTypeAlias() {
        runKSPTest("typeAlias.kt")
    }

    @TestMetadata("nestedTypeAlias.kt")
    @Test
    fun testNestedTypeAlias() {
        runKSPTest("nestedTypeAlias.kt")
    }

    @TestMetadata("typeAliasComparison.kt")
    @Test
    fun testTypeAliasComparison() {
        runKSPTest("typeAliasComparison.kt")
    }

    @TestMetadata("typeComposure.kt")
    @Test
    fun testTypeComposure() {
        runKSPTest("typeComposure.kt")
    }

    @TestMetadata("typeComparison2.kt")
    @Test
    fun testTypeComparison2() {
        runKSPTest("typeComparison2.kt")
    }

    @TestMetadata("typeParameterReference.kt")
    @Test
    fun testTypeParameterReference() {
        runKSPTest("typeParameterReference.kt")
    }

    @TestMetadata("typeParameterVariance.kt")
    @Test
    fun testTypeParameterVariance() {
        runKSPTest("typeParameterVariance.kt")
    }

    @TestMetadata("getSymbolsWithAnnotation/useSiteTargets.kt")
    @Test
    fun testUseSiteTargets() {
        runKSPTest("getSymbolsWithAnnotation/useSiteTargets.kt")
    }

    @TestMetadata("valueParameter.kt")
    @Test
    fun testValueParameter() {
        runKSPTest("valueParameter.kt")
    }

    @Bug("https://github.com/google/ksp/issues/3271", BugState.FIXED)
    @TestMetadata("varargAnnotationApplication.kt")
    @Test
    fun testVarargAnnotationApplication() {
        runKSPTest("varargAnnotationApplication.kt")
    }

    @TestMetadata("varianceTypeCheck.kt")
    @Test
    fun testVarianceTypeCheck() {
        runKSPTest("varianceTypeCheck.kt")
    }

    @TestMetadata("validateTypes.kt")
    @Test
    fun testValidateTypes() {
        runKSPTest("validateTypes.kt")
    }

    @TestMetadata("vararg.kt")
    @Test
    fun testVararg() {
        runKSPTest("vararg.kt")
    }

    @TestMetadata("visibilities.kt")
    @Test
    fun testVisibilities() {
        runKSPTest("visibilities.kt")
    }

    @TestMetadata("multipleround.kt")
    @Test
    fun testMultipleround() {
        runKSPTest("multipleround.kt")
    }

    @TestMetadata("deferredSymbols.kt")
    @Test
    fun testDeferredSymbols() {
        runKSPTest("deferredSymbols.kt")
    }

    @TestMetadata("deferredJavaSymbols.kt")
    @Test
    fun testDeferredJavaSymbols() {
        runKSPTest("deferredJavaSymbols.kt")
    }

    @Disabled
    @TestMetadata("deferredTypeRefs.kt")
    @Test
    fun testDeferredTypeRefs() {
        runKSPTest("deferredTypeRefs.kt")
    }

    @TestMetadata("exitCode.kt")
    @Test
    fun testExitCode() {
        runKSPTest("exitCode.kt")
    }

    @TestMetadata("packageProviderForGenerated.kt")
    @Test
    fun testPackageProviderForGenerated() {
        runKSPTest("packageProviderForGenerated.kt")
    }

    @TestMetadata("typeAnnotationClassReference.kt")
    @Test
    @Bug("https://github.com/google/ksp/issues/3030", BugState.FIXED)
    @Bug(
        "https://github.com/google/ksp/issues/3036",
        BugState.FIXED,
        "The toString method of KSValueArgument has a circular call chain leading to a stack overflow."
    )
    fun testTypeAnnotationClassReference() {
        runKSPTest("typeAnnotationClassReference.kt")
    }

    @Bug("https://github.com/google/ksp/issues/3242", BugState.FIXED)
    @TestMetadata("typeParameterBoundTypeAlias.kt")
    @Test
    fun testTypeParameterBoundTypeAlias() {
        runKSPTest("typeParameterBoundTypeAlias.kt")
    }

    @TestMetadata("native/nativeTest.kt")
    @Test
    fun testNativeTest() {
        runKSPTest("native/nativeTest.kt")
    }

    @Bug("https://github.com/google/ksp/issues/2396", BugState.FIXED)
    @TestMetadata("native/packageDeclarations.kt")
    @Test
    fun testNativePackageDeclarations() {
        runKSPTest("native/packageDeclarations.kt")
    }

    @Bug("https://github.com/google/ksp/issues/2396", BugState.FIXED)
    @TestMetadata("native/packageDeclarationsMultiRound.kt")
    @Test
    fun testNativePackageDeclarationsMultiRound() {
        runKSPTest("native/packageDeclarationsMultiRound.kt")
    }

    @Bug("https://github.com/google/ksp/issues/2356", BugState.FIXED)
    @TestMetadata("native/annotationDefaultValue.kt")
    @Test
    fun testNativeAnnotationDefaultValue() {
        runKSPTest("native/annotationDefaultValue.kt")
    }

    @Bug(
        "https://github.com/google/ksp/issues/3233",
        BugState.FIXED,
        "Covers the default value types of native/annotationDefaultValue.kt in depth, including parameters " +
            "without default values and annotations applied inside the library"
    )
    @TestMetadata("native/annotationDefaultValueTypes.kt")
    @Test
    fun testNativeAnnotationDefaultValueTypes() {
        runKSPTest("native/annotationDefaultValueTypes.kt")
    }

    @Bug(
        "https://github.com/google/ksp/issues/3233",
        BugState.FIXED,
        "Asserts that on JVM, parameters without default values (including array parameters) are absent " +
            "from defaultArguments, matching Native behavior."
    )
    @TestMetadata("annotationDefaultValueTypes.kt")
    @Test
    fun testAnnotationDefaultValueTypes() {
        runKSPTest("annotationDefaultValueTypes.kt")
    }

    @Bug(
        "https://github.com/google/ksp/issues/2396",
        BugState.FIXED,
        "This test is a copy of native/packageDeclarations.kt but this test asserts that it works on JVM"
    )
    @TestMetadata("packageDeclarations.kt")
    @Test
    fun testPackageDeclarations() {
        runKSPTest("packageDeclarations.kt")
    }

    @Bug("https://github.com/google/ksp/issues/3140", BugState.FIXED)
    @TestMetadata("pluginProblemReporter.kt")
    @Test
    fun testPluginProblemReporter() {
        runKSPTest("pluginProblemReporter.kt")
    }

    @TestMetadata("expectDifferentOutput.kt")
    @Test
    fun testExpectDifferentOutput() {
        runKSPTest("expectDifferentOutput.kt")
    }

    @TestMetadata("expectDifferentOutputFailingOnCurrent.kt")
    @Test
    fun testExpectDifferentOutputFailingOnCurrent() {
        // N.B.: This test is supposed to fail on one configuration.
        // It asserts that the test output actually varies depending on the configuration.
        if (!apiFeatures.enableBackingFields && !apiFeatures.enableContextParameters) {
            runFailingKSPTest("expectDifferentOutputFailingOnCurrent.kt")
        } else {
            runKSPTest("expectDifferentOutputFailingOnCurrent.kt")
        }
    }

    @TestMetadata("expectDifferentOutputFailingOnBackingFields.kt")
    @Test
    fun testExpectDifferentOutputFailingOnBackingFields() {
        // N.B.: This test is supposed to fail on one configuration.
        // It asserts that the test output actually varies depending on the configuration.
        if (apiFeatures.enableBackingFields && !apiFeatures.enableContextParameters) {
            runFailingKSPTest("expectDifferentOutputFailingOnBackingFields.kt")
        } else {
            runKSPTest("expectDifferentOutputFailingOnBackingFields.kt")
        }
    }

    @TestMetadata("expectDifferentOutputFailingOnContextParameters.kt")
    @Test
    fun testExpectDifferentOutputFailingOnContextParameters() {
        // N.B.: This test is supposed to fail on one configuration.
        // It asserts that the test output actually varies depending on the configuration.
        if (!apiFeatures.enableBackingFields && apiFeatures.enableContextParameters) {
            runFailingKSPTest("expectDifferentOutputFailingOnContextParameters.kt")
        } else {
            runKSPTest("expectDifferentOutputFailingOnContextParameters.kt")
        }
    }

    @TestMetadata("expectDifferentOutputFailingOnNext.kt")
    @Test
    fun testExpectDifferentOutputFailingOnNext() {
        // N.B.: This test is supposed to fail on one configuration.
        // It asserts that the test output actually varies depending on the configuration.
        if (apiFeatures.enableBackingFields && apiFeatures.enableContextParameters) {
            runFailingKSPTest("expectDifferentOutputFailingOnNext.kt")
        } else {
            runKSPTest("expectDifferentOutputFailingOnNext.kt")
        }
    }
}
