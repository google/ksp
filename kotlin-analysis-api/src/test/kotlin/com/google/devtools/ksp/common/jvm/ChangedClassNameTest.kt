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

package com.google.devtools.ksp.common.jvm

import com.google.devtools.ksp.common.LookupSymbolWrapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChangedClassNameTest {
    @Test
    fun `parses package and binary simple name`() {
        val nested = ChangedClassName.fromInternalName("p1/Outer\$Inner")
        assertEquals("p1.Outer\$Inner", nested.raw)
        assertEquals("p1", nested.packageName)
        assertEquals("Outer\$Inner", nested.binarySimpleName)
        assertEquals("p1", nested.normalizedPackageName)
        assertEquals("p1.Outer.Inner", nested.normalizedFqn)

        val root = ChangedClassName.fromInternalName("Root\$Generated")
        assertEquals("", root.packageName)
        assertEquals("Root\$Generated", root.binarySimpleName)
        assertEquals("", root.normalizedPackageName)
        assertEquals("Root.Generated", root.normalizedFqn)

        val dollarPkg = ChangedClassName.fromInternalName("pkg\$1/My\$UtilKt")
        assertEquals("pkg\$1", dollarPkg.packageName)
        assertEquals("My\$UtilKt", dollarPkg.binarySimpleName)
        assertEquals("pkg.1", dollarPkg.normalizedPackageName)
        assertEquals("pkg.1.My.UtilKt", dollarPkg.normalizedFqn)
    }

    @Test
    fun `equality depends only on raw and derived properties are cached`() {
        val a = ChangedClassName("p1.Outer\$Inner")
        val b = ChangedClassName("p1.Outer\$Inner")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertSame(a.lookupSymbols, a.lookupSymbols)
        assertSame(a.normalizedFqn, a.normalizedFqn)
    }

    @Test
    fun `expands lookup symbols for nested, dollar-named, and root-package classes`() {
        assertEquals(
            setOf(
                LookupSymbolWrapper("Outer\$Inner", "p1"),
                LookupSymbolWrapper("Inner", "p1.Outer"),
            ),
            ChangedClassName("p1.Outer\$Inner").lookupSymbols,
        )

        assertEquals(
            setOf(
                LookupSymbolWrapper("Root\$Inner", ""),
                LookupSymbolWrapper("Root\$Inner", ANONYMOUS_ROOT_SCOPE),
                LookupSymbolWrapper("Inner", "Root"),
            ),
            ChangedClassName("Root\$Inner").lookupSymbols,
        )

        val multiDollar = ChangedClassName("pkg\$1.Outer\$Gen\$Inner").lookupSymbols
        assertTrue(LookupSymbolWrapper("Outer\$Gen\$Inner", "pkg\$1") in multiDollar)
        assertTrue(LookupSymbolWrapper("Outer\$Gen\$Inner", "pkg.1") in multiDollar)
        assertTrue(LookupSymbolWrapper("Inner", "pkg\$1.Outer\$Gen") in multiDollar)
        assertTrue(LookupSymbolWrapper("Inner", "pkg\$1.Outer.Gen") in multiDollar)
        assertTrue(LookupSymbolWrapper("Inner", "pkg.1.Outer\$Gen") in multiDollar)
        assertTrue(LookupSymbolWrapper("Inner", "pkg.1.Outer.Gen") in multiDollar)
        assertTrue(LookupSymbolWrapper("Gen\$Inner", "pkg\$1.Outer") in multiDollar)

        val leadingDollar = ChangedClassName("p1.\$Outer\$Inner").lookupSymbols
        assertTrue(LookupSymbolWrapper("Inner", "p1.\$Outer") in leadingDollar)
    }

    @Test
    fun `root scope aliases and dot-prefix matching`() {
        assertEquals(
            listOf(LookupSymbolWrapper("Foo", ""), LookupSymbolWrapper("Foo", ANONYMOUS_ROOT_SCOPE)),
            LookupSymbolWrapper("Foo", "").withRootScopeAliases(),
        )
        assertEquals(
            listOf(LookupSymbolWrapper("Foo", ANONYMOUS_ROOT_SCOPE), LookupSymbolWrapper("Foo", "")),
            LookupSymbolWrapper("Foo", ANONYMOUS_ROOT_SCOPE).withRootScopeAliases(),
        )
        assertEquals(
            listOf(LookupSymbolWrapper("Foo", "p1")),
            LookupSymbolWrapper("Foo", "p1").withRootScopeAliases(),
        )

        val changed = setOf("p1.Outer.Inner", "p2.Foo")
        val prefixes = dotPrefixesOf(changed)
        assertEquals(setOf("p1", "p1.Outer", "p2"), prefixes)
        assertTrue(isClassAffectedBy("p1.Outer.Inner", changed, prefixes))
        assertTrue(isClassAffectedBy("p1.Outer", changed, prefixes))
        assertTrue(isClassAffectedBy("p1.Outer.Inner.Deep", changed, prefixes))
        assertFalse(isClassAffectedBy("p1.Other", changed, prefixes))
    }
}
