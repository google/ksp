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

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
class KSPAA17Test : AbstractKSPAATest() {

    override fun configureTest(builder: TestConfigurationBuilder) {
        builder.defaultDirectives {
            -JvmEnvironmentConfigurationDirectives.JVM_TARGET
            JvmEnvironmentConfigurationDirectives.JVM_TARGET with JvmTarget.JVM_17
        }
    }

    @TestMetadata("jvmNameRecord.kt")
    @Test
    fun testJvmNameRecord() {
        runTest("../kotlin-analysis-api/testData/jvmNameRecord.kt")
    }
}
