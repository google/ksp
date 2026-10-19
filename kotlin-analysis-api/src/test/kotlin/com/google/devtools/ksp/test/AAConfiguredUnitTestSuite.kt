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

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.buildApiFeatures
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Test

abstract class AAConfiguredUnitTestSuiteBase(
    apiFeatures: ApiFeatures
) : KSPUnitTestSuite(experimentalPsiResolution = false, apiFeatures) {

    @TestMetadata("getSymbolsWithAnnotation/negative/allUseSiteTargetAppliedToAnnotationList.kt")
    @Test
    override fun testAllUseSiteTargetAppliedToAnnotationList() {
        runFailingKSPTest("getSymbolsWithAnnotation/negative/allUseSiteTargetAppliedToAnnotationList.kt")
    }

    @TestMetadata("getSymbolsWithAnnotation/groupedAnnotationsWithUseSiteTargets.kt")
    @Test
    override fun testGroupedAnnotationsWithUseSiteTargets() {
        runKSPTest("getSymbolsWithAnnotation/groupedAnnotationsWithUseSiteTargets.kt")
    }

    @TestMetadata("functionKindsJavaInheritsKotlin.kt")
    @Test
    override fun testFunctionKindsJavaInheritsKotlin() {
        runThrowingKSPTest("functionKindsJavaInheritsKotlin.kt")
    }

    @TestMetadata("javaSubtypeOfKotlinInterface.kt")
    @Test
    override fun testJavaSubtypeOfKotlinInterface() {
        runKSPTest("javaSubtypeOfKotlinInterface.kt")
    }
}

class AAConfiguredDefaultFeaturesUnitTestSuite : AAConfiguredUnitTestSuiteBase(
    buildApiFeatures {
        enableBackingFields = false
        enableContextParameters = false
    }
)

class AAConfiguredBackingFieldsUnitTestSuite : AAConfiguredUnitTestSuiteBase(
    buildApiFeatures {
        enableBackingFields = true
        enableContextParameters = false
    }
)

class AAConfiguredContextParametersUnitTestSuite : AAConfiguredUnitTestSuiteBase(
    buildApiFeatures {
        enableBackingFields = false
        enableContextParameters = true
    }
)

class AAConfiguredAllFeaturesUnitTestSuite : AAConfiguredUnitTestSuiteBase(
    buildApiFeatures {
        enableBackingFields = true
        enableContextParameters = true
    }
)
