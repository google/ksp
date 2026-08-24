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

abstract class AAConfiguredUnitTestSuiteBase(
    enableNewFeatures: Boolean
) : KSPUnitTestSuite(experimentalPsiResolution = false, enableNewFeatures) {

    @TestMetadata("getSymbolsWithAnnotation/aliasedAnnotation.kt")
    @Test
    override fun testAliasedAnnotations() {
        runTest("$AA_PATH/getSymbolsWithAnnotation/aliasedAnnotation.kt")
    }

    @TestMetadata("allUseSiteTargetAppliedToAnnotationList.kt")
    @Test
    override fun testAllUseSiteTargetAppliedToAnnotationList() {
        runFailingTest("$AA_PATH/getSymbolsWithAnnotation/negative/allUseSiteTargetAppliedToAnnotationList.kt")
    }

    @TestMetadata("contextParameters.kt")
    @Test
    override fun testContextParameters() {
        runFailingTest("$AA_PATH/getSymbolsWithAnnotation/contextParameters.kt")
    }

    @TestMetadata("getSymbolsWithAnnotation/groupedAnnotationsWithUseSiteTargets.kt")
    @Test
    override fun testGroupedAnnotationsWithUseSiteTargets() {
        runTest("$AA_PATH/getSymbolsWithAnnotation/groupedAnnotationsWithUseSiteTargets.kt")
    }

    @TestMetadata("hello.kt")
    @Test
    override fun testHello() {
        runTest("$AA_PATH/hello.kt")
    }

    @TestMetadata("functionKindsJavaInheritsKotlin.kt")
    @Test
    override fun testFunctionKindsJavaInheritsKotlin() {
        runThrowingTest("$AA_PATH/functionKindsJavaInheritsKotlin.kt")
    }

    @TestMetadata("javaSubtypeOfKotlinInterface.kt")
    @Test
    override fun testJavaSubtypeOfKotlinInterface() {
        runTest("$AA_PATH/javaSubtypeOfKotlinInterface.kt")
    }
}

class AAConfiguredUnitTestSuite : AAConfiguredUnitTestSuiteBase(enableNewFeatures = false) {

    @TestMetadata("docString.kt")
    @Test
    override fun testDocString() {
        runTest("$AA_PATH/docString.kt")
    }
}

class AAConfiguredNewFeaturesUnitTestSuite : AAConfiguredUnitTestSuiteBase(enableNewFeatures = true) {
    @TestMetadata("docString.kt")
    @Test
    override fun testDocString() {
        runFailingTest("$AA_PATH/docString.kt")
    }
}
