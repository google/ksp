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

// TEST PROCESSOR: DocStringProcessor
// EXPECTED:
// <init>: \n anno ctor doc\n
// <init>: \n class between anno and mod\n
// <init>: \n generic ctor doc\n
// <init>: \n owned ctor doc\n
// <init>: \n primary ctor doc\n
// <init>: \n private ctor doc\n
// <init>: \n separate primary ctor doc\n
// <init>: null
// <init>: null
// <init>: null
// <init>: null
// Anno: null
// AnnoWithArg: null
// ClassBetweenAnnoAndMod: \n class between anno and mod\n
// ClassWithAnnoCtor: null
// ClassWithMultiCtorDoc: null
// ClassWithOwnAndCtorDoc: \n own class doc\n
// ClassWithPrivateCtor: null
// CtorClass: null
// FirstParamMultiDoc: null
// GenericCtorClass: null
// JavaMultiDoc: null
// EXPECT ENABLE_BACKING_FIELDS = TRUE: firstProp.field: \n owned first\n
// firstProp: \n owned first\n
// fnBetweenAnnoAndMod: \n fn doc\n
// EXPECT ENABLE_BACKING_FIELDS = TRUE: jAfterBlock.field: \n java owned\n
// jAfterBlock: \n java owned\n
// EXPECT ENABLE_BACKING_FIELDS = TRUE: jMulti.field: \n java owned multi\n
// jMulti: \n java owned multi\n
// EXPECT ENABLE_BACKING_FIELDS = TRUE: prop1.field: \n kdoc between annotation and modifier\n
// prop1: \n kdoc between annotation and modifier\n
// EXPECT ENABLE_BACKING_FIELDS = TRUE: prop2.field: \n owned doc\n
// prop2: \n owned doc\n
// EXPECT ENABLE_BACKING_FIELDS = TRUE: propAfterAnno.field: \n owned after anno\n
// propAfterAnno: \n owned after anno\n
// EXPECT ENABLE_BACKING_FIELDS = TRUE: propAfterAnnoNoMod.field: \n kdoc after anno no modifier\n
// propAfterAnnoNoMod: \n kdoc after anno no modifier\n
// EXPECT ENABLE_BACKING_FIELDS = TRUE: propAfterBlock.field: \n owned after block\n
// propAfterBlock: \n owned after block\n
// EXPECT ENABLE_BACKING_FIELDS = TRUE: propWithArgComment.field: null
// propWithArgComment: null
// EXPECT ENABLE_BACKING_FIELDS = TRUE: propWithTrailingDoc.field: null
// propWithTrailingDoc: null
// EXPECT ENABLE_BACKING_FIELDS = TRUE: t.field: null
// t: null
// EXPECT ENABLE_BACKING_FIELDS = TRUE: value.field: null
// value: null
// END
// FILE: KotlinSrc.kt

annotation class Anno
annotation class AnnoWithArg(val value: Int)

class CtorClass
/**
 * primary ctor doc
 */
constructor(
    @Anno
    /**
     * kdoc between annotation and modifier
     */
    private val prop1: Int,
    /**
     * unassociated doc
     */
    /**
     * owned doc
     */
    val prop2: Int,
)

/**
 * own class doc
 */
class ClassWithOwnAndCtorDoc
/**
 * separate primary ctor doc
 */
constructor()

class ClassWithMultiCtorDoc
/**
 * unassociated ctor doc
 */
/**
 * owned ctor doc
 */
constructor()

class ClassWithPrivateCtor
/**
 * private ctor doc
 */
private constructor()

class ClassWithAnnoCtor
@Anno
/**
 * anno ctor doc
 */
private constructor()

class GenericCtorClass<T>
/**
 * generic ctor doc
 */
constructor(val t: T)

@Anno
/**
 * unassociated after anno
 */
/**
 * owned after anno
 */
private val propAfterAnno = 0

@Anno
/**
 * kdoc after anno no modifier
 */
val propAfterAnnoNoMod = 0

/* not a kdoc */
/**
 * owned after block
 */
val propAfterBlock = 0

@Anno
/**
 * fn doc
 */
private fun fnBetweenAnnoAndMod() = 0

@Anno
/**
 * class between anno and mod
 */
private class ClassBetweenAnnoAndMod

class FirstParamMultiDoc(
    /**
     * unassociated first
     */
    /**
     * owned first
     */
    val firstProp: Int,
)

@AnnoWithArg(/** arg comment */ 1)
val propWithArgComment = 0

val propWithTrailingDoc = /** expr doc */ 0

// FILE: JavaMultiDoc.java
class JavaMultiDoc {
    /* not a javadoc */
    /**
     * java owned
     */
    int jAfterBlock = 0;

    /**
     * java unassociated
     */
    /**
     * java owned multi
     */
    int jMulti = 0;
}
