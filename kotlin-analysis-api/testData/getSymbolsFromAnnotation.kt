/*
 * Copyright 2024 Google LLC
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// TEST PROCESSOR: GetSymbolsFromAnnotationProcessor
// EXPECTED:
// ==== Anno superficial====
// Foo:KSClassDeclaration
// constructorParameterFoo:KSPropertyDeclaration
// propertyFoo:KSPropertyDeclaration
// functionFoo:KSFunctionDeclaration
// p1:KSValueParameter
// <init>:KSFunctionDeclaration
// constructorParameterFoo:KSValueParameter
// param:KSValueParameter
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// Burp:KSClassDeclaration
// Flux:KSTypeAlias
// ==== Anno in depth ====
// Foo:KSClassDeclaration
// constructorParameterFoo:KSPropertyDeclaration
// propertyFoo:KSPropertyDeclaration
// functionFoo:KSFunctionDeclaration
// p1:KSValueParameter
// local:KSPropertyDeclaration
// <init>:KSFunctionDeclaration
// constructorParameterFoo:KSValueParameter
// param:KSValueParameter
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// Burp:KSClassDeclaration
// Flux:KSTypeAlias
// ==== Bnno superficial====
// File: Foo.kt:KSFile
// propertyFoo.getter():KSPropertyAccessorImpl
// p2:KSValueParameter
// <init>:KSFunctionDeclaration
// ==== Bnno in depth ====
// File: Foo.kt:KSFile
// propertyFoo.getter():KSPropertyAccessorImpl
// p2:KSValueParameter
// <init>:KSFunctionDeclaration
// ==== A1 superficial====
// Foo:KSClassDeclaration
// constructorParameterFoo:KSPropertyDeclaration
// propertyFoo:KSPropertyDeclaration
// functionFoo:KSFunctionDeclaration
// p1:KSValueParameter
// <init>:KSFunctionDeclaration
// constructorParameterFoo:KSValueParameter
// param:KSValueParameter
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// Burp:KSClassDeclaration
// Flux:KSTypeAlias
// ==== A1 in depth ====
// Foo:KSClassDeclaration
// constructorParameterFoo:KSPropertyDeclaration
// propertyFoo:KSPropertyDeclaration
// functionFoo:KSFunctionDeclaration
// p1:KSValueParameter
// local:KSPropertyDeclaration
// <init>:KSFunctionDeclaration
// constructorParameterFoo:KSValueParameter
// param:KSValueParameter
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// Burp:KSClassDeclaration
// Flux:KSTypeAlias
// ==== A2 superficial====
// Foo:KSClassDeclaration
// constructorParameterFoo:KSPropertyDeclaration
// propertyFoo:KSPropertyDeclaration
// functionFoo:KSFunctionDeclaration
// p1:KSValueParameter
// <init>:KSFunctionDeclaration
// constructorParameterFoo:KSValueParameter
// param:KSValueParameter
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// Burp:KSClassDeclaration
// Flux:KSTypeAlias
// ==== A2 in depth ====
// Foo:KSClassDeclaration
// constructorParameterFoo:KSPropertyDeclaration
// propertyFoo:KSPropertyDeclaration
// functionFoo:KSFunctionDeclaration
// p1:KSValueParameter
// local:KSPropertyDeclaration
// <init>:KSFunctionDeclaration
// constructorParameterFoo:KSValueParameter
// param:KSValueParameter
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// Burp:KSClassDeclaration
// Flux:KSTypeAlias
// ==== Cnno in depth ====
// constructorParameterFoo:KSPropertyDeclaration
// <set-?>:KSValueParameter
// constructorParameterFoo:KSValueParameter
// x:KSPropertyDeclaration
// x:KSValueParameter
// END
//FILE: annotations.kt
annotation class Anno
annotation class Bnno
annotation class Cnno
typealias A1 = Anno
typealias A2 = A1

//FILE: Foo.kt
@file:Bnno

import Anno
import Anno as A3

@Anno
class Foo @Anno constructor(@Anno @param:Cnno val constructorParameterFoo: Int, @Anno param: Int){
    @Bnno constructor() {

    }

    @Anno
    val propertyFoo: String
        @Bnno get() = TODO()

    @Anno
    fun functionFoo(@Anno p1: Int, @Bnno p2: Int) {
        @Anno val local = 1
    }

    @setparam:Cnno
    var a = 1
}

class C(@Cnno val x: Int)

@A1
class Bar

@A2
class Baz

@A3
class Burp

@Anno
typealias Flux = String
