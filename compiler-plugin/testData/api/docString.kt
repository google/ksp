/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// TEST PROCESSOR: DocStringProcessor
// EXPECTED:
// <init>: \n This is a java doc\n\n This is a second line\n\n more lines\n
// <init>: \n inner class\n
// <init>: \n nest class\n
// <init>: \n top level class\n\n doc can have multiple lines\n\n third non-empty line\n
// Inner: \n inner class\n
// JavaSrc: \n This is a java doc\n\n This is a second line\n\n more lines\n
// Nested: \n nest class\n
// TopClass: \n top level class\n\n doc can have multiple lines\n\n third non-empty line\n
// f1: \n top level function\n
// f2: \n member function\n
// foo: \n\n\n member function\n\n
// j1: \n field\n
// j2: null
// j3: null
// v1: \n\n top level property\n\n
// v2:  Irregular doc comment 1
// v3: \n Irregular doc comment 2
// v4: Irregular doc comment 3 *\n
// v5: \n owned doc comment\n
// v6: null
// v7: null
// v8: \n member property\n
// END
// FILE: KotlinSrc.kt

/**
 * top level function
 */
fun f1() = 0

/**
 *
 * top level property
 *
 */
val v1 = 0


/** * Irregular doc comment 1***/
val v2 = 0

/**
 * Irregular doc comment 2*/
val v3 = 0

/** Irregular doc comment 3 *
 */
val v4 = 0

/**
 * unassociated doc comment
 */
/**
 * owned doc comment
 */
val v5 = 0

/* Not doc comment 1 */
val v6 = 0

// Not doc comment 2
val v7 = 0

/**
 * top level class
 *
 * doc can have multiple lines
 *
 * third non-empty line
 */
class TopClass {
    /**
     * nest class
     */
    class Nested

    /**
     * inner class
     */
    class Inner

    /**
     * member function
     */
    fun f2() = 0

    /**
     * member property
     */
    val v8 = 0
}

// FILE: JavaSrc.java
/**
 * This is a java doc
 *
 * This is a second line
 *
 * more lines
 */
class JavaSrc {
    /**
     *
     *
     * member function
     *
     */
    int foo() {
        return 0;
    }

    /**
     * field
     */
    int j1 = 0;

    // Not a doc
    int j2 = 0;

    /* Not a doc */
    int j3 = 0;
}
