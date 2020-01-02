/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.gradle.model.builder

import org.jetbrains.kotlin.ksp.gradle.model.Ksp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KspModelBuilderTest {
    @Test
    fun testCanBuild() {
        val modelBuilder = KspModelBuilder()
        assertTrue(modelBuilder.canBuild(Ksp::class.java.name))
        assertFalse(modelBuilder.canBuild("wrongModel"))
    }
}